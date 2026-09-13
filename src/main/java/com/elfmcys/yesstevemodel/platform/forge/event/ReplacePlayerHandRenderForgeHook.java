package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
//? if <1.17 {
/*import net.minecraftforge.client.event.RenderArmEvent;*/
//?}
//? if >=1.18.2 {
import net.minecraftforge.client.event.RenderArmEvent;
//?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// RenderArmEvent 1.17.1 forge 缺失（1.16.5/1.18.2+ 有）→ 1.17.1 无手臂替换钩子
//（功能差记 tasks.feature-debts）
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerHandRenderForgeHook {

    private ReplacePlayerHandRenderForgeHook() {
    }

    //? if <1.17 {
    /*
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm(event.getPlayer(), event.getArm(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
     *///?}
    //? if >=1.18.2 {
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm(event.getPlayer(), event.getArm(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
    //?}
}
