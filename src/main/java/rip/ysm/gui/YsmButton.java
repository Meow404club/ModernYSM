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

    protected boolean hoveredOrFocused() {
        //? if >1.17 {
        return this.isHoveredOrFocused();
        //?} else {
        /*return this.isHovered();
         *///?}
    }
}
