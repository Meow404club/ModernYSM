package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 26.2/26.3 PiP 预览渲染器注册（debt-262-preview-pip；262/263 代树各一份孪生拷贝）。
 *
 * 官方入口：EntityRenderersEvent.RegisterRenderers.registerEntityRenderer →
 * EntityRenderers.register = PROVIDERS.put（EntityRenderers.java:28 实证，可覆写）。
 * 覆写 EntityTypes.PLAYER 槽位的可观测面=按 entityType 查表且 state 非 AvatarRenderState
 * 的解析——vanilla 玩家实体（AbstractClientPlayer→avatar 分支）与玩家状态
 * （AvatarRenderState→playerRenderers 分支，EntityRenderDispatcher.java:94-99/112-118 实证）
 * 均绕开本表项；唯一消费者=我方 YsmPreviewRenderState（预览 PiP state），故对 vanilla
 * 与真实玩家渲染零行为差。注册实例仅供 dispatcher 解析（预览 PiP submit 路径），
 * RendererManager 的 Static 直绘实例不变，世界玩家渲染仍走 RenderPlayerEvent 事件链。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class PreviewRendererRegisterHook {

    private PreviewRendererRegisterHook() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.PLAYER, CustomPlayerRenderer::new);
    }
}
