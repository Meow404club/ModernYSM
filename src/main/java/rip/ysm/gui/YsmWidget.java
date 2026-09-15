package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if >=21.9 {
/*import net.minecraft.client.input.MouseButtonEvent;
 *///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * 自绘控件基类：统一各版 AbstractWidget 的渲染钩子与坐标存取差异。
 * <ul>
 *   <li>渲染钩子：1.20.1 renderWidget(GuiGraphics,...)（1201 AbstractWidget.java:121）↔
 *       1.19.4 renderWidget(PoseStack,...)（1194:117，public abstract）↔
 *       1.16.5~1.19.2 renderButton(PoseStack,...)（1165:98/1182:74/1192:73 均 public），
 *       子类只实现版本中性的 {@link #renderWidget(YsmGui, int, int, float)}</li>
 *   <li>坐标存取：1.19.4+ 有 getX/getY/setX/setY（1194:315/320/330）；1.16.5~1.19.2 x/y 是
 *       public 字段（1165:25-26/1182:26-27/1192:25-26）→ &lt;1.19.4 轴补桥接方法</li>
 *   <li>narration（updateWidgetNarration/defaultButtonNarrationText）为 1.19.3+ API
 *       （1194:301/303），1.17~1.19.2 走父类 updateNarration→createNarrationMessage
 *       （1182:232），1.16.5 无 narration 机制 → 仅 >=1.19.4 轴覆写</li>
 * </ul>
 */
public abstract class YsmWidget extends AbstractWidget {
    protected YsmWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    // 1.21.9+ AbstractWidget 事件对象化（onClick(MouseButtonEvent,boolean)/
    // onDrag(MouseButtonEvent,double,double)/onRelease(MouseButtonEvent)，
    // neoforge-21.10.64 AbstractWidget.java:119-137 实证）→ 本仓双参钩子收编为本类持有，
    // vanilla 新签名转发（子类 @Override 双参钩子全线语义不变）
    //? if >=21.9 {
    /*public void onClick(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    }

    public void onRelease(double mouseX, double mouseY) {
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onClick(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        this.onDrag(event.x(), event.y(), dragX, dragY);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.onRelease(event.x(), event.y());
    }
    *///?}

    // 1.16.5~1.19.2 AbstractWidget 无 getX/getY/setX/setY（x/y 为 public 字段），补桥接；
    // 1.19.3 起 x/y 私有化且父类自带 getX/getY（1.19.3 merged jar 实证）→ 桥接边界回 1.19.3，
    // 桥接注释态（<1.19.3 父类无此签名，不可加 @Override）
    //? if <1.19.3 {
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
    // 1.19.4 专属：renderWidget 名 1.19.4 引入但参数仍 PoseStack（1194 AbstractWidget.java:117）
    //? if >=1.19.4 && <1.20 {
    /*
    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    // forge 46（1.20）renderWidget 为 public abstract（1.20-forge merged jar javap 实证），
    // forge 47（1.20.1）起回落 protected abstract（1201 merged jar javap 实证）→ 1.20 专属段
    // 须 public 覆写（protected 降权编译错），>=1.20.1 保持 protected
    //? if >=1.20 && <1.20.1 {
    /*@Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
     *///?}
    //? if >=1.20.1 && <26 {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 26.x GUI 换代：AbstractWidget.renderWidget → extractWidgetRenderState
    //（vanilla-26.1 AbstractWidget.java:89 protected abstract；GuiGraphics 同代改名
    // GuiGraphicsExtractor，生成树后处理统一改写）
    //? if >=26 {
    /*@Override
    protected void extractWidgetRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }*/
    //?}

    /** 版本中性自绘主体（子类唯一入口）。 */
    protected abstract void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick);

    // 1.17~1.19.2：NarratableEntry extends NarrationSupplier（updateNarration 抽象），
    // AbstractWidget 不实现 → concrete 子类必炸（1192 编译实测）；中段补实现，
    // defaultButtonNarrationText 与 1.19.4+ 同名同义（1182 AbstractWidget.java:231）
    //? if >=1.17 && <1.19.3 {
    /*
    @Override
    public void updateNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
     *///?}
    // 1.19.3 updateWidgetNarration 已抽象（f1193 AbstractWidget 实证）→ 边界 1.19.4 放宽 1.19.3
    //? if >=1.19.3 {
    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
    //?}
}
