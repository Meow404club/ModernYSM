package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.config.LoadingStateConfig;
import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.network.chat.Component;

public class LoadingStateButton extends YsmButton {
    public LoadingStateButton(int x, int y) {
        //? if <1.17 {
        /*super(x, y, 100, 20, YsmText.literal(""), button -> {*/
        //?} else {
        super(x, y, 100, 20, Component.empty(), button -> {
        //?}
        });
    }

    //? if >1.17 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    @Override
    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderVanilla(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(Minecraft.getInstance().font, YsmText.translatable("gui.yes_steve_model.config.loading_state_position"), getX() + 105, getY() + 6, 16777215, false);
    }

    public Component getMessage() {
        return YsmText.literal(LoadingStateConfig.LOADING_STATE_POSITION.get().name());
    }

    public void onPress() {
        LoadingStateConfig.Position stateConfig;
        switch (LoadingStateConfig.LOADING_STATE_POSITION.get()) {
            case TOP_LEFT:
                stateConfig = LoadingStateConfig.Position.TOP_CENTER;
                break;
            case TOP_CENTER:
                stateConfig = LoadingStateConfig.Position.TOP_RIGHT;
                break;
            case TOP_RIGHT:
                stateConfig = LoadingStateConfig.Position.BOTTOM_RIGHT;
                break;
            case BOTTOM_RIGHT:
                stateConfig = LoadingStateConfig.Position.BOTTOM_CENTER;
                break;
            case BOTTOM_CENTER:
                stateConfig = LoadingStateConfig.Position.BOTTOM_LEFT;
                break;
            case BOTTOM_LEFT:
                stateConfig = LoadingStateConfig.Position.TOP_LEFT;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }
        LoadingStateConfig.LOADING_STATE_POSITION.set(stateConfig);
    }
}