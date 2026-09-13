package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代，1.20.4 版在 src/neoforge-1204/java）。断裂=@EventBusSubscriber 删除→fml.common.EventBusSubscriber（1.20.6 文档+oldtest 实证）。RenderArmEvent 同名同方法面且可取消
 * （20.4.251 javap：implements ICancellableEvent）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
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
