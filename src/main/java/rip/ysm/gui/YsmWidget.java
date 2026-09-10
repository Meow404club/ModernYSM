package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * 自绘控件基类：统一两版 AbstractWidget 的渲染钩子与坐标存取差异。
 * <ul>
 *   <li>渲染钩子：1.20.1 renderWidget(GuiGraphics,...) ↔ 1.16.5 renderButton(PoseStack,...)
 *       （1.16.5 AbstractWidget.java:60/98），子类只实现版本中性的
 *       {@link #renderWidget(YsmGui, int, int, float)}</li>
 *   <li>坐标存取：1.20.1 有 getX/getY/setX/setY（x/y 私有）；1.16.5 x/y 是 public 字段
 *       （1.16.5 AbstractWidget.java:25-26），无存取器 → 1.16.5 轴补桥接方法</li>
 *   <li>narration（updateWidgetNarration/defaultButtonNarrationText）为 1.17+ API，1.16.5 轴剔除</li>
 * </ul>
 */
public abstract class YsmWidget extends AbstractWidget {
    protected YsmWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    // 1.16.5 AbstractWidget 无 getX/getY/setX/setY（x/y 为 public 字段），补桥接；
    // 1.20.1 侧父类自带同名方法，桥接注释态（不可加 @Override：1.16.5 父类无此签名）
    //? if <1.17 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

     *///?}

    //? if <1.17 {
    /*@Override
    protected void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(pose), mouseX, mouseY, partialTick);
    }
     *///?} else {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);

    //? if >1.17 {
    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
    //?}
}
