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

    // 1.16.5~1.19.2 AbstractSliderButton 无 getX/getY（x/y 为 public 字段，1192 AbstractWidget.java:25-26），
    // 补桥接；1.19.3 起 x/y 私有化且父类自带同名方法（1.19.3 merged jar 实证），桥接注释态
    //? if <1.19.3 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
     *///?}

    // 渲染钩子三段：renderWidget(GuiGraphics) 1.20 起 / renderWidget(PoseStack) 1.19.4
    //（1194 AbstractSliderButton.java:65）/ renderButton(PoseStack) 1.16.5~1.19.2
    //? if >=1.20 && <26 {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 26.x：AbstractSliderButton 自带 extractWidgetRenderState（26.1:70 concrete）→ 本覆写
    // 全量替换 vanilla 滑条绘制（与 21.x 覆写 renderWidget 同语义）
    //? if >=26 {
    /*@Override
    public void extractWidgetRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }*/
    //?}
    //? if >=1.19.4 && <1.20 {
    /*
    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    //? if <1.19.4 {
    /*
    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);
}
