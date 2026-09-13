package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.20.5+ 断裂：@Mod.EventBusSubscriber 删除（1.20.4 版还误 import 了 TickEvent——1.20.5+ 已无此类，
 * 一并移除）；RenderLevelStageEvent/RenderHandEvent 1.20.6 同名同方法面保留。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
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
