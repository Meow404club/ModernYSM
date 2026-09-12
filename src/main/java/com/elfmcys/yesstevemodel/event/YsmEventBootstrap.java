package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.client.event.*;
import com.elfmcys.yesstevemodel.client.input.*;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.platform.YsmPlatform;
import rip.ysm.api.PlatformAPI;

public final class YsmEventBootstrap {

    private YsmEventBootstrap() {
    }

    public static void register() {
        ServerStartupEvent.register();
        EnterServerEvent.register();
        PlayerLogoutEvent.register();
        CommonEvent.register();
        CommandRegistry.register();
        // 1.16.5 capability 注册已随 CommonEvent.onCommonSetup（FMLCommonSetupEvent）延后——
        // 构造期 CapabilityManager.INSTANCE.register 在 1.16.x 必 NPE（callbacks 仅由
        // injectCapabilities 赋值，见 CapabilityEvent#registerCapabilities 注释）
        if (!PlatformAPI.isServer()) {
            EntityJoinCallbackEvent.register();
            ClientSetupEvent.register();
            ClientTickEvent.register();
            ClientPlayerJoinNotification.register();
            ClientPlayerCloneEvent.register();
            AnimationLockEvent.register();
            PlayerSkinTextureManager.register();
            PlayerModelToggleKey.register();
            AnimationRouletteKey.register();
            DebugAnimationKey.register();
            ExtraPlayerRenderKey.register();
            ExtraAnimationKey.register();
            InputStateKey.register();
            // 测试基建（harness/tour.sh 走查）：仅 dev 激活。生产 jar 已 exclude
            // rip/ysm/harness（构建脚本），此守卫同时保证 invokestatic 永不执行——
            // JVM 惰性解析下未执行的指令不解析被调类，缺类也不炸（实验实证：
            // guard=false 缺类存活 / guard=true NoClassDefFoundError）。
            if (YsmPlatform.isDevelopmentEnvironment()) {
                rip.ysm.harness.GuiTourDriver.register();
            }
        }
    }
}
