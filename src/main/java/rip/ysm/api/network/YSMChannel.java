package rip.ysm.api.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import rip.ysm.api.network.platform.forge.YSMChannelImpl;

public final class YSMChannel {

    private YSMChannel() {
    }

    @ExpectPlatform
    public static void init(ResourceLocation channelId, String version) {
        YSMChannelImpl.init(channelId, version);
    }

    @ExpectPlatform
    public static <T> void register(int discriminator, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        YSMChannelImpl.register(discriminator, type, encoder, decoder, handler, direction);
    }

    @ExpectPlatform
    public static void sendToServer(Object packet) {
        YSMChannelImpl.sendToServer(packet);
    }

    @ExpectPlatform
    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        YSMChannelImpl.sendToClientPlayer(packet, player);
    }

    @ExpectPlatform
    public static void sendToAll(Object packet) {
        YSMChannelImpl.sendToAll(packet);
    }

    @ExpectPlatform
    public static void sendToTrackingEntity(Object packet, Entity entity) {
        YSMChannelImpl.sendToTrackingEntity(packet, entity);
    }

    @ExpectPlatform
    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        YSMChannelImpl.sendToTrackingEntityAndSelf(packet, player);
    }

    @ExpectPlatform
    public static Packet<?> toClientboundPacket(Object packet) {
        return YSMChannelImpl.toClientboundPacket(packet);
    }

    @ExpectPlatform
    public static List<Packet<?>> toClientboundPackets(Object packet) {
        return YSMChannelImpl.toClientboundPackets(packet);
    }

    @ExpectPlatform
    public static Packet<?> toServerboundPacket(Object packet) {
        return YSMChannelImpl.toServerboundPacket(packet);
    }
}
