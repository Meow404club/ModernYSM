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
 * 见 ForgeClientSetupHooks）↔ 1.16.5 挂 Forge 总线 RenderGameOverlayEvent.Post(ElementType.ALL)——
 * 选层依据（forge-1.16.x ForgeIngameGui 实证）：DEBUG 层被 options.renderDebug（F3）门控
 *（renderHUDText:611）HUD 常态不触发；post(ALL, mStack) 是 ForgeIngameGui.render 末尾
 *（gui.render 收尾、Screen 子通道之前）的无条件每帧触发点——实体绘制所需 GL 状态在此
 * 已收敛（纸娃娃为实体 RenderType 绘制，依赖该时点状态），故取 ALL 而非 DEBUG/TEXT。
 * post(ALL) 发的是普通 Post 实例，Post 订阅照常接收。
 * （javap 实证 forge-1.16.5-36.2.39 mojmap jar：getMatrixStack()/getPartialTicks()/getWindow()。）
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
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
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
