package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
//? if <1.16.5 {
/*import net.minecraftforge.client.event.RenderHandEvent;*/
//?}
//? if >=1.16.5 && <1.17 {
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

    // RenderArmEvent forge 36 起才有（33/34/35 即 1.16.2/3/4 实证无——unimined merged jar
    // unzip 逐线核对：三线均只有 RenderHandEvent.class，1.16.5 jar 两类并存）→ <1.16.5 全段
    // RenderHandEvent 承接：第一人称手部渲染恒为本地玩家（vanilla GameRenderer.renderHand），
    // 事件 @Cancelable 实证（forge 32~35 RenderHandEvent javap 同签名 getHand/
    // getMatrixStack/getBuffers/getLight）；手→臂映射 MAIN_HAND=RIGHT/OFF_HAND=LEFT。
    // 功能差入 tasks.feature-debts（RenderArmEvent 先例格式）。
    //? if <1.16.5 {
    /*
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        net.minecraft.world.entity.HumanoidArm arm = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.entity.HumanoidArm.RIGHT : net.minecraft.world.entity.HumanoidArm.LEFT;
        if (ReplacePlayerHandRenderEvent.onRenderArm(player, arm, event.getMatrixStack(), event.getBuffers(), event.getLight())) {
            event.setCanceled(true);
        }
    }
     *///?}
    //? if >=1.16.5 && <1.17 {
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
