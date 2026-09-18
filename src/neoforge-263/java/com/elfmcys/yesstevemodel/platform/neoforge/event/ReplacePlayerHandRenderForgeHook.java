package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * 26.3 代孪生（262 树分代 fork，m3-263-increment；全树分代挂载，同 FQCN——2110/2111 先例）。
 * 分歧点：26.3 RenderArmEvent 删 getAvatar（nf-26.3 RenderArmEvent.java 仅余
 * getArm/getPoseStack/getSubmitNodeCollector/getLightCoords/getSkinTexture/hasSleeve/
 * getArmPart/getPlayerRenderState）→ 该事件仅 FirstPersonHandsAndItemsRenderer.renderPlayerHand
 *（vanilla-26.3 :139/:267）第一人称自臂触发，实体恒为本地玩家 → Minecraft.player 取替。
 */

@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerHandRenderForgeHook {
    private ReplacePlayerHandRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm(net.minecraft.client.Minecraft.getInstance().player, event.getArm(), event.getPoseStack(), event.getSubmitNodeCollector(), event.getLightCoords())) {
            event.setCanceled(true);
        }
    }
}
