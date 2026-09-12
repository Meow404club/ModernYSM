package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * 自绘滑条基类（对位 YsmButton 的 AbstractSliderButton 轴）：
 * 渲染钩子 1.20.1 renderWidget(GuiGraphics,...) ↔ 1.16.5 renderButton(PoseStack,...)
 * （1.16.5 AbstractSliderButton javap：public void renderButton(PoseStack,int,int,float)），
 * 构造器两版同形 (x,y,w,h,msg,value)，子类只实现版本中性的 renderWidget(YsmGui,...)。
 */
public abstract class YsmSliderButton extends AbstractSliderButton {
    protected YsmSliderButton(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    //? if <1.17 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
     *///?}

    //? if >=1.20 {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);
}
