package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * 26.2 代孪生（2111 树的 ReplacePlayerHandRenderForgeHook 26.2 分歧版；全树分代挂载，
 * 同 FQCN 无需异包——2110/2111 树先例）。
 * 分歧点：26.2 render-dag 换代删 Minecraft.renderBuffers()（MultiBufferSource 全删）→
 * collector 直取事件携带值（neoforge-26.2 RenderArmEvent.java:70 getSubmitNodeCollector 实证）。
 * 其余断裂：getPackedLight→getLightCoords（:79）、getPlayer→getAvatar（:99，Player extends
 * Avatar，vanilla-26.2 Player.java:128 实证）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerHandRenderForgeHook {
    private ReplacePlayerHandRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm((net.minecraft.world.entity.player.Player) event.getAvatar(), event.getArm(), event.getPoseStack(), event.getSubmitNodeCollector(), event.getLightCoords())) {
            event.setCanceled(true);
        }
    }
}
