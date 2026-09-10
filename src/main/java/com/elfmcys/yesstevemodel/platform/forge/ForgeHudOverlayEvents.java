package com.elfmcys.yesstevemodel.platform.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
//? if <1.17 {
/*import net.minecraftforge.client.event.RenderGameOverlayEvent;
 *///?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import rip.ysm.api.client.HudOverlay;

/**
 * 1.16.5 HUD 原生注册路径（gate-compat 遗留的 RegisterGuiOverlaysEvent 无宿主问题的收口）：
 * 1.20.1 挂 RegisterGuiOverlaysEvent.registerAbove(VanillaGuiOverlay.DEBUG_TEXT.id(),...)（mod bus，
 * 见 ForgeClientSetupHooks）↔ 1.16.5 挂 Forge 总线 RenderGameOverlayEvent.Post(ElementType.DEBUG)——
 * 事件在原版 DEBUG 层绘制后触发，语义等价"DEBUG_TEXT 层之上"（javap 实证 forge-1.16.5-36.2.39 mojmap jar：
 * RenderGameOverlayEvent.getMatrixStack()/getPartialTicks()/getWindow()，ElementType.DEBUG 存在）。
 * <p>1.20.1 轴：本类保留但无 @SubscribeEvent 处理器（方法整段注释态），Forge 总线扫描空类为 no-op，
 * 不会与 RegisterGuiOverlaysEvent 路径双画。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeHudOverlayEvents {

    private ForgeHudOverlayEvents() {
    }

    //? if <1.17 {
    /*@SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.DEBUG) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        HudOverlay debugOverlay = AnimationDebugOverlay.createOverlay();
        HudOverlay extraPlayerOverlay = new ExtraPlayerOverlay();
        HudOverlay syncOverlay = new ModelSyncStateOverlay();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        float partialTicks = event.getPartialTicks();
        debugOverlay.render(event.getMatrixStack(), minecraft.font, partialTicks, screenWidth, screenHeight);
        extraPlayerOverlay.render(event.getMatrixStack(), minecraft.font, partialTicks, screenWidth, screenHeight);
        syncOverlay.render(event.getMatrixStack(), minecraft.font, partialTicks, screenWidth, screenHeight);
    }
     *///?}
}
