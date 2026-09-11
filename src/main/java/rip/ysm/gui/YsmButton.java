package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 自绘按钮基类（对位 YsmWidget 的 Button 轴）：统一两版 Button 的渲染钩子/narration 差异。
 * <ul>
 *   <li>构造器：1.20.1 Button(x,y,w,h,msg,onPress,DEFAULT_NARRATION) ↔ 1.16.5 无 narration 参数</li>
 *   <li>渲染钩子：1.20.1 renderWidget(GuiGraphics,...)（AbstractButton）↔ 1.16.5 renderButton(PoseStack,...)
 *       （1.16.5 Button javap：public void renderButton(PoseStack,int,int,float)），子类只实现
 *       版本中性的 {@link #renderWidget(YsmGui, int, int, float)}</li>
 *   <li>hover 判定：1.20.1 isHoveredOrFocused ↔ 1.16.5 isHovered</li>
 * </ul>
 */
public abstract class YsmButton extends Button {
    protected YsmButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        //? if >1.17 {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        //?} else {
        /*super(x, y, width, height, message, onPress);
         *///?}
    }

    // 1.16.5 AbstractWidget 无 getX/getY/setX/setY（x/y 为 public 字段），补桥接
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

    //? if >1.17 {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(pose), mouseX, mouseY, partialTick);
    }
     *///?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);

    /** vanilla Button 原始绘制（纹理底/边框）：子类需要叠绘时经此调用。 */
    protected void renderVanilla(YsmGui g, int mouseX, int mouseY, float partialTick) {
        //? if <1.17 {
        /*super.renderButton(g.pose(), mouseX, mouseY, partialTick);
         *///?} else {
        super.renderWidget(g.graphics(), mouseX, mouseY, partialTick);
        //?}
    }

    protected boolean hoveredOrFocused() {
        //? if >1.17 {
        return this.isHoveredOrFocused();
        //?} else {
        /*return this.isHovered();
         *///?}
    }
}
