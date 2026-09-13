package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.TickEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。forge 原类 1.20 展开态活跃面 =
 * RenderLevelStageEvent(AFTER_CUTOUT_BLOCKS 帧复位) + RenderHandEvent（同名同方法面，
 * 20.4.251 javap 实证）；forge &lt;1.20 的 RenderTickEvent 分支不进孪生。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class RenderFirstPlayerForgeHook {
    private RenderFirstPlayerForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            RenderFirstPlayerBackground.resetFrame();
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
    }
}
