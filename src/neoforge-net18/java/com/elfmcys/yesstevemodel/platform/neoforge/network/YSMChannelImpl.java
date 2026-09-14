package com.elfmcys.yesstevemodel.platform.neoforge.network;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.platform.neoforge.YesSteveModelForge;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.PacketDirection;
import rip.ysm.util.Rl;

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
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * [M4 锚点落地] forge 原类的 SimpleChannel 体系在 NeoForge 1.20.4 已被 payload 体系取代；
 * 1.20.5+ 又一次重铸（neoforge-1.20.6 源码实证）：
 * - RegisterPayloadHandlerEvent → RegisterPayloadHandlersEvent（net.neoforged.neoforge.network.event，
 *   RegisterPayloadHandlersEvent.java:40 registrar(String version)=网络版本字符串非命名空间）；
 * - IPayloadRegistrar.play(RL, Reader, handler) → PayloadRegistrar（具体类）.playBidirectional(
 *   CustomPacketPayload.Type&lt;T&gt;, StreamCodec&lt;? super RegistryFriendlyByteBuf, T&gt;, IPayloadHandler&lt;T&gt;)；
 * - Wrapped 信封从 write(FriendlyByteBuf)/id() 契约改为 type()（Type 为 ResourceLocation 记录，
 *   编解码路由按 type().id() 查连接期注册表，CustomPacketPayload.java codec(...)），
 *   编码经 StreamCodec.ofMember 桥接原 BiConsumer&lt;T,FriendlyByteBuf&gt;（RegistryFriendlyByteBuf
 *   is-a FriendlyByteBuf，零拷贝直通）；
 * - PacketDistributor.SERVER/PLAYER/ALL/TRACKING_ENTITY(.AND_SELF).send →
 *   sendToServer/sendToPlayer/sendToAllPlayers/sendToPlayersTrackingEntity(AndSelf) 静态方法
 *   （1.20.6 PacketDistributor.java:38-100 实证，varargs 多 payload）；
 * - 方向守卫移入分发 lambda（context.flow() 判定，语义同 1.20.4 代）；分片协议/版本协商口径不变。
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
        // RegisterPayloadHandlersEvent 在构造后的加载阶段触发，门面全部 register 均已就位。
        YesSteveModelForge.getModEventBus().addListener(YSMChannelImpl::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // registrar 参数=网络版本（1.20.6 RegisterPayloadHandlersEvent.java:40 "The network version.
        // May not be empty"，语义与 1.20.4 的命名空间不同）；.optional() 对应 forge
        // newSimpleChannel 的 str -> true（接受任意版本）
        PayloadRegistrar registrar = event.registrar("1").optional();
        registerPayload(registrar, FRAGMENT_DISCRIMINATOR);
        for (Map.Entry<Integer, LocalCodec<?>> entry : codecs.entrySet()) {
            registerPayload(registrar, entry.getKey());
        }
        channelRegistered = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerPayload(PayloadRegistrar registrar, int discriminator) {
        ResourceLocation id = payloadId(discriminator);
        idByDiscriminator.put(discriminator, id);
        // 1.21.8 3 参 playBidirectional 的 clientHandler 固定 null（21854 PayloadRegistrar.java:70/80，
        // javadoc 明示客户端 handler 须走 RegisterClientPayloadHandlersEvent——1.21.5 的 3 参形
        // 仍是单 handler 双向注册，21598 PayloadRegistrar.java:62 实证，静默语义变更）
        // → 显式同 handler 双向注册，否则客户端 payload 全部「missing client-side handlers」崩加载
        //（与 2110 树 21.10 同修法）
        IPayloadHandler<Wrapped<?>> ysmHandler = (payload, context) -> dispatch((Wrapped<?>) payload, context);
        registrar.playBidirectional((CustomPacketPayload.Type) new CustomPacketPayload.Type<Wrapped<?>>(id),
                StreamCodec.ofMember(
                        (Wrapped<?> payload, RegistryFriendlyByteBuf buf) -> payload.encode(buf),
                        (RegistryFriendlyByteBuf buf) -> Wrapped.decode(buf, id, discriminator)),
                ysmHandler, ysmHandler);
    }

    private static ResourceLocation payloadId(int discriminator) {
        // Rl 工厂收编构造点（1.21 起 new ResourceLocation 私有、1.20.6 仍 public——
        // 本孪生为 1.20.6/1.21.1 共用 RAW 树，构造统一走 stonecutter 条件化的 Rl）
        return Rl.of("yes_steve_model", "channel.disc." + discriminator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatch(Wrapped<?> payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
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
                sendFragments(encoded, fragments -> {
                    for (CustomPacketPayload fragment : fragments) {
                        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(fragment);
                    }
                });
                return;
            }
        }
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(wrap(packet));
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, wrap(packet));
    }

    public static void sendToAll(Object packet) {
        PacketDistributor.sendToAllPlayers(wrap(packet));
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, wrap(packet));
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, wrap(packet));
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
     * payload 信封（1.20.5+ 契约）：CustomPacketPayload 只剩 type() 抽象方法，编码由
     * 连接期注册的 StreamCodec 承担（本类 registerPayload 的 ofMember 桥）——信封自身只提供
     * encode(FriendlyByteBuf) 供 ofMember 成员编码器调用。Type 每次新建记录实例，
     * 路由按 type().id() 的 ResourceLocation 相等性，无缓存依赖。
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
        public CustomPacketPayload.Type<?> type() {
            return new CustomPacketPayload.Type<>(this.id);
        }

        void encode(FriendlyByteBuf buf) {
            this.encoder.accept(this.message, buf);
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
