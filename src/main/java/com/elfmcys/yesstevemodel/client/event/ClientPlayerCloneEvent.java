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
        MinecraftForge.EVENT_BUS.addListener(ClientPlayerCloneEvent::onClientPlayerClone);
    }

    private static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        onClientPlayerRespawn(event.getOldPlayer(), event.getNewPlayer());
    }

    private static void onClientPlayerRespawn(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        // 原 rip.ysm CapabilityLifecycle 门面退役：revive/invalidate 即 Entity 两同名方法直调
        //（Clone 读旧 caps 时序与 server 侧 CapabilityEvent.onPlayerCloned 一致：revive -> copy -> invalidate）
        oldPlayer.reviveCaps();
        PlayerCapability.get(oldPlayer).ifPresent(cap -> PlayerCapability.get(newPlayer).ifPresent(cap2 -> cap2.copyFrom(cap)));
        oldPlayer.invalidateCaps();
    }
}
