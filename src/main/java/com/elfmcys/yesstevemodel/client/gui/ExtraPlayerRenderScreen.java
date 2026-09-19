package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
//? if >=21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
 *///?}
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
//? if >=1.16.2
import net.minecraft.util.FormattedCharSequence;
//? if >=1.21 {
/*import com.elfmcys.yesstevemodel.util.YsmFrame;*/
//?}


public class ExtraPlayerRenderScreen extends Screen {

    private static final char RESET_KEY = 'r';

    private int mouseStartX;

    private int mouseStartY;

    private float rotationX;

    private float rotationY;

    private boolean isDragging;

    private boolean isRightDragging;

    private int offsetX;

    private int offsetY;

    public ExtraPlayerRenderScreen() {
        super(YsmText.literal("YSM Extra Player Render Config GUI"));
        this.isDragging = false;
        this.isRightDragging = false;
        this.offsetX = 5;
        this.offsetY = 1;
        this.mouseStartX = ExtraPlayerRenderConfig.PLAYER_POS_X.get().intValue();
        this.mouseStartY = ExtraPlayerRenderConfig.PLAYER_POS_Y.get().intValue();
        this.rotationX = ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue();
        this.rotationY = ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue();
        if (PauseScreenButtonBuilder.isAndroid()) {
            this.offsetX = 16;
            this.offsetY = 0;
        }
    }

