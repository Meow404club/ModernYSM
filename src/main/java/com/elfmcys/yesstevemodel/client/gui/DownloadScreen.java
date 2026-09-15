package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class DownloadScreen extends Screen {

    private final PlayerModelScreen parentScreen;

    private int guiLeft;

    private int guiTop;

    public DownloadScreen(PlayerModelScreen modelScreen) {
        super(YsmText.literal("YSM Config GUI"));
        this.parentScreen = modelScreen;
    }

    public void init() {
        this.guiLeft = (this.width - 420) / 2;
        this.guiTop = (this.height - 235) / 2;
        ysmAddWidget(new FlatColorButton(this.guiLeft + 5, this.guiTop, 80, 18, YsmText.translatable("gui.yes_steve_model.model.return"), button -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
        }));
    }

    //? if >=1.20 && <26 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    //? if <1.20 {
    /*@Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    // 26.x GUI 换代：Screen.render → extractRenderState（vanilla-26.1 Screen.java:116；
    // GuiGraphics 同代改名 GuiGraphicsExtractor，由 rlToIdentifier 生成树规则统一改写）
    //? if >=26 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
     *///?}

    public void render(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.renderScreenBackground(this);
        guiGraphics.drawCenteredString(this.font, "Coming Soooooooooooooooooooooooooon™", this.width / 2, (this.height / 2) - 5, ChatFormatting.DARK_RED.getColor().intValue());
        //? if <1.17
        /*super.render(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.17 && <1.20
        /*super.render(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.20 && <26
        super.render(guiGraphics.graphics(), mouseX, mouseY, partialTick);
        // 26.x：render → extractRenderState（vanilla-26.1 Screen/AbstractWidget 换代）
        //? if >=26
        /*super.extractRenderState(guiGraphics.graphics(), mouseX, mouseY, partialTick);*/
    }
    // addRenderableWidget/addWidget 均为 protected 实例方法（JLS 6.6.2 子类内才可调）→ 桥方法；
    // 泛型返回保持原 addRenderableWidget 的链式取回语义（如 .setTooltipText 续链）
    //? if <1.17 {
    /*private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractButton) {
            this.addButton((net.minecraft.client.gui.components.AbstractButton) widget);
        } else {
            this.addWidget(widget);
            ((com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor) this).ysm$getRenderables().add(widget);
        }
        return widget;
    }
     *///?} else {
    private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        this.addRenderableWidget(widget);
        return widget;
    }
    //?}

}
