package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class PlayerLogoutEvent {

    private PlayerLogoutEvent() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(PlayerLogoutEvent::onPlayerLoggedOut);
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        NetworkHandler.clearClientModelSyncFragments(player.getUUID());
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        ServerModelManager.syncModelToPlayer(player.getUUID());
    }
}
