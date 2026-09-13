package com.elfmcys.yesstevemodel.platform.neoforge.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * forge 原类包 NetworkEvent.Context，neoforge 1.20.4 重铸为 PlayPayloadContext（record），
 * 1.20.5+ 再铸为 IPayloadContext 接口（neoforge-1.20.6 handling/IPayloadContext.java 实证）：
 * - 方向：getDirection().getReceptionSide() → flow()（PacketFlow 判定同前代）；
 * - 服务端发送者：getSender() → player()（1.20.5+ 直接返回 Player 非 Optional，
 *   serverbound=发送者 ServerPlayer；接口 javadoc "For server-bound payloads, retrieves the
 *   sending ServerPlayer"——非 ServerPlayer 仅配置阶段语义，PLAY 阶段直取）；
 * - 连接：getNetworkManager() → connection() 默认方法（listener().getConnection() 直达，
 *   不再经 channelHandlerContext().pipeline() 查找）；
 * - 主线程排队：enqueueWork → enqueueWork（CompletableFuture 返回值忽略，fire-and-forget 同义）。
 */
final class PacketContextImpl implements PacketContext {

    private final IPayloadContext context;

    PacketContextImpl(IPayloadContext context) {
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
        return context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Override
    public Connection getConnection() {
        return context.connection();
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.enqueueWork(runnable);
    }
}
