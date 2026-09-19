package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;

public class DisclaimerScreen extends Screen {

    private Checkbox checkbox;

    private int textY;

    private int textHeight;

    public DisclaimerScreen() {
        super(YsmText.literal("Disclaimer GUI"));
    }

    public void init() {
// 1.16.5 无 Screen.clearWidgets（死注释形态的 init(mc,w,h)+return 从未生效，1.16.5 字段
// 由 setScreen→init(Minecraft,w,h) 先行赋值，无参 init() 直接可用，m2.6 实测语义）
//? if >=1.17 {
        clearWidgets();
//?}
        int size = this.font.split(YsmText.translatable("gui.yes_steve_model.disclaimer.text"), 400).size();
        Objects.requireNonNull(this.font);
        int i = (size * 9) + 20 + 20 + 10 + 20;
        this.textY = (this.width - 400) / 2;
        this.textHeight = (this.height - i) / 2;
        MutableComponent mutableComponentTranslatable = YsmText.translatable("gui.yes_steve_model.disclaimer.read");
        int iWidth = this.font.width(mutableComponentTranslatable);
        //? if neoforge && >=1.20.3
        /*this.checkbox = Checkbox.builder(mutableComponentTranslatable, this.font)
                .pos((this.width - iWidth) / 2, (this.textHeight + i) - 50)
                .selected(!GeneralConfig.DISCLAIMER_SHOW.get().booleanValue())
                .build();*/
        //? if forge || neoforge && <1.20.3
        this.checkbox = new Checkbox((this.width - iWidth) / 2, (this.textHeight + i) - 50, iWidth, 20, mutableComponentTranslatable, !GeneralConfig.DISCLAIMER_SHOW.get().booleanValue());
        ysmAddWidget(this.checkbox);
        ysmAddWidget(YsmGui.button((this.width - 300) / 2, (this.textHeight + i) - 20, 300, 20, YsmText.translatable("gui.yes_steve_model.disclaimer.close"), button -> {
            if (this.checkbox.selected()) {
                GeneralConfig.DISCLAIMER_SHOW.set(false);
                Minecraft.getInstance().setScreen(new PlayerModelScreen());
            } else {
                Minecraft.getInstance().setScreen(null);
            }
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
        guiGraphics.drawWordWrap(this.font, YsmText.translatable("gui.yes_steve_model.disclaimer.text"), this.textY, this.textHeight, 400, -1);
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