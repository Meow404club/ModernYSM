package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代，1.20.4 版在 src/neoforge-1204/java）。
 * <1.21.2 代独占挂载（1.21.2+ 见 src/neoforge-1213 树；1205 树不再承载本类）。
断裂=@EventBusSubscriber 删除→fml.common.EventBusSubscriber（1.20.6 文档+oldtest 实证）。RenderPlayerEvent.Pre 可取消
 * （20.4.251 javap：implements ICancellableEvent，与 forge 1.20.1 同语义）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerRenderForgeHook {
    private ReplacePlayerRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (ReplacePlayerRenderEvent.onRenderPlayerPre(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
