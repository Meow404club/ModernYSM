package com.elfmcys.yesstevemodel.platform.forge.network;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
// 1.16.x NetworkEvent 在 fml.network 包（Context 的 getDirection/getSender/getNetworkManager/
// enqueueWork/setPacketHandled 方法面与 1.20.1 同签名，javap 实证）——仅包移动条件化
//? if >=1.17 {
import net.minecraftforge.network.NetworkEvent;
//?} else {
/*import net.minecraftforge.fml.network.NetworkEvent;*/
//?}
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

import java.util.function.Supplier;

final class PacketContextImpl implements PacketContext {

    private final Supplier<NetworkEvent.Context> contextSupplier;

    PacketContextImpl(Supplier<NetworkEvent.Context> contextSupplier) {
        this.contextSupplier = contextSupplier;
    }

    @Override
    public boolean isClientSide() {
        return contextSupplier.get().getDirection().getReceptionSide().isClient();
    }

    @Override
    public boolean isServerSide() {
        return contextSupplier.get().getDirection().getReceptionSide().isServer();
    }

    @Override
    public @Nullable ServerPlayer getSender() {
        return contextSupplier.get().getSender();
    }

    @Override
    public Connection getConnection() {
        return contextSupplier.get().getNetworkManager();
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        contextSupplier.get().enqueueWork(runnable);
    }
}
