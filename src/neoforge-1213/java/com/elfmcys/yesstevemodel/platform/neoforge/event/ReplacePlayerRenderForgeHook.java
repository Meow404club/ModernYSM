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
 * neoforge 孪生（同 FQCN；RAW 源集，1.21.2+ 代，<1.21.2 版在 src/neoforge-1205/java）。
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
        if (ReplacePlayerRenderEvent.onRenderPlayerPre(player, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
