package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。RenderArmEvent 同名同方法面且可取消
 * （20.4.251 javap：implements ICancellableEvent）。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerHandRenderForgeHook {
    private ReplacePlayerHandRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (ReplacePlayerHandRenderEvent.onRenderArm(event.getPlayer(), event.getArm(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
