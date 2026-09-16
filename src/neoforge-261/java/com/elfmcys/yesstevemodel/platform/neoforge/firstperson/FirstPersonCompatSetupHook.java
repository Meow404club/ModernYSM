package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * neoforge 26.1 三线 client setup 挂接 + 旗标采样 modifier 注册
 * （fpm-26x-pr659 卡A + 26.1.2 逆向追加单）。
 *
 * client setup：镜像 forge 端 ForgeClientSetupHooks.onClientSetup 的
 * FMLClientSetupEvent 挂法（ForgeClientSetupHooks.java:60-66）：neoforge
 * 对应时点=同名事件 FMLClientSetupEvent（loader-11.0.5/11.0.13 javap：
 * extends ParallelDispatchEvent→ModLifecycleEvent implements IModBusEvent，
 * enqueueWork(Runnable) 在位）。@EventBusSubscriber 无 bus 属性（loader 11
 * 注解仅 value/modid 两成员，javap 实证），AutomaticEventSubscriber 按
 * @SubscribeEvent 方法参数类型 isAssignableFrom IModBusEvent 自动路由
 * mod 总线（loader-11.0.13 AutomaticEventSubscriber 字节码 invoke
 * ModContainer.getEventBus 实证）。
 *
 * 旗标采样：RegisterRenderStateModifiersEvent 的 modifier 在 vanilla
 * extractRenderState 尾回调（neoforge-26.1 RenderStateExtensions
 * .onUpdateEntityRenderState:41-57），加宽版 FPM 的 cameraEntityExtract
 * 窗口恰好覆盖相机实体的 extractEntity → 此处 FirstPersonCompat.sample()
 * 采到 true（stock FPM 恒 false=官方 stock 行为）；采样值经
 * FirstPersonCompat.putSample 存随 state 走的槽。与 2111 树
 * PlayerRenderStateEntityCache 的 modifier 并存（registerEntity 集合追加，
 * RenderStateExtensions.java:74 实证）；TypeToken 匿名子类写法照搬该文件
 * （AvatarRenderer 泛型化 → 显式定位 E/S，2110 编译实证）。
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

    @SubscribeEvent
    public static void onRegisterModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new com.google.common.reflect.TypeToken<net.minecraft.client.renderer.entity.player.AvatarRenderer<?>>() {},
                (entity, state) -> FirstPersonCompat.putSample(
                        (net.minecraft.client.renderer.entity.state.EntityRenderState) state,
                        FirstPersonCompat.sample()));
    }

}
