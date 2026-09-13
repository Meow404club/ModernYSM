package com.elfmcys.yesstevemodel.platform.neoforge.network;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.platform.neoforge.YesSteveModelForge;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.PacketDirection;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。[M4 锚点落地] forge 原类的 SimpleChannel 体系在
 * NeoForge 1.20.4 已被 payload 体系取代（20.4.251 merged jar javap 实证：无
 * NetworkRegistry.newSimpleChannel/SimpleChannel；有 RegisterPayloadHandlerEvent +
 * IPayloadRegistrar.play(RL, Reader, IPlayPayloadHandler) + PacketDistributor.send(payload)）。
 * 门面（rip.ysm.api.network.YSMChannel）签名保持不变，语义映射：
 * - register(disc,type,encoder,decoder,handler,direction) → 每个 discriminator 一个 payload id
 *   （&lt;channelNs&gt;:&lt;channelPath&gt;."disc.&lt;n&gt;"），编解码器经 Wrapped 信封适配
 *   CustomPacketPayload.write/id 契约；方向守卫移入分发 lambda（forge 由 NetworkDirection 在传输层强制）；
 * - 分片协议（disc 255）逻辑逐行等价：netty 键从 Connection 换成 Channel（closeFuture 直达，
 *   免 ConnectionAccessor mixin 依赖），重排/超时/上限常量与 forge 原类一致；
 * - 版本协商：forge newSimpleChannel(str -&gt; true) 接受任意版本 → IPayloadRegistrar.optional()。
 */
public final class YSMChannelImpl {

    private static final int FRAGMENT_DISCRIMINATOR = 255;
    private static final int FRAGMENT_DATA_SIZE = 30_000;
    private static final int MAX_FRAGMENT_COUNT = 128;
    private static final int MAX_REASSEMBLED_SIZE = 2 * 1024 * 1024;
    private static final long FRAGMENT_TIMEOUT_NANOS = 30_000_000_000L;

    private static final Map<Integer, LocalCodec<?>> codecs = new HashMap<>();
    private static final Map<Class<?>, Integer> discriminatorByType = new HashMap<>();
    private static final Map<Integer, ResourceLocation> idByDiscriminator = new HashMap<>();
    private static final Map<io.netty.channel.Channel, Map<Integer, FragmentAccumulator>> incomingFragments = new ConcurrentHashMap<>();
    private static final AtomicInteger nextTransferId = new AtomicInteger();

    private static String version;
    private static boolean channelRegistered = false;

    private YSMChannelImpl() {
    }

