package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。RenderPlayerEvent.Pre 可取消
 * （20.4.251 javap：implements ICancellableEvent，与 forge 1.20.1 同语义）。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
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
