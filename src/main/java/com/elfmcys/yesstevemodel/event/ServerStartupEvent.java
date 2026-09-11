package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
// 1.17+ net.minecraftforge.event.server ↔ 1.16.5 net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent
//? if <1.17 {
/*import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
 *///?} else {
import net.minecraftforge.event.server.ServerAboutToStartEvent;
//?}

public final class ServerStartupEvent {

    private ServerStartupEvent() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ServerStartupEvent::onServerAboutToStart);
    }

    //? if <1.17
    /*private static void onServerAboutToStart(FMLServerAboutToStartEvent event) {*/
    //? if >=1.17
    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (ServerModelManager.canReuseLoadedModels()) {
            return;
        }
        ServerModelManager.loadModels(result -> {
            if (!result.isSuccess()) {
                server.execute(() -> {
                    throw new RuntimeException("YSM Loading Failed: " + result.getErrorMessage().getString(256));
                });
            }
        }, null);
    }
}
