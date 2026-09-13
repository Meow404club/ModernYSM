package com.elfmcys.yesstevemodel.platform.neoforge.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。forge 原类包 NetworkEvent.Context，neoforge 20.4 重铸后
 * 为 PlayPayloadContext（record；20.4.251 javap 实证）：
 * - 方向：getDirection().getReceptionSide() → flow()（PacketFlow.isClient/isServer 同 vanilla 面）；
 * - 服务端发送者：getSender() → player()（Optional&lt;Player&gt;，filter ServerPlayer）；
 * - 连接：getNetworkManager() → channelHandlerContext().pipeline().get(Connection.class)；
 * - 主线程排队：enqueueWork → workHandler().execute。
 */
final class PacketContextImpl implements PacketContext {

    private final PlayPayloadContext context;

    PacketContextImpl(PlayPayloadContext context) {
        this.context = context;
    }

    @Override
    public boolean isClientSide() {
        return context.flow() == PacketFlow.CLIENTBOUND;
    }

    @Override
    public boolean isServerSide() {
        return context.flow() == PacketFlow.SERVERBOUND;
    }

    @Override
    public @Nullable ServerPlayer getSender() {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return null;
        }
        return context.player()
                .filter(player -> player instanceof ServerPlayer)
                .map(player -> (ServerPlayer) player)
                .orElse(null);
    }

    @Override
    public Connection getConnection() {
        return context.channelHandlerContext().pipeline().get(Connection.class);
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.workHandler().execute(runnable);
    }
}
