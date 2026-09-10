package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.minecraftforge.api.distmarker.Dist;
//? if <1.17 {
// import net.minecraftforge.event.TickEvent;
//? } else {
import net.minecraftforge.client.event.RenderLevelStageEvent;
//? }
// RenderLevelStageEvent 为 1.17+（1.16.5 无分阶段事件）；帧级 reset 语义由
// RenderTickEvent START 承接（每渲染帧开头复位 once-per-frame 标记）
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class RenderFirstPlayerForgeHook {

    private RenderFirstPlayerForgeHook() {
    }

    //? if <1.17 {
    // @SubscribeEvent
    // public static void onRenderTick(TickEvent.RenderTickEvent event) {
    //     if (event.phase == TickEvent.Phase.START) {
    //         RenderFirstPlayerBackground.resetFrame();
    //     }
    // }
    //? } else {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            RenderFirstPlayerBackground.resetFrame();
        }
    }
    //? }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // 1.16.x getter：getMatrixStack/getBuffers/getLight/getPartialTicks
        //? if <1.17 {
        // RenderFirstPlayerBackground.onRenderHand(event.getMatrixStack(), event.getBuffers(), event.getLight(), event.getPartialTicks());
        //? } else {
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
        //? }
    }
}
