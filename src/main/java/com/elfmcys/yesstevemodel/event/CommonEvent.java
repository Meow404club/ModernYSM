package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
//? if neoforge
/*import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;*/
//? if forge
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//? if forge
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//? if neoforge && <1.20.3
/*import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;*/
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
        // neoforge 三线统一走主类孪生构造期注入的 mod 事件总线（FMLJavaModLoadingContext
        // 1.21 已删除，net.neoforged.fml.javafmlmod 包不复存在）；forge 线保持原口
        //? if neoforge && >=1.20.3
        /*com.elfmcys.yesstevemodel.platform.neoforge.YesSteveModelForge.getModEventBus().addListener(CommonEvent::onCommonSetup);*/
        //? if neoforge && <1.20.3
        /*FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonEvent::onCommonSetup);*/
        //? if forge
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
        // forge 32/33/34（<1.16.4）自家命令参数类型（ModIdArgument/EnumArgument）从未注册
        // ArgumentTypes → 命令树同步写占位 "minecraft:" → 客户端反序列化失败进服必断连
        // （unimined-env-116x；两侧都要注册，故挂 common setup）。
        // 注意必须块包裹形态：1201 vcs 直通线下裸行会常驻生效（与 35+ 代 ForgeMod 注册撞车）
        //? if forge && <1.16.4 {
        /*com.elfmcys.yesstevemodel.platform.forge.ForgeArgTypeSyncShim.register();*/
        //?}
    }
}
