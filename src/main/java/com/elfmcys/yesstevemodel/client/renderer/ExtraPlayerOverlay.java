package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.client.HudOverlay;
//? if >=1.21 {
/*import com.elfmcys.yesstevemodel.util.YsmFrame;*/
//?}

public class ExtraPlayerOverlay implements HudOverlay {
    // <1.20 轴：主体与 >=1.20 分支同构，首参双轴 PoseStack↔GuiGraphics；
    // 实现委托 ModelPreviewRenderer.renderPlayerOverlay（<1.17 pushMatrix 变体 /
    // >=1.17 getModelViewStack 变体，per-axis 分代见该文件）
    //? if <1.20 {
    /*@Override
    public void render(PoseStack poseStack, Font font, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get() || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null || (minecraft.screen instanceof ExtraPlayerRenderScreen)) {
            return;
        }
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER_THIRD_PERSON.get() && minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        ModelPreviewRenderer.renderPlayerOverlay(poseStack, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, minecraft.getFrameTime());
    }
     *///?}
     //? if >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, Font font, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get() || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null || (minecraft.screen instanceof ExtraPlayerRenderScreen)) {
            return;
        }
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER_THIRD_PERSON.get() && minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        //? if >=1.21
        /*ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, YsmFrame.partialTick(minecraft));*/
        //? if <1.21
        ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, minecraft.getFrameTime());
    }
     //?}
}