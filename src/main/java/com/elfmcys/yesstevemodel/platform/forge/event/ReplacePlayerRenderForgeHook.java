package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerRenderForgeHook {

    private ReplacePlayerRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        // RenderPlayerEvent 两代参数名：1.16.x getPartialRenderTick/getMatrixStack/getBuffers/
        // getLight + getPlayer；1.17+ getPartialTick/getPoseStack/getMultiBufferSource/getPackedLight
        // + getEntity（forge-1.16.x / forge-1.20.1 源码实证）
        //? if <1.17 {
        // if (ReplacePlayerRenderEvent.onRenderPlayerPre(event.getPlayer(), event.getPartialRenderTick(), event.getMatrixStack(), event.getBuffers(), event.getLight())) {
        //     event.setCanceled(true);
        // }
        //? } else {
        if (ReplacePlayerRenderEvent.onRenderPlayerPre(event.getEntity(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
        }
        //? }
    }
}
