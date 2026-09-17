package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.elfmcys.yesstevemodel.platform.neoforge.event.PlayerRenderStateEntityCache;

/**
 * 26.2 代孪生（2111 树的 ReplacePlayerRenderForgeHook 26.2 分歧版；全树分代挂载，同 FQCN）。

 * 1.21.2 render-state 化：RenderPlayerEvent（extends RenderLivingEvent）头参 entity→renderState
 *（neoforge-1.21.3 RenderPlayerEvent.java:27-31 实证，无 getEntity）→ 玩家实体经
 * PlayerRenderStateEntityCache（RegisterRenderStateModifiersEvent 注册的 extractor，
 * PlayerRenderer.extractRenderState 时 stash）按 state 反查。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerRenderForgeHook {
    private ReplacePlayerRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = PlayerRenderStateEntityCache.get(event.getRenderState());
        if (player == null) {
            return;
        }
        // 26.2 render-dag 换代删 Minecraft.renderBuffers()（MultiBufferSource 全删）→
        // collector 直取事件携带值（neoforge-26.2 RenderPlayerEvent.java:30 构造实证）。
        // Pre 事件头参去 packedLight（2110 RenderLivingEvent.java:98 同构）→
        // 全亮常量替代（事件仅作取消闸，不参与实际置光）
        if (ReplacePlayerRenderEvent.onRenderPlayerPre(player, event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector(), 15728880 /* LightTexture.FULL_BRIGHT（26.x 类删，常量字面量版本中立；26.1 LightCoordsUtil.FULL_BRIGHT 同值） */)) {
            event.setCanceled(true);
        }
    }
}
