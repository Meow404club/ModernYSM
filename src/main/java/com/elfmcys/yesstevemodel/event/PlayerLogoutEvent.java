package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if neoforge
/*import net.neoforged.neoforge.event.entity.player.PlayerEvent;*/
//? if forge
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class PlayerLogoutEvent {

    private PlayerLogoutEvent() {
    }

    public static void register() {
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(PlayerLogoutEvent::onPlayerLoggedOut);*/
        //? if forge
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
