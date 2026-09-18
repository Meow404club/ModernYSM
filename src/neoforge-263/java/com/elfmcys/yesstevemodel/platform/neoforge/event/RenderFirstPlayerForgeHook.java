package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueFeatures;

/**
 * 26.2 代孪生（2111 树的 RenderFirstPlayerForgeHook 26.x 分歧版吸收+26.2 分歧；全树分代
 * 挂载，同 FQCN——2610 event26x 异包孪生被全树分代替代，v26 && <26.2 才挂 2610）。
 * 分歧点①26.x render-dag 删 AfterEntities 子事件（neoforge-26.1 RenderLevelStageEvent
 * 子事件列实证）→ 取 AfterOpaqueFeatures（时点差异继续记功能债）。
 * 分歧点②26.2 删 Minecraft.renderBuffers()（MultiBufferSource 全删）→ RenderHandEvent
 * 携带的 SubmitNodeCollector 直传（neoforge-26.2 RenderHandEvent.java getSubmitNodeCollector
 * 实证），几何经 submitCustomGeometry 逃生口提交。
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
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), event.getSubmitNodeCollector(), event.getPackedLight(), event.getPartialTick());
    }
}
