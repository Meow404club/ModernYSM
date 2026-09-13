package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 自绘按钮基类（对位 YsmWidget 的 Button 轴）：统一各版 Button 的渲染钩子/narration 差异。
 * <ul>
 *   <li>构造器：1.19.4+ Button(x,y,w,h,msg,onPress,DEFAULT_NARRATION)（1194 Button.java:21，
 *       protected + CreateNarration；DEFAULT_NARRATION 1194:13 protected static）↔
 *       1.16.5~1.19.2 无 narration 参数的 public 6 参构造（1165:12/1182:15/1192:18）</li>
 *   <li>渲染钩子：1.20.1 renderWidget(GuiGraphics,...)（AbstractButton）↔ 1.19.4
 *       renderWidget(PoseStack,...)（1194 AbstractButton.java:25 public）↔
 *       1.16.5~1.19.2 renderButton(PoseStack,...)（1165 Button javap public；
 *       1182/1192 AbstractButton renderButton public），
 *       子类只实现版本中性的 {@link #renderWidget(YsmGui, int, int, float)}</li>
 *   <li>hover 判定：1.17+ isHoveredOrFocused（1182:154/1192:153）↔ 1.16.5 isHovered（1165:177）</li>
 * </ul>
 */
public abstract class YsmButton extends Button {
    protected YsmButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        //? if >=1.19.4 {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        //?} else {
        /*super(x, y, width, height, message, onPress);
         *///?}
    }

    // 1.16.5~1.19.2 AbstractWidget 无 getX/getY/setX/setY（x/y 为 public 字段），补桥接；
    // 1.19.4+ 侧父类自带同名方法，桥接注释态（不可加 @Override：父类无此签名）
    //? if <1.19.4 {
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

    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(pose), mouseX, mouseY, partialTick);
    }
     *///?}
    // 1.19.4 专属：renderWidget 名 1.19.4 引入但参数仍 PoseStack（1194 AbstractButton.java:25）
    //? if >=1.19.4 && <1.20 {
    /*
    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    //? if >=1.20 && <21.11 {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 1.21.11 AbstractButton.renderWidget final 化，可覆写钩子改名 renderContents
    //（2111 AbstractWidget.java:62/89 renderWidget+renderContents 分工实证）
    //? if >=21.11 {
    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);

    /** vanilla Button 原始绘制（纹理底/边框）：子类需要叠绘时经此调用。 */
    protected void renderVanilla(YsmGui g, int mouseX, int mouseY, float partialTick) {
        //? if <1.19.4 {
        /*super.renderButton(g.pose(), mouseX, mouseY, partialTick);
         *///?}
        //? if >=1.19.4 && <1.20 {
        /*
        super.renderWidget(g.pose(), mouseX, mouseY, partialTick);
         *///?}
        //? if >=1.20 {
        super.renderWidget(g.graphics(), mouseX, mouseY, partialTick);
        //?}
    }

    protected boolean hoveredOrFocused() {
        //? if >=1.18.2 {
        return this.isHoveredOrFocused();
        //?} else {
        /*return this.isHovered();
         *///?}
    }
}
