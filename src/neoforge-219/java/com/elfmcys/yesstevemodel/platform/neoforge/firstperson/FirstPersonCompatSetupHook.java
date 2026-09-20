package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * neoforge 21.9~21.11 三线 client setup 挂接 + 第二 extractor 注册
 * （fpm-selfdrive-219-2111 卡，261 卡 FirstPersonCompatSetupHook 平移）。
 *
 * client setup：261 卡先例（neoforge 线无 1.20.1 ForgeClientSetupHooks 挂点；
 * loader 11+ @EventBusSubscriber 无 bus 属性，AutomaticEventSubscriber 按
 * IModBusEvent 参数类型自动路由 mod 总线）。
 *
 * 第二 extractor：RegisterRenderStateModifiersEvent 的 modifier 在 vanilla
 * extractRenderState 尾回调（21.9/21.10/21.11 三代 javap/源码实证同签名：
 * registerEntityModifier(TypeToken/Class, BiConsumer)，tmp/refs neoforge-1.21.11
 * RegisterRenderStateModifiersEvent.java:54-81 + 21.9.16/21.10.0-beta
 * universal.jar javap）。与 2111 树 PlayerRenderStateEntityCache 的既有
 * modifier 并存（RenderStateExtensions.registerEntity 集合 add 多 modifier
 * 共存，261 卡实证）。TypeToken 匿名子类写法照搬 PlayerRenderStateEntityCache
 * （AvatarRenderer 泛型化 → 通配符 {@code <?>} 让 E/S 落 bound，lambda 内收窄）。
 *
 * 采样判据（spec）：FPM 在场 && FPM 旗标在 extract 窗口内 && 实体==相机实体
 * （本地玩家——FPM 2.6.0/2.7.2 WorldRendererMixin.extractEntity 对同一实体经
 * AvatarRenderer 走 vanilla extract → 本 modifier 回调，覆盖 FPM 对 YSM 的
 * re-extract 路径）。自家 extractor 标自家 state 旗标，不经 FPM mixin，
 * 天然避开第三方捕获污染（研究卡论证）。
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
                        FirstPersonCompat.sample(entity)));
    }

}
