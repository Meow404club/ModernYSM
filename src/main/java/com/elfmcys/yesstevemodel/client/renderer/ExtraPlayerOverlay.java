package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >1.17 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.client.HudOverlay;

public class ExtraPlayerOverlay implements HudOverlay {
    // 1.16.5 降级：不渲染（实现委托 ModelPreviewRenderer.renderPlayerOverlay，其 modelViewStack/
    // renderType 管线为 1.17+ API，对位归渲染管线卡；此处仅保证 HUD 契约双轴可编译，记录功能差）
    //? if <1.17 {
    /*@Override
    public void render(PoseStack poseStack, Font font, float partialTick, int screenWidth, int screenHeight) {
    }
     *///?} else {
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
        ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, minecraft.getFrameTime());
    }
    //?}
}