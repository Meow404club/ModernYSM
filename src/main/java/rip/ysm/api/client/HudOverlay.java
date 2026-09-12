package rip.ysm.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}

/**
 * HUD overlay 契约：首参双轴——1.20.1 GuiGraphics ↔ 1.16.5 PoseStack（GuiGraphics 为 1.19.4+ API）。
 * 注册挂点：1.20.1 RegisterGuiOverlaysEvent（mod bus）↔ 1.16.5 RenderGameOverlayEvent.Post（forge bus，
 * 见 platform/forge/ForgeHudOverlayEvents）。
 */
@FunctionalInterface
public interface HudOverlay {
    // PoseStack 首参覆盖 1.16.5~1.19.4（GuiGraphics 1.20 起）
    //? if <1.20 {
    /*void render(PoseStack poseStack, Font font, float partialTick, int screenWidth, int screenHeight);
     *///?}
    //? if >=1.20 {
    void render(GuiGraphics guiGraphics, Font font, float partialTick, int screenWidth, int screenHeight);
    //?}
}
