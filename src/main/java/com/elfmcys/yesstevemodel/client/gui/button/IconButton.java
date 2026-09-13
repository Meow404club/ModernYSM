package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends FlatColorButton {

    //? if >=1.21
    /*private static final ResourceLocation ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/icon.png");*/
    //? if <1.21
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(YesSteveModel.MOD_ID, "texture/icon.png");

    private final int iconU;

    private final int iconV;

    public IconButton(int x, int y, int width, int height, int iconU, int iconV, OnPress onPress) {
        super(x, y, width, height, YsmText.literal(""), onPress);
        this.iconU = iconU;
        this.iconV = iconV;
    }

    //? if >=1.20 && <21.11 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 1.21.11 AbstractButton.renderWidget final 化 → renderContents
    //? if >=21.11 {
    @Override
    public void renderContents(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    @Override
    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(ICON_TEXTURE, getX() + ((this.width - 16) / 2), getY() + ((this.height - 16) / 2), 16, 16, this.iconU, this.iconV, 16, 16, 256, 256);
    }
}