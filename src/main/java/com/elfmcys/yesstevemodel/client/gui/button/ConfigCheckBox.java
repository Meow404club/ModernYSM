package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;


import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ConfigCheckBox extends StateSwitchingButton implements ISpecialWidget {

    private static final ResourceLocation location = new ResourceLocation(YesSteveModel.MOD_ID, "texture/roulette.png");

    private final Consumer<Boolean> consumer2;

    private final Component component2;

    public ConfigCheckBox(int x, int y, int width, Component component, Consumer<Boolean> consumer) {
        super(x, y, width, 12, false);
        this.component2 = component;
        this.consumer2 = consumer;
        initTextureValues(0, 0, 128, 12, location);
    }

    public ConfigCheckBox(int x, int y, Component component, Consumer<Boolean> consumer) {
        this(x, y, 115, component, consumer);
    }

    //? if >1.17 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    // 1.16.5 StateSwitchingButton 无 getX/getY → 桥接（对位 YsmButton 同款）
    //? if <1.17 {
    /*public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
     *///?}

    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.17
        /*super.renderButton(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.17
        super.renderWidget(guiGraphics.graphics(), mouseX, mouseY, partialTick);
        guiGraphics.drawString(Minecraft.getInstance().font, this.component2, getX() + 14, getY() + 2, -1, false);
    }

    public void onClick(double mouseX, double mouseY) {
        this.isStateTriggered = !this.isStateTriggered;
        this.consumer2.accept(Boolean.valueOf(this.isStateTriggered));
    }
}