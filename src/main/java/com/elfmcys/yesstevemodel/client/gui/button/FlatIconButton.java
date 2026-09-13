package com.elfmcys.yesstevemodel.client.gui.button;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.17 {
import net.minecraft.client.gui.narration.NarrationElementOutput;
//?}
import net.minecraft.network.chat.Component;


@OnlyIn(Dist.CLIENT)
public class FlatIconButton extends AbstractWidget implements ISpecialWidget {

    private final int iconIndex;

    public FlatIconButton(int x, int y, int iconIndex, Component component) {
        super(x, y, 115, 15, component);
        this.iconIndex = iconIndex;
    }

    //? if >=1.20 && <21.11 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // FlatIconButton 直extends AbstractWidget：1.21.11 其抽象钩子仍名 renderWidget
    //（2111 AbstractWidget.java:89）→ 恢复同名覆写
    //? if >=21.11 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
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

    // 1.16.5~1.19.2 AbstractWidget 无 getX/getY（x/y public 字段 1192 AbstractWidget.java:25-26），
    // getWidth/getHeight 1.16.5 也无 → 全量桥接；1.19.4+ 父类自带 getX/getY/getWidth/getHeight
    //? if <1.19.4 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
     *///?}

    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + this.iconIndex, -280804798);
        //? if <1.17
        /*guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 2, this.getY() + (this.height - 8) / 2, 16777215, false);*/
        //? if >=1.17 && <1.20
        /*guiGraphics.renderScrollingString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 2, this.getY(), (this.getX() + this.width) - 2, this.getY() + this.height, 16777215);*/
        //? if >=1.20
        //? if <21.11
        renderScrollingString(guiGraphics.graphics(), Minecraft.getInstance().font, 2, 16777215);
        //? if >=21.11
        /*guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 2, this.getY(), 16777215, false);*/
    }

    // narration：updateNarration 抽象 1.17~1.19.2（NarratableEntry extends NarrationSupplier 且
    // AbstractWidget 不实现，1192 编译实测 concrete 子类必炸）；updateWidgetNarration 1.19.4 起
    //（1194:301）；defaultButtonNarrationText 1182:231/1192 同名存活
    //? if >=1.17 && <1.19.4 {
    /*@Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
     *///?}
    //? if >=1.19.4 {
    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
    //?}
}