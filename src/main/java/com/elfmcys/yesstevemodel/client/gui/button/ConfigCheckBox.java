package com.elfmcys.yesstevemodel.client.gui.button;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
//? if >=21.11 {
/*import rip.ysm.gui.YsmWidget;
 *///?}
// 1.21.11 vanilla StateSwitchingButton 删除（2111 gui/components 零命中，recipebook 改用
// ImageButton 实证）→ >=21.11 分代改继承 YsmWidget（勾选态自持，fill 自绘），<21.11 不变
//? if <21.11 {
import net.minecraft.client.gui.components.StateSwitchingButton;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


import java.util.function.Consumer;

//? if <21.11 {
@OnlyIn(Dist.CLIENT)
public class ConfigCheckBox extends StateSwitchingButton implements ISpecialWidget {

    //? if >=1.21
    /*private static final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/roulette.png");*/
    //? if <1.21
    private static final ResourceLocation location = new ResourceLocation(YesSteveModel.MOD_ID, "texture/roulette.png");

    private final Consumer<Boolean> consumer2;

    private final Component component2;

    public ConfigCheckBox(int x, int y, int width, Component component, Consumer<Boolean> consumer) {
        super(x, y, width, 12, false);
        this.component2 = component;
        this.consumer2 = consumer;
        //? if neoforge
        /*initTextureValues(new net.minecraft.client.gui.components.WidgetSprites(location, location, location, location));*/
        //? if forge
        initTextureValues(0, 0, 128, 12, location);
    }

    public ConfigCheckBox(int x, int y, Component component, Consumer<Boolean> consumer) {
        this(x, y, 115, component, consumer);
    }

    // 渲染钩子三段：StateSwitchingButton.renderWidget(PoseStack) 1.19.4 起（1194:44）/
    // renderButton(PoseStack) 1.16.5~1.19.2（1192 StateSwitchingButton.java:50）/ GuiGraphics 1.20 起
    //? if >=1.20 {
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

    // 1.16.5~1.19.2 StateSwitchingButton 无 getX/getY（x/y 为 public 字段 1192 AbstractWidget.java:25-26）
    // → 桥接（对位 YsmButton 同款）；1.19.4+ 父类自带
    //? if <1.19.4 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
     *///?}

    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.19.4
        /*super.renderButton(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.19.4 && <1.20
        /*super.renderWidget(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.20
        super.renderWidget(guiGraphics.graphics(), mouseX, mouseY, partialTick);
        guiGraphics.drawString(Minecraft.getInstance().font, this.component2, getX() + 14, getY() + 2, -1, false);
    }

    public void onClick(double mouseX, double mouseY) {
        this.isStateTriggered = !this.isStateTriggered;
        this.consumer2.accept(Boolean.valueOf(this.isStateTriggered));
    }
}
//?}
//? if >=21.11 {
@OnlyIn(Dist.CLIENT)
public class ConfigCheckBox extends YsmWidget implements ISpecialWidget {

    private static final ResourceLocation location = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/roulette.png");

    private final Consumer<Boolean> consumer2;

    private final Component component2;

    private boolean isStateTriggered;

    public ConfigCheckBox(int x, int y, int width, Component component, Consumer<Boolean> consumer) {
        super(x, y, width, 12, component);
        this.component2 = component;
        this.consumer2 = consumer;
        this.isStateTriggered = false;
    }

    public ConfigCheckBox(int x, int y, Component component, Consumer<Boolean> consumer) {
        this(x, y, 115, component, consumer);
    }

    @Override
    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(location, getX(), getY(), 0, 0, 12, 12);
        if (this.isStateTriggered) {
            guiGraphics.fill(getX() + 2, getY() + 2, getX() + 10, getY() + 10, 0xFF55FF55);
        }
        guiGraphics.drawString(Minecraft.getInstance().font, this.component2, getX() + 14, getY() + 2, -1, false);
    }

    public void setStateTriggered(boolean triggered) {
        this.isStateTriggered = triggered;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.isStateTriggered = !this.isStateTriggered;
        this.consumer2.accept(Boolean.valueOf(this.isStateTriggered));
    }
}
//?}
