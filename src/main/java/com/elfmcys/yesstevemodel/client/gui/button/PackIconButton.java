package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import rip.ysm.util.RenderCompat;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import com.elfmcys.yesstevemodel.util.YsmText;
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//? if >=1.16.2
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class PackIconButton extends YsmButton {

    //? if >=1.21
    /*private static final ResourceLocation default_pack_icon = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/default_pack_icon.png");*/
    //? if <1.21
    private static final ResourceLocation default_pack_icon = new ResourceLocation(YesSteveModel.MOD_ID, "texture/default_pack_icon.png");

    private final ModelPackData packData;

    public PackIconButton(int x, int y, int width, int height, ModelPackData packData, OnPress onPress) {
        super(x, y, width, height, YsmText.literal(ModelMetadataPresenter.getLocalizedString(packData, "name", packData.getName())), onPress);
        this.packData = packData;
    }

    //? if >=1.20 && <21.11 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 1.21.11 AbstractButton.renderWidget final 化 → renderContents
    //? if >=21.11 && <26 {
    /*
    @Override
    public void renderContents(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >=1.19.4 && <1.20 {
    /*@Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}
    //? if <1.19.4 {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    @Override
    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -6598176, -6598176);
        ResourceLocation location = FileTypeUtil.getPackIconLocation(this.packData.getPath());
        AbstractTexture texture = guiGraphics.getTexture(location);
        RenderCompat.enableBlend();
        RenderCompat.defaultBlendFunc();
        // 1.21.4 MissingTextureAtlasSprite.getTexture() 删除 → 以缺省纹理
        // TextureManager.getTexture(rl) 的返回比对改为引用缺省单参返回值不可行，
        // 直接以 RegisteredTexture 判定改为：对缺省纹理不做特判（未注册 rl 本就返回缺省
        // 纹理实例，blit 同样渲染灰白占位）——保留图标分支仅 <21.4
        //? if <21.4 {
        if (texture == MissingTextureAtlasSprite.getTexture()) {
            guiGraphics.blit(default_pack_icon, getX(), getY(), 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        } else {
            guiGraphics.blit(location, getX(), getY(), 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        }
        //?}
        //? if >=21.4 {
        /*guiGraphics.blit(location, getX(), getY(), 0.0f, 0.0f, this.width, this.height, this.width, this.height);*/
        //?}
        RenderCompat.disableBlend();
        List listSplit = font.split(getMessage(), 45);
        if (listSplit.size() > 1) {
            //? if <1.16.2 {
            /*drawCenteredString(guiGraphics, font, (net.minecraft.network.chat.FormattedText) listSplit.get(0), getX() + (this.width / 2), (getY() + this.height) - 19, 5592405);
            drawCenteredString(guiGraphics, font, (net.minecraft.network.chat.FormattedText) listSplit.get(1), getX() + (this.width / 2), (getY() + this.height) - 10, 5592405);*/
            //?}
            //? if >=1.16.2 {
            drawCenteredString(guiGraphics, font, (FormattedCharSequence) listSplit.get(0), getX() + (this.width / 2), (getY() + this.height) - 19, 5592405);
            drawCenteredString(guiGraphics, font, (FormattedCharSequence) listSplit.get(1), getX() + (this.width / 2), (getY() + this.height) - 10, 5592405);
            //?}
        } else {
            drawCenteredString(guiGraphics, font, getMessage(), getX() + (this.width / 2), (getY() + this.height) - 15, 5592405);
        }
        if (hoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY() + 1, getX() + 1, (getY() + this.height) - 1, -1982745, -1982745);
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + 1, -1982745, -1982745);
            guiGraphics.fillGradient((getX() + this.width) - 1, getY() + 1, getX() + this.width, (getY() + this.height) - 1, -1982745, -1982745);
            guiGraphics.fillGradient(getX(), (getY() + this.height) - 1, getX() + this.width, getY() + this.height, -1982745, -1982745);
        }
    }

    public void renderDescription(YsmGui guiGraphics, Screen screen, int mouseX, int mouseY) {
        String str = ModelMetadataPresenter.getLocalizedString(this.packData, "description", this.packData.getDescription());
        if (StringUtils.isBlank(str)) {
            return;
        }
        List<Component> listSingletonList = Collections.singletonList(YsmText.literal(str));
        if (/*? if >=1.18 && <1.19.4 {*/ /*isHoveredOrFocused()*//*?} else {*/ isHovered() /*?}*/) {
            //? if <21.6
            guiGraphics.pose().pushPose();
            //? if >=21.6
            /*guiGraphics.pose().pushMatrix();*/
            //? if <21.6
            guiGraphics.pose().translate(0.0f, 0.0f, 4000.0f);
            //? if >=21.6
            /*guiGraphics.pose().translate(0.0f, 0.0f);*/
            guiGraphics.renderScreenComponentTooltip(screen, Minecraft.getInstance().font, listSingletonList, mouseX, mouseY);
            //? if <21.6
            guiGraphics.pose().popPose();
            //? if >=21.6
            /*guiGraphics.pose().popMatrix();*/
        }
    }

    private static void drawCenteredString(YsmGui guiGraphics, Font font, Component component, int centerX, int y, int color) {
        guiGraphics.drawString(font, component, centerX - (font.width(component) / 2), y, color, false);
    }

    // 1.16.1 无 FormattedCharSequence → <1.16.2 段为 FormattedText 形（其余行为等价）
    //? if <1.16.2 {
    /*private static void drawCenteredString(YsmGui guiGraphics, Font font, net.minecraft.network.chat.FormattedText formattedCharSequence, int centerX, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, centerX - (font.width(formattedCharSequence) / 2), y, color, false);
    }
     *///?}
    //? if >=1.16.2 {
    private static void drawCenteredString(YsmGui guiGraphics, Font font, FormattedCharSequence formattedCharSequence, int centerX, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, centerX - (font.width(formattedCharSequence) / 2), y, color, false);
    }
    //?}
}