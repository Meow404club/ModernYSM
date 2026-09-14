package com.elfmcys.yesstevemodel.platform.neoforge.event212;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.elfmcys.yesstevemodel.platform.neoforge.event212.PlayerRenderStateEntityCache;

/**
 * neoforge 孪生（异包 event212；RAW 源集，21.2 混合形态独占，批二 c-2）。
 * 逻辑同 src/neoforge-1213 版（1.21.2 render-state 化：RenderPlayerEvent 头参
 * entity→renderState，无 getEntity）→ 玩家实体经 event212 反查缓存
 *（PlayerRenderStateStashMixin stash）按 state 查回；异包原因见该缓存类注释
 *（1213 版同名类走 neoforge renderstate 注册，21.2 无此机制且同名文件无法按线剔除）。
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
