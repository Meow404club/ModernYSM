package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ClientPlayerCloneEvent {

    private ClientPlayerCloneEvent() {
    }

    public static void register() {
        // architectury ClientPlayerEvent.CLIENT_PLAYER_RESPAWN 在 forge 端 = ClientPlayerNetworkEvent.Clone（不可取消）
        //? if <1.19.2 {
        /*MinecraftForge.EVENT_BUS.addListener(ClientPlayerCloneEvent::onClientPlayerRespawnEvent);
         *///?} else {
        MinecraftForge.EVENT_BUS.addListener(ClientPlayerCloneEvent::onClientPlayerClone);
        //?}
    }

    // 1.16.5 无 Clone 事件（jar 检索）：对位 RespawnEvent（getOldPlayer/getNewPlayer 同构）
    //? if <1.19.2 {
    /*private static void onClientPlayerRespawnEvent(ClientPlayerNetworkEvent.RespawnEvent event) {
        onClientPlayerRespawn(event.getOldPlayer(), event.getNewPlayer());
    }
     *///?} else {
    private static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        onClientPlayerRespawn(event.getOldPlayer(), event.getNewPlayer());
    }
    //?}

    private static void onClientPlayerRespawn(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        // 原 rip.ysm CapabilityLifecycle 门面退役：revive/invalidate 即 Entity 两同名方法直调
        //（Clone 读旧 caps 时序与 server 侧 CapabilityEvent.onPlayerCloned 一致：revive -> copy -> invalidate）
        // 1.16.x reviveCaps/invalidateCaps 为 protected 且 clone 事件期旧 caps 保持有效
        //（forge-1.16.x PlayerList patch 实证，thinlayer 卡结论）→ <1.17 不调 revive/invalidate
        //? if >=1.17 {
        oldPlayer.reviveCaps();
        //?}
        PlayerCapability.get(oldPlayer).ifPresent(cap -> PlayerCapability.get(newPlayer).ifPresent(cap2 -> cap2.copyFrom(cap)));
        //? if >=1.17 {
        oldPlayer.invalidateCaps();
        //?}
    }
}
