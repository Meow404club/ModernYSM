package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
//? if neoforge
/*import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;*/
//? if forge
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//? if neoforge
/*import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;*/
//? if forge
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;

import java.io.IOException;

public final class CommonEvent {

    private CommonEvent() {
    }

    public static Object nativeInit() {
        try {
            ServerModelManager.reloadPacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonEvent::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!YesSteveModel.isAvailable()) {
            YesSteveModel.LOGGER.error(YesSteveModel.getErrorMessage());
            return;
        }
        // 1.16.5 capability 注册须发生在 FMLCommonSetupEvent：构造期 register 即
        // CapabilityManager.callbacks==null NPE（CapabilityManager.java:65，dev runServer 实测；
        // 详见 CapabilityEvent#registerCapabilities 注释）
        CapabilityEvent.registerCapabilities();
        NetworkHandler.init();
        TouhouMaidCompat.init();
        nativeInit();
    }
}
