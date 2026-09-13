package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.resource.models.AuthorInfo;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
// 1.21.11 Util 移 net.minecraft.util 子包
//? if >=21.11
/*import net.minecraft.util.Util;*/
//? if <21.11
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import com.elfmcys.yesstevemodel.util.YsmText;
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class AuthorButton extends YsmButton {

    private final AuthorInfo authorInfo;

    private final ModelAssembly modelAssembly;

    private final ResourceLocation resourceLocation;

    private final int authorIndex;

    private final List<Component> componentList;

    private int selectedContactIndex;

    private final Screen parentScreen;

    public AuthorButton(int x, int y, AuthorInfo authorInfo, ModelAssembly modelAssembly, ResourceLocation resourceLocation, int authorIndex, Screen screen) {
        super(x, y, 70, 130, YsmText.literal(""), button -> {
        });
        this.selectedContactIndex = -1;
        this.authorInfo = authorInfo;
        this.modelAssembly = modelAssembly;
        this.resourceLocation = resourceLocation;
        this.authorIndex = authorIndex;
        this.componentList = Lists.newArrayList();
        if (this.authorInfo != null) {
            renderTooltip(false);
        }
        this.parentScreen = screen;
    }

    public static AuthorButton createAuthorButton(int x, int y, Screen screen) {
        return new AuthorButton(x, y, null, null, null, -1, screen);
    }

    //? if >=1.20 && <21.11 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
    // 1.21.11 AbstractButton.renderWidget final 化 → renderContents（2111 AbstractWidget.java 实证）
    //? if >=21.11 {
    @Override
    public void renderContents(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?}
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
        Font font = Minecraft.getInstance().font;
        if (this.authorInfo == null || this.modelAssembly == null || this.resourceLocation == null) {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1891417534, -1891417534);
            guiGraphics.drawCenteredString(font, YsmText.literal("......"), getX() + (this.width / 2), getY() + (this.height / 2), ChatFormatting.GRAY.getColor().intValue());
            return;
        }
        if (hoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1892652116, -1892652116);
        } else {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1891417534, -1891417534);
        }
        guiGraphics.blit(this.resourceLocation, getX() + 3, getY() + 3, 64, 64, 0.0f, 0.0f, 64, 64, 64, 64);
        String str = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, String.format("metadata.authors.%d.name", this.authorIndex), this.authorInfo.getName());
        String str2 = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, String.format("metadata.authors.%d.role", this.authorIndex), this.authorInfo.getRole());
        String str3 = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, String.format("metadata.authors.%d.comment", this.authorIndex), this.authorInfo.getComment());
        //? if <1.17
        /*guiGraphics.drawString(font, YsmText.literal(str), getX() + 2, getY() + 72, ChatFormatting.GOLD.getColor().intValue(), false);*/
        //? if >=1.17 && <1.20
        /*guiGraphics.renderScrollingString(font, YsmText.literal(str), getX() + 2, getY() + 72, (getX() + this.width) - 2, getY() + 82, ChatFormatting.GOLD.getColor().intValue());*/
        //? if >=1.20
        //? if <21.11
        renderScrollingString(guiGraphics.graphics(), font, YsmText.literal(str), getX() + 2, getY() + 72, (getX() + this.width) - 2, getY() + 82, ChatFormatting.GOLD.getColor().intValue());
        //? if >=21.11
        /*guiGraphics.drawString(font, YsmText.literal(str), getX() + 2, getY() + 72, ChatFormatting.GOLD.getColor().intValue(), false);*/
        guiGraphics.drawCenteredString(font, str2, getX() + 35, getY() + 82, ChatFormatting.GREEN.getColor().intValue());
        drawWrappedText(guiGraphics, YsmText.literal(str3), getX() + 3, getY() + 95, 64, -1);
    }

    public void drawWrappedText(YsmGui guiGraphics, FormattedText formattedText, int x, int y, int wrapWidth, int color) {
        Font font = Minecraft.getInstance().font;
        for (FormattedCharSequence formattedCharSequence : font.split(formattedText, wrapWidth)) {
            guiGraphics.drawString(font, formattedCharSequence, x, y, color, false);
            y += 9;
            if (y > getY() + this.height) {
                return;
            }
        }
    }

    public void refreshContactComponents(YsmGui guiGraphics, Screen screen, int mouseX, int mouseY) {
        if (this.isHovered && !this.componentList.isEmpty()) {
            guiGraphics.renderScreenComponentTooltip(screen, Minecraft.getInstance().font, this.componentList, mouseX, mouseY);
        } else if (this.selectedContactIndex != -1) {
            this.selectedContactIndex = -1;
            renderTooltip(false);
        }
    }

    //? if neoforge
/*public boolean mouseScrolled(double mouseX, double mouseY, double delta, double scrollY) {*/
//? if forge
public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0.0d) {
            if (this.selectedContactIndex > 0) {
                this.selectedContactIndex--;
                renderTooltip(false);
                return true;
            }
            return true;
        }
        if (delta < 0.0d) {
            if (this.selectedContactIndex < this.componentList.size() - 2) {
                this.selectedContactIndex++;
                renderTooltip(false);
                return true;
            }
            return true;
        }
        //? if neoforge
/*return super.mouseScrolled(mouseX, mouseY, delta, delta);*/
//? if forge
return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderTooltip(boolean copied) {
        if (this.authorInfo == null) {
            return;
        }
        this.componentList.clear();
        for (int i = 0; i < this.authorInfo.getContact().size(); i++) {
            MutableComponent componentLiteral = YsmText.literal(this.authorInfo.getContact().getKeyAt(i) + ": " + this.authorInfo.getContact().getValueAt(i));
            if (i == this.selectedContactIndex) {
                componentLiteral.append(YsmText.literal(copied ? " ✓" : " ◀").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            }
            this.componentList.add(componentLiteral);
        }
        if (!this.componentList.isEmpty()) {
            this.componentList.add(YsmText.translatable("gui.yes_steve_model.model.info.contact.click_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public void onPress() {
        String link;
        if (this.authorInfo == null) {
            return;
        }
        int i = this.selectedContactIndex;
        if (i == -1) {
            i = 0;
        }
        if (i < 0 || i >= this.authorInfo.getContact().size() || (link = this.authorInfo.getContact().getValueAt(i)) == null) {
            return;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            Minecraft.getInstance().setScreen(new ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    Util.getPlatform().openUri(link);
                }
                Minecraft.getInstance().setScreen(this.parentScreen);
            }, link, true));
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(link);
        if (this.selectedContactIndex == -1) {
            this.selectedContactIndex = 0;
        }
        renderTooltip(true);
    }
}
