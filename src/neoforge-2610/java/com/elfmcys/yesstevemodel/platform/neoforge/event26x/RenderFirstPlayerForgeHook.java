package com.elfmcys.yesstevemodel.platform.neoforge.event26x;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueFeatures;

/**
 * 26.x 代孪生（2111 树的 RenderFirstPlayerForgeHook 26.x 分歧版）。
 * 异包原因（212 树先例）：2111 原件经 sourceSet 级 exclude 剔除时按相对路径双杀同名 RAW 文件，
 * 本孪生必须异包（event26x）才能与 exclude 共存。@EventBusSubscriber 注解自注册，包名无关。
 * 分歧点：26.x render-dag 再换代删 AfterEntities 子事件（neoforge-26.1
 * RenderLevelStageEvent.java 子事件列 AfterSky/AfterOpaqueBlocks/AfterOpaqueFeatures/...），
 * 取 AfterOpaqueFeatures（javadoc："after opaque features from entities, block entities and
 * particles have been rendered"——实体/块实体档语义直系后继；时点差异继续记功能债）。
 * RenderHandEvent.getPoseStack/getPartialTick 面 26.1 同形（neoforge-26.1 RenderHandEvent 实证）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class RenderFirstPlayerForgeHook {
    private RenderFirstPlayerForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(AfterOpaqueFeatures event) {
        RenderFirstPlayerBackground.resetFrame();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(), event.getPackedLight(), event.getPartialTick());
    }
}