    public void init() {
// 1.16.5 无 Screen.clearWidgets（死注释形态的 init(mc,w,h)+return 从未生效，1.16.5 字段
// 由 setScreen→init(Minecraft,w,h) 先行赋值，无参 init() 直接可用，m2.6 实测语义）
//? if >=1.17 {
        clearWidgets();
//?}
        int i = -30;
        if (PauseScreenButtonBuilder.isAndroid()) {
            ysmAddWidget(YsmGui.button((this.width / 2) - 50, this.height - 35, 100, 30, YsmText.translatable("controls.reset"), button -> {
                resetTransform();
            }));
            i = -60;
        }
        MutableComponent mutableComponentTranslatable = YsmText.translatable("gui.yes_steve_model.hide_or_show");
        int iWidth = this.font.width(mutableComponentTranslatable) + 24;
        //? if neoforge && >=1.20.3 {
        /*ysmAddWidget(Checkbox.builder(mutableComponentTranslatable, this.font)
                .pos((this.width - iWidth) / 2, this.height + i)
                .selected(ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get().booleanValue())
                .onValueChange((checkbox, value) -> ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.set(value))
                .build());
         *///?}
        //? if forge || neoforge && <1.20.3 {
        ysmAddWidget(new Checkbox((this.width - iWidth) / 2, this.height + i, iWidth, 20, mutableComponentTranslatable, ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get().booleanValue(), true) {
            public void onPress() {
                super.onPress();
                ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.set(Boolean.valueOf(selected()));
            }
        });
        //?}
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
        int boxLeft = this.mouseStartX;
        int boxTop = this.mouseStartY;
        int boxRight = (int) (boxLeft + (this.rotationX));
        int boxBottom = (int) (boxTop + (this.rotationX * 2.0f));
        //? if <21.6
        guiGraphics.pose().pushPose();
        //? if >=21.6
        /*guiGraphics.pose().pushMatrix();*/
        //? if <21.6
        guiGraphics.pose().translate(0.0f, 0.0f, (-500.0f) - ((50.0f * this.rotationX) / 40.0f));
        //? if >=21.6
        /*guiGraphics.pose().translate(0.0f, 0.0f);*/
        guiGraphics.vLine((this.width / 2) - 1, -2, this.height + 2, -1610612737);
        guiGraphics.hLine(-2, this.width + 2, (this.height / 2) - 1, -1610612737);
        guiGraphics.vLine(10, -2, this.height + 2, -1610612737);
        guiGraphics.vLine(this.width - 10, -2, this.height + 2, -1610612737);
        guiGraphics.hLine(-2, this.width + 2, 10, -1610612737);
        guiGraphics.hLine(-2, this.width + 2, this.height - 10, -1610612737);
        guiGraphics.vLine(boxLeft, boxTop, boxBottom, -65536);
        guiGraphics.vLine(boxRight, boxTop, boxBottom, -65536);
        guiGraphics.hLine(boxLeft, boxRight, boxTop, -65536);
        guiGraphics.hLine(boxLeft, boxRight, boxBottom, -65536);
        guiGraphics.fillGradient(boxLeft, boxTop, boxRight, boxBottom, 1342177279, 1342177279);
        guiGraphics.fillGradient(boxLeft - this.offsetX, boxTop - this.offsetX, boxLeft + this.offsetX, boxTop + this.offsetX, -16711777, -16711777);
        guiGraphics.fillGradient(boxRight - this.offsetX, boxBottom - this.offsetX, boxRight + this.offsetX, boxBottom + this.offsetX, -16777057, -16777057);
        int tipY = 15;
        //? if <1.16.2 {
        /*for (net.minecraft.network.chat.FormattedText formattedCharSequence : this.font.split(YsmText.translatable("gui.yes_steve_model.extra_player_render.tips"), 500)) {
            guiGraphics.drawString(this.font, formattedCharSequence, (this.width - 15) - this.font.width(formattedCharSequence), tipY, 16777215);
            tipY += 10;
        }*/
        //?}
        //? if >=1.16.2 {
        for (FormattedCharSequence formattedCharSequence : this.font.split(YsmText.translatable("gui.yes_steve_model.extra_player_render.tips"), 500)) {
            guiGraphics.drawString(this.font, formattedCharSequence, (this.width - 15) - this.font.width(formattedCharSequence), tipY, 16777215);
            tipY += 10;
        }
        //?}
        //? if <21.6
        guiGraphics.pose().popPose();
        //? if >=21.6
        /*guiGraphics.pose().popMatrix();*/
        if (Minecraft.getInstance().player != null && !ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get().booleanValue()) {
            //? if <1.20
            /*ModelPreviewRenderer.renderPlayerOverlay(guiGraphics.pose(), Minecraft.getInstance().player, this.mouseStartX, this.mouseStartY, this.rotationX, this.rotationY, -500, this.minecraft.getFrameTime());*/
            //? if >=1.20 && <1.21
            ModelPreviewRenderer.renderPlayerOverlay(guiGraphics.graphics(), Minecraft.getInstance().player, this.mouseStartX, this.mouseStartY, this.rotationX, this.rotationY, -500, this.minecraft.getFrameTime());
            //? if >=1.21
            /*ModelPreviewRenderer.renderPlayerOverlay(guiGraphics.graphics(), Minecraft.getInstance().player, this.mouseStartX, this.mouseStartY, this.rotationX, this.rotationY, -500, YsmFrame.partialTick(this.minecraft));*/
        }
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

    // 1.21.9+ 输入事件对象化（GuiEventListener: mouseClicked(MouseButtonEvent,boolean)/
    // mouseReleased(MouseButtonEvent)/mouseDragged(MouseButtonEvent,double,double)/
    // charTyped(CharacterEvent)，neoforge-21.10.64 GuiEventListener.java:20-44 实证）
    //? if >=21.9 {
    /*public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        boolean inLeftHandleX = ((double) (this.mouseStartX - this.offsetX)) < mouseX && mouseX < ((double) (this.mouseStartX + this.offsetX));
        boolean inLeftHandleY = ((double) (this.mouseStartY - this.offsetX)) < mouseY && mouseY < ((double) (this.mouseStartY + this.offsetX));
        if (button == 0 && inLeftHandleX && inLeftHandleY) {
            this.isDragging = true;
        }
        int rightHandleX = (int) (this.mouseStartX + (this.rotationX));
        int rightHandleY = (int) (this.mouseStartY + (this.rotationX * 2.0f));
        boolean inRightHandleX = ((double) (rightHandleX - this.offsetX)) < mouseX && mouseX < ((double) (rightHandleX + this.offsetX));
        boolean inRightHandleY = ((double) (rightHandleX - this.offsetX)) < mouseY && mouseY < ((double) (rightHandleX + this.offsetX));
        if (button == 0 && inRightHandleX && inRightHandleY) {
            this.isRightDragging = true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        this.isRightDragging = false;
        return super.mouseReleased(event);
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.isRightDragging) {
            this.rotationX = (float) Math.min(mouseX - this.mouseStartX, (mouseY - this.mouseStartY) / 2.0d);
            return true;
        }
        if (this.isDragging) {
            this.mouseStartX = (int) mouseX;
            this.mouseStartY = (int) mouseY;
            return true;
        }
        if (button == this.offsetY) {
            this.rotationY += (float) (dragX * 2.0d);
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (Character.toLowerCase((char) event.codepoint()) == RESET_KEY && Minecraft.getInstance().hasAltDown()) {
            resetTransform();
        }
        return super.charTyped(event);
    }
    *///?}
    //? if <21.9 {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inLeftHandleX = ((double) (this.mouseStartX - this.offsetX)) < mouseX && mouseX < ((double) (this.mouseStartX + this.offsetX));
        boolean inLeftHandleY = ((double) (this.mouseStartY - this.offsetX)) < mouseY && mouseY < ((double) (this.mouseStartY + this.offsetX));
        if (button == 0 && inLeftHandleX && inLeftHandleY) {
            this.isDragging = true;
        }
        int rightHandleX = (int) (this.mouseStartX + (this.rotationX));
        int rightHandleY = (int) (this.mouseStartY + (this.rotationX * 2.0f));
        boolean inRightHandleX = ((double) (rightHandleX - this.offsetX)) < mouseX && mouseX < ((double) (rightHandleX + this.offsetX));
        boolean inRightHandleY = ((double) (rightHandleY - this.offsetX)) < mouseY && mouseY < ((double) (rightHandleY + this.offsetX));
        if (button == 0 && inRightHandleX && inRightHandleY) {
            this.isRightDragging = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        this.isRightDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isRightDragging) {
            this.rotationX = (float) Math.min(mouseX - this.mouseStartX, (mouseY - this.mouseStartY) / 2.0d);
            return true;
        }
        if (this.isDragging) {
            this.mouseStartX = (int) mouseX;
            this.mouseStartY = (int) mouseY;
            return true;
        }
        if (button == this.offsetY) {
            this.rotationY += (float) (dragX * 2.0d);
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (Character.toLowerCase(codePoint) == RESET_KEY && hasAltDown()) {
            resetTransform();
        }
        return super.charTyped(codePoint, modifiers);
    }
    //?}

    private void resetTransform() {
        this.mouseStartX = 10;
        this.mouseStartY = 10;
        this.rotationX = 40.0f;
        this.rotationY = 5.0f;
    }

    public void onClose() {
        ExtraPlayerRenderConfig.PLAYER_POS_X.set(Integer.valueOf(this.mouseStartX));
        ExtraPlayerRenderConfig.PLAYER_POS_Y.set(Integer.valueOf(this.mouseStartY));
        ExtraPlayerRenderConfig.PLAYER_SCALE.set(Double.valueOf(this.rotationX));
        ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.set(Double.valueOf(this.rotationY));
        super.onClose();
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
