package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.Minecraft;
import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class FlatColorButton extends YsmButton {

    private boolean selected;

    private List<Component> tooltip;

    public FlatColorButton(int x, int y, int width, int height, Component component, OnPress onPress) {
        super(x, y, width, height, component, onPress);
        this.selected = false;
    }

    public FlatColorButton setTooltipText(String str) {
        this.tooltip = Collections.singletonList(YsmText.translatable(str));
        return this;
    }

    public FlatColorButton setTooltipLines(List<Component> list) {
        this.tooltip = list;
        return this;
    }

    public void renderTooltip(YsmGui guiGraphics, Screen screen, int mouseX, int mouseY) {
        if (this.isHovered && this.tooltip != null) {
            guiGraphics.renderScreenComponentTooltip(screen, Minecraft.getInstance().font, this.tooltip, mouseX, mouseY);
        }
    }

    //? if >=1.20 {
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
        Font font = Minecraft.getInstance().font;
        if (this.selected) {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -14774017, -14774017);
        } else {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -12369342, -12369342);
        }
        if (hoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY() + 1, getX() + 1, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + 1, -790560, -790560);
            guiGraphics.fillGradient((getX() + this.width) - 1, getY() + 1, getX() + this.width, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), (getY() + this.height) - 1, getX() + this.width, getY() + this.height, -790560, -790560);
        }
        //? if <1.17
        /*guiGraphics.drawString(font, getMessage(), getX() + 2, getY() + (this.height - 8) / 2, 15986656, false);*/
        //? if >=1.17
        renderScrollingString(guiGraphics.graphics(), font, 2, 15986656);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}