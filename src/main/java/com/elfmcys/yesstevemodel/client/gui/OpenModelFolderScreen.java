package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.YsmText;
// 1.21.11 Util 移 net.minecraft.util 子包
//? if >=21.11
/*import net.minecraft.util.Util;*/
//? if <21.11
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class OpenModelFolderScreen extends Screen {

    private final PlayerModelScreen parentScreen;

    public OpenModelFolderScreen(PlayerModelScreen modelScreen) {
        super(YsmText.literal("Open Model Folder"));
        this.parentScreen = modelScreen;
    }

    public void init() {
        int x = (this.width - 310) / 2;
        int y = (this.height / 2) + 60;
// 1.16.5 无 Screen.clearWidgets（死注释形态的 init(mc,w,h)+return 从未生效，1.16.5 字段
// 由 setScreen→init(Minecraft,w,h) 先行赋值，无参 init() 直接可用，m2.6 实测语义）
//? if >=1.17 {
        clearWidgets();
//?}
        ysmAddWidget(YsmGui.button(x, y, 150, 20, YsmText.translatable("gui.yes_steve_model.open_model_folder.open"), button -> {
            // 26.3 Util.OS 撤桌面打开面 → Blaze3D.openPath(Path)（Blaze3D.java:39）
            //? if <26.3
            Util.getPlatform().openFile(ServerModelManager.CUSTOM.toFile());
            //? if >=26.3
            /*com.mojang.blaze3d.Blaze3D.openPath(ServerModelManager.CUSTOM);*/
        }));
        ysmAddWidget(YsmGui.button(x + 160, y, 150, 20, YsmText.translatable("gui.yes_steve_model.model.return"), button2 -> {
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
        guiGraphics.drawWordWrap(this.font, YsmText.translatable("gui.yes_steve_model.open_model_folder.tips"), (this.width - 400) / 2, (this.height / 2) - 80, 400, 16777215);
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