    public static void init(ResourceLocation channelId, String channelVersion) {
        version = channelVersion;
        // init 由 YesSteveModel.init（mod 构造期）调用，mod 事件总线此时已注入；
        // RegisterPayloadHandlerEvent 在构造后的加载阶段触发，门面全部 register 均已就位。
        YesSteveModelForge.getModEventBus().addListener(YSMChannelImpl::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlerEvent event) {
        // registrar 参数=payload 命名空间（须等于 modId，ModdedPacketRegistrar.validatePayload 校验），
        // 非 channel 版本；.optional() 对应 forge newSimpleChannel 的 str -> true（接受任意版本）
        IPayloadRegistrar registrar = event.registrar(YesSteveModel.MOD_ID).optional();
        registerPayload(registrar, FRAGMENT_DISCRIMINATOR);
        for (Map.Entry<Integer, LocalCodec<?>> entry : codecs.entrySet()) {
            registerPayload(registrar, entry.getKey());
        }
        channelRegistered = true;
    }

    private static void registerPayload(IPayloadRegistrar registrar, int discriminator) {
        ResourceLocation id = payloadId(discriminator);
        idByDiscriminator.put(discriminator, id);
        registrar.play(id,
                (FriendlyByteBuf.Reader<Wrapped<?>>) buf -> Wrapped.decode(buf, id, discriminator),
                (payload, context) -> dispatch(payload, context));
    }

    private static ResourceLocation payloadId(int discriminator) {
        return new ResourceLocation("yes_steve_model", "channel.disc." + discriminator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatch(Wrapped<?> payload, net.neoforged.neoforge.network.handling.PlayPayloadContext context) {
        int discriminator = payload.discriminator;
        LocalCodec codec = discriminator == FRAGMENT_DISCRIMINATOR ? null : codecs.get(discriminator);
        if (codec != null) {
            // 方向守卫（forge NetworkDirection 传输层强制的 neoforge 等价物）
            PacketDirection expected = context.flow() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND ? PacketDirection.PLAY_TO_CLIENT : PacketDirection.PLAY_TO_SERVER;
            if (codec.direction != expected) {
                return;
            }
            codec.handler.accept(payload.message, new PacketContextImpl(context));
            return;
        }
        // 未注册/分片 discrimator：分片信封本体（Wrapped 包 FragmentPacket）
        Object message = payload.message;
        if (message instanceof FragmentPacket fragmentPacket) {
            handleFragment(fragmentPacket, new PacketContextImpl(context));
        }
    }

    public static <T> void register(int discriminator, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        codecs.put(discriminator & 0xff, new LocalCodec<>(type, decoder, handler, encoder, direction));
        discriminatorByType.put(type, discriminator & 0xff);
    }

    public static void sendToServer(Object packet) {
        if (packet instanceof C2SModelSyncPayload && NetworkHandler.serverSupportsModelSyncFragments()) {
            byte[] encoded = encode(packet);
            if (encoded.length > FRAGMENT_DATA_SIZE) {
                sendFragments(encoded, fragments -> PacketDistributor.SERVER.noArg().send(fragments));
                return;
            }
        }
        PacketDistributor.SERVER.noArg().send(wrap(packet));
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        PacketDistributor.PLAYER.with(player).send(wrappedArray(packet));
    }

    public static void sendToAll(Object packet) {
        PacketDistributor.ALL.noArg().send(wrappedArray(packet));
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        PacketDistributor.TRACKING_ENTITY.with(entity).send(wrappedArray(packet));
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player).send(wrappedArray(packet));
    }

    public static Packet<?> toClientboundPacket(Object packet) {
        return new ClientboundCustomPayloadPacket(wrap(packet));
    }

    public static List<Packet<?>> toClientboundPackets(Object packet) {
        byte[] encoded = encode(packet);
        if (encoded.length <= FRAGMENT_DATA_SIZE) {
            List<Packet<?>> packets = new ArrayList<>(1);
            packets.add(toClientboundPacket(packet));
            return packets;
        }
        List<Packet<?>> packets = new ArrayList<>();
        int transferId = nextTransferId.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            packets.add(new ClientboundCustomPayloadPacket(wrapFragment(new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)))));
        }
        return packets;
    }

    public static Packet<?> toServerboundPacket(Object packet) {
        return new ServerboundCustomPayloadPacket(wrap(packet));
    }

    private static CustomPacketPayload[] wrappedArray(Object packet) {
        return new CustomPacketPayload[]{wrap(packet)};
    }

    private static int discriminatorOf(Object packet) {
        Class<?> clazz = packet.getClass();
        Integer discriminator = discriminatorByType.get(clazz);
        while (discriminator == null && clazz.getSuperclass() != null) {
            clazz = clazz.getSuperclass();
            discriminator = discriminatorByType.get(clazz);
        }
        if (discriminator == null) {
            throw new IllegalArgumentException("Unregistered YSM packet type: " + packet.getClass());
        }
        return discriminator;
    }

    private static Wrapped<?> wrap(Object packet) {
        if (packet instanceof FragmentPacket fragmentPacket) {
            return wrapFragment(fragmentPacket);
        }
        int discriminator = discriminatorOf(packet);
        return new Wrapped<>(payloadId(discriminator), discriminator, packet, codecOf(discriminator).encoder);
    }

    private static Wrapped<FragmentPacket> wrapFragment(FragmentPacket fragmentPacket) {
        return new Wrapped<>(payloadId(FRAGMENT_DISCRIMINATOR), FRAGMENT_DISCRIMINATOR, fragmentPacket, FragmentPacket::encode);
    }

    @SuppressWarnings("unchecked")
    private static LocalCodec<Object> codecOf(int discriminator) {
        return (LocalCodec<Object>) codecs.get(discriminator);
    }

    private static byte[] encode(Object packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            // forge SimpleChannel.encodeMessage 的编码首字节即 discriminator（handleFragment
            // 重排后 readUnsignedByte 读取的就是它）——Wrapped 直发路径由 payload id 路由
            // 无此字节，分片内数据必须显式补齐，否则重排流错位一位
            int discriminator = discriminatorOf(packet);
            buf.writeByte(discriminator & 0xff);
            codecOf(discriminator).encoder.accept(packet, buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void sendFragments(byte[] encoded, java.util.function.Consumer<CustomPacketPayload[]> sender) {
        int transferId = nextTransferId.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            sender.accept(new CustomPacketPayload[]{wrapFragment(new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)))});
        }
    }

    private static void handleFragment(FragmentPacket packet, PacketContext context) {
        PacketDirection expected = context.isServerSide() ? PacketDirection.PLAY_TO_SERVER : PacketDirection.PLAY_TO_CLIENT;

        long now = System.nanoTime();
        io.netty.channel.Channel channel = context.getConnection().channel();
        Map<Integer, FragmentAccumulator> newTransfers = new ConcurrentHashMap<>();
        Map<Integer, FragmentAccumulator> transfers = incomingFragments.putIfAbsent(channel, newTransfers);
        if (transfers == null) {
            transfers = newTransfers;
            Map<Integer, FragmentAccumulator> registeredTransfers = transfers;
            channel.closeFuture().addListener(ignored -> incomingFragments.remove(channel, registeredTransfers));
        }
        transfers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateNanos > FRAGMENT_TIMEOUT_NANOS);

        FragmentAccumulator accumulator = transfers.computeIfAbsent(packet.transferId(), ignored -> new FragmentAccumulator(packet.fragmentCount()));
        byte[] complete = accumulator.add(packet, now);
        if (complete == null) {
            return;
        }
        transfers.remove(packet.transferId(), accumulator);
        if (transfers.isEmpty()) {
            incomingFragments.remove(context.getConnection().channel(), transfers);
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));
        try {
            int discriminator = buf.readUnsignedByte();
            LocalCodec<?> codec = codecs.get(discriminator);
            if (codec == null || codec.direction != expected) {
                throw new IllegalArgumentException("Invalid fragmented YSM packet discriminator: " + discriminator);
            }
            codec.dispatch(buf, context);
            if (buf.isReadable()) {
                throw new IllegalArgumentException("Fragmented YSM packet left " + buf.readableBytes() + " unread bytes");
            }
        } finally {
            buf.release();
        }
    }

    /**
     * payload 信封：适配 CustomPacketPayload.write/id 契约与门面的 (encoder, T) 形态。
     * 解码侧由 Wrapped.decode 构造（先解出原始消息对象再包信封，handler 拆封后分发）。
     */
    private static final class Wrapped<T> implements CustomPacketPayload {
        private final ResourceLocation id;
        private final int discriminator;
        private final T message;
        private final BiConsumer<T, FriendlyByteBuf> encoder;

        Wrapped(ResourceLocation id, int discriminator, T message, BiConsumer<T, FriendlyByteBuf> encoder) {
            this.id = id;
            this.discriminator = discriminator;
            this.message = message;
            this.encoder = encoder;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            encoder.accept(message, buf);
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @SuppressWarnings("unchecked")
        static Wrapped<?> decode(FriendlyByteBuf buf, ResourceLocation id, int discriminator) {
            if (discriminator == FRAGMENT_DISCRIMINATOR) {
                FragmentPacket fragment = FragmentPacket.decode(buf);
                return new Wrapped<>(id, discriminator, fragment, FragmentPacket::encode);
            }
            LocalCodec<Object> codec = codecOf(discriminator);
            Object message = codec.decoder.apply(buf);
            return new Wrapped<>(id, discriminator, message, codec.encoder);
        }
    }

    private static final class FragmentPacket {
        final int transferId;
        final int fragmentIndex;
        final int fragmentCount;
        final byte[] data;

        FragmentPacket(int transferId, int fragmentIndex, int fragmentCount, byte[] data) {
            this.transferId = transferId;
            this.fragmentIndex = fragmentIndex;
            this.fragmentCount = fragmentCount;
            this.data = data;
        }

        int transferId() {
            return this.transferId;
        }

        int fragmentIndex() {
            return this.fragmentIndex;
        }

        int fragmentCount() {
            return this.fragmentCount;
        }

        byte[] data() {
            return this.data;
        }

        private static void encode(FragmentPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.transferId);
            buf.writeVarInt(packet.fragmentIndex);
            buf.writeVarInt(packet.fragmentCount);
            buf.writeByteArray(packet.data);
        }

        private static FragmentPacket decode(FriendlyByteBuf buf) {
            return new FragmentPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(FRAGMENT_DATA_SIZE));
        }
    }

    private static final class FragmentAccumulator {
        private final byte[][] fragments;
        private int received;
        private int totalSize;
        private volatile long lastUpdateNanos = System.nanoTime();

        private FragmentAccumulator(int fragmentCount) {
            if (fragmentCount <= 0 || fragmentCount > MAX_FRAGMENT_COUNT) {
                throw new IllegalArgumentException("Invalid YSM fragment count: " + fragmentCount);
            }
            this.fragments = new byte[fragmentCount][];
        }

        private synchronized byte[] add(FragmentPacket packet, long now) {
            if (packet.fragmentCount() != fragments.length || packet.fragmentIndex() < 0 || packet.fragmentIndex() >= fragments.length) {
                throw new IllegalArgumentException("Inconsistent YSM fragment metadata");
            }
            lastUpdateNanos = now;
            if (fragments[packet.fragmentIndex()] == null) {
                fragments[packet.fragmentIndex()] = packet.data();
                received++;
                totalSize += packet.data().length;
                if (totalSize > MAX_REASSEMBLED_SIZE) {
                    throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
                }
            }
            if (received != fragments.length) {
                return null;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream(totalSize);
            for (byte[] fragment : fragments) {
                output.write(fragment, 0, fragment.length);
            }
            return output.toByteArray();
        }
    }

    private static final class LocalCodec<T> {
        final Class<T> type;
        final Function<FriendlyByteBuf, T> decoder;
        final BiConsumer<T, PacketContext> handler;
        final BiConsumer<Object, FriendlyByteBuf> encoder;
        final PacketDirection direction;

        @SuppressWarnings("unchecked")
        LocalCodec(Class<T> type, Function<FriendlyByteBuf, T> decoder,
                   BiConsumer<T, PacketContext> handler, BiConsumer<T, FriendlyByteBuf> encoder, PacketDirection direction) {
            this.type = type;
            this.decoder = decoder;
            this.handler = handler;
            this.encoder = (BiConsumer<Object, FriendlyByteBuf>) encoder;
            this.direction = direction;
        }

        private void dispatch(FriendlyByteBuf buf, PacketContext context) {
            handler.accept(decoder.apply(buf), context);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LocalCodec)) {
                return false;
            }
            LocalCodec<?> other = (LocalCodec<?>) obj;
            return Objects.equals(this.type, other.type) && Objects.equals(this.decoder, other.decoder)
                    && Objects.equals(this.handler, other.handler) && Objects.equals(this.direction, other.direction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.decoder, this.handler, this.direction);
        }
    }
}
