package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.client.ClientOnlySelection;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import com.elfmcys.yesstevemodel.util.YsmText;
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
//? if >=1.21 {
/*import com.elfmcys.yesstevemodel.util.YsmFrame;*/
//?}

public class TextureButton extends YsmButton {

    public final PlayerPreviewEntity previewEntity;

    public final ModelAssembly modelAssembly;

    public TextureButton(int x, int y, PlayerPreviewEntity previewEntity, ModelAssembly modelAssembly) {
        super(x, y, 54, 102, YsmText.literal(""), button -> {
        });
        this.previewEntity = previewEntity;
        this.modelAssembly = modelAssembly;
    }

    public void onPress() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                cap.setCurrentTexture(this.previewEntity.getCurrentTextureName());
                if (NetworkHandler.isClientConnected() && !ClientOnlyMode.isForced()) {
                    NetworkHandler.sendToServer(new C2SRequestSwitchModelPacket(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName()));
                    return;
                }
                cap.initModelWithTexture(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName());
                ClientOnlySelection.save(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName());
            });
        }
    }

    //? if >=1.20 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -12369342, -12369342);
        //? if >=1.21
        /*renderPlayerPreview(guiGraphics, YsmFrame.partialTick(minecraft));*/
        //? if <1.21
        renderPlayerPreview(guiGraphics, minecraft.getFrameTime());
        String str = this.previewEntity.getCurrentTextureName();
        MutableComponent mutableComponentLiteral = YsmText.literal(ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, String.format("files.player.texture.%s", str), str));
        List listSplit = font.split(mutableComponentLiteral, 50);
        if (listSplit.size() > 1) {
            guiGraphics.drawCenteredString(font, (FormattedCharSequence) listSplit.get(0), getX() + (this.width / 2), (getY() + this.height) - 19, 15986656);
            guiGraphics.drawCenteredString(font, (FormattedCharSequence) listSplit.get(1), getX() + (this.width / 2), (getY() + this.height) - 10, 15986656);
        } else {
            guiGraphics.drawCenteredString(font, mutableComponentLiteral, getX() + (this.width / 2), (getY() + this.height) - 15, 15986656);
        }
        if (hoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY() + 1, getX() + 1, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + 1, -790560, -790560);
            guiGraphics.fillGradient((getX() + this.width) - 1, getY() + 1, getX() + this.width, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), (getY() + this.height) - 1, getX() + this.width, getY() + this.height, -790560, -790560);
        }
    }

    public void renderPlayerPreview(YsmGui guiGraphics, float partialTick) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        // 1.21.6+ RenderSystem.enableScissor 删 → GuiGraphics 剪裁（GUI 坐标，内部处理缩放）
        //? if <21.6
        RenderSystem.enableScissor((int) (getX() * guiScale), (int) (Minecraft.getInstance().getWindow().getHeight() - (((getY() + this.height) - 20) * guiScale)), (int) (this.width * guiScale), (int) ((this.height - 20) * guiScale));
        //? if >=21.6
        /*guiGraphics.graphics().enableScissor(getX(), getY(), getX() + this.width, getY() + this.height - 20);*/
        ModelPreviewRenderer.renderLivingEntityPreview(getX() + (this.width / 2.0f), getY() + (this.height / 2.0f) + 24.0f, 35.0f, partialTick, this.previewEntity, RendererManager.getPlayerRenderer(), false, true);
        //? if <21.6
        RenderSystem.disableScissor();
        //? if >=21.6
        /*guiGraphics.graphics().disableScissor();*/
    }
}