package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * neoforge 26.1 三线 client setup 挂接（fpm-26x-pr659 卡A）。
 * 镜像 forge 端 ForgeClientSetupHooks.onClientSetup 的 FMLClientSetupEvent
 * 挂法（ForgeClientSetupHooks.java:60-66）：neoforge 对应时点=同名事件
 * FMLClientSetupEvent（loader-11.0.5/11.0.13 javap：extends
 * ParallelDispatchEvent→ModLifecycleEvent implements IModBusEvent，
 * enqueueWork(Runnable) 在位）。@EventBusSubscriber 无 bus 属性（loader 11
 * 注解仅 value/modid 两成员，javap 实证），AutomaticEventSubscriber 按
 * @SubscribeEvent 方法参数类型 isAssignableFrom IModBusEvent 自动路由
 * mod 总线（loader-11.0.13 AutomaticEventSubscriber 字节码 invoke
 * ModContainer.getEventBus 实证）——RenderFirstPlayerForgeHook（2610 树）
 * 同款注解自注册，包名无关。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonCompatSetupHook {

    private FirstPersonCompatSetupHook() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.enqueueWork(FirstPersonCompat::init);
    }

}
