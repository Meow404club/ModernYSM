package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.components.AbstractWidget;
//? if >1.17 {
import net.minecraft.client.gui.narration.NarrationElementOutput;
//?}
import net.minecraft.network.chat.Component;


@OnlyIn(Dist.CLIENT)
public class FlatIconButton extends AbstractWidget implements ISpecialWidget {

    private final int iconIndex;

    public FlatIconButton(int x, int y, int iconIndex, Component component) {
        super(x, y, 115, 15, component);
        this.iconIndex = iconIndex;
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

    //? if <1.17 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
     *///?}

    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + this.iconIndex, -280804798);
        //? if <1.17
        /*guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 2, this.getY() + (this.height - 8) / 2, 16777215, false);*/
        //? if >=1.17
        renderScrollingString(guiGraphics.graphics(), Minecraft.getInstance().font, 2, 16777215);
    }

    //? if >1.17 {
    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
    //?}
}