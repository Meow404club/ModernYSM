package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ModelUploadScreen extends Screen implements ModelUploadSession.Listener {
    private final Screen parentScreen;
    private long lastFlashTime = 0L;
    private String error = "";
    private float displayedProgress = 0f;
    private float prevProgressTarget = -1f;

    public ModelUploadScreen(Screen parent) {
        super(YsmText.literal("upload"));
        this.parentScreen = parent;
    }

    private static void drawBorder(YsmGui g, int x1, int y1, int x2, int y2, int w, int color) {
        g.fill(x1, y1, x2, y1 + w, color);
        g.fill(x1, y2 - w, x2, y2, color);
        g.fill(x1, y1, x1 + w, y2, color);
        g.fill(x2 - w, y1, x2, y2, color);
    }

    @Override
    public void init() {
// 1.16.5 无 Screen.clearWidgets（死注释形态的 init(mc,w,h)+return 从未生效，1.16.5 字段
// 由 setScreen→init(Minecraft,w,h) 先行赋值，无参 init() 直接可用，m2.6 实测语义）
//? if >=1.17 {
        clearWidgets();
//?}
        ModelUploadSession.addListener(this);
        ysmAddWidget(new FlatColorButton(this.width - 70, 10, 60, 18, YsmText.literal("Back"), button -> Minecraft.getInstance().setScreen(this.parentScreen)));
    }

    @Override
    public void removed() {
        ModelUploadSession.removeListener(this);
        ModelUploadSession.clearIfTerminal();
    }

    @Override
    public void onSessionUpdate(ModelUploadSession session) {
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty()) {
            return;
        }
        this.error = "";
        this.lastFlashTime = Util.getMillis();
        Path path = paths.get(0);
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".ysm")) {
            this.error = "Invalid file type, expected .ysm";
            return;
        }
        ModelUploadSession existing = ModelUploadSession.getInstance();
        if (existing != null && !existing.isTerminal()) {
            this.error = "Upload already in progress";
            return;
        }
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            this.error = "Failed to read file: " + e.getMessage();
            return;
        }
        String stem = fileName.substring(0, fileName.length() - 4);
        String modelId = stem.toLowerCase();
        if (modelId.isEmpty()) {
            this.error = "Cannot derive a valid model id from filename: " + stem;
            return;
        }
        String err = ModelUploadSession.start(modelId, data);
        if (err != null) {
            this.error = err;
        }
    }

    //? if >=1.20 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    public void render(YsmGui g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xC0000000);

        long sinceFlash = Util.getMillis() - this.lastFlashTime;
        int borderColor;
        int borderWidth;
        if (sinceFlash < 900) {
            float t = 1f - (float) sinceFlash / 900;
            int alpha = Math.min(255, Math.max(0, (int) (255 * t)));
            borderColor = (alpha << 24) | 0x00FFC107;
            borderWidth = 4;
        } else {
            borderColor = 0x66808080;
            borderWidth = 2;
        }
        drawBorder(g, 0, 0, this.width, this.height, borderWidth, borderColor);

        ModelUploadSession session = ModelUploadSession.getInstance();
        if (session == null) {
            renderEmptyState(g);
        } else {
            renderSessionState(g, session);
        }

        if (!this.error.isEmpty()) {
            MutableComponent err = YsmText.literal(this.error).withStyle(ChatFormatting.RED);
            int w = this.font.width(err);
            g.drawString(this.font, err, (this.width - w) / 2, this.height - 60, 0xFFFFFFFF, false);
        }

//? if <1.17
        /*super.render(g.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.17 && <1.20
        /*super.render(g.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.20
        super.render(g.graphics(), mouseX, mouseY, partialTick);
    }

    private void renderEmptyState(YsmGui guiGraphics) {
        MutableComponent main = YsmText.literal("Drag a YSM file into this window").withStyle(ChatFormatting.WHITE);
        MutableComponent sub = YsmText.literal("Require standalone ysm model file.").withStyle(ChatFormatting.GRAY);
        int cx = this.width / 2;
        int cy = this.height / 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy - 14, 0);
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        int mw = this.font.width(main);
        guiGraphics.drawString(this.font, main, -mw / 2, 0, 0xFFFFFFFF);
        guiGraphics.pose().popPose();
        int sw = this.font.width(sub);
        guiGraphics.drawString(this.font, sub, cx - sw / 2, cy + 22, 0xFFAAAAAA);
        if (ModelUploadSession.hasServerLimits()) {
            MutableComponent limit = YsmText.literal("Size limit: " + ModelUploadSession.formatBytes(ModelUploadSession.getLastMaxTotalBytes())).withStyle(ChatFormatting.DARK_GRAY);
            int lw = this.font.width(limit);
            guiGraphics.drawString(this.font, limit, cx - lw / 2, cy + 36, 0xFFFFFFFF);
        }
    }

    private void renderSessionState(YsmGui guiGraphics, ModelUploadSession session) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        ChatFormatting color;
        switch (session.getState()) {
            case COMPLETED:
                color = ChatFormatting.GREEN;
                break;
            case FAILED:
                color = ChatFormatting.RED;
                break;
            default:
                color = ChatFormatting.YELLOW;
                break;
        }
        Component title = YsmText.literal(session.getMessage()).withStyle(color);
        int tw = this.font.width(title);
        guiGraphics.drawString(this.font, title, cx - tw / 2, cy - 32, 0xFFFFFFFF);

        Component sub = YsmText.literal(session.getModelId()).withStyle(ChatFormatting.GRAY);
        int sw = this.font.width(sub);
        guiGraphics.drawString(this.font, sub, cx - sw / 2, cy - 16, 0xFFFFFFFF);

        int barW = 320;
        int barH = 14;
        int barX = cx - barW / 2;
        int barY = cy + 4;
        float target = session.getProgress();
        if (target < prevProgressTarget) {
            displayedProgress = target;
        }
        prevProgressTarget = target;
        displayedProgress += (target - displayedProgress) * 0.18f;
        if (Math.abs(target - displayedProgress) < 0.001f) {
            displayedProgress = target;
        }
        int fillW = (int) (barW * displayedProgress);
        int fillColor;
        if (session.getState() == ModelUploadSession.State.FAILED) {
            fillColor = 0xFFD23232;
        } else if (session.getState() == ModelUploadSession.State.COMPLETED) {
            fillColor = 0xFF4CAF50;
        } else {
            fillColor = 0xFFFFC107;
        }
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF2A2A2A);
        if (fillW > 0) {
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        }
        if (session.getState() == ModelUploadSession.State.UPLOADING && fillW > 4) {
            long now = Util.getMillis();
            int period = 1400;
            int travel = fillW + 40;
            int shimmerX = (int) (((now % period) / (float) period) * travel) - 20;
            int shimmerW = 24;
            int left = barX + Math.max(0, shimmerX);
            int right = barX + Math.min(fillW, shimmerX + shimmerW);
            if (right > left) {
                guiGraphics.fill(left, barY + 1, right, barY + barH - 1, 0x55FFFFFF);
            }
        }
        guiGraphics.fill(barX, barY, barX + barW, barY + 1, -1);
        guiGraphics.fill(barX, barY + barH - 1, barX + barW, barY + barH, -1);
        guiGraphics.fill(barX, barY, barX + 1, barY + barH, -1);
        guiGraphics.fill(barX + barW - 1, barY, barX + barW, barY + barH, -1);

        String stat = ModelUploadSession.formatBytes(session.getSentBytes()) + " / " + ModelUploadSession.formatBytes(session.getTotalBytes());
        int statW = this.font.width(stat);
        guiGraphics.drawString(this.font, stat, cx - statW / 2, barY + barH + 6, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
    }
    // addRenderableWidget/addWidget 均为 protected 实例方法（JLS 6.6.2 子类内才可调）→ 桥方法；
    // 泛型返回保持原 addRenderableWidget 的链式取回语义（如 .setTooltipText 续链）
    //? if <1.17 {
    /*private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractButton) {
            this.addButton((net.minecraft.client.gui.components.AbstractButton) widget);
        } else {
            this.addWidget(widget);
            ((com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor) this).ysm$getRenderables().add(widget);
        }
        return widget;
    }
     *///?} else {
    private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        this.addRenderableWidget(widget);
        return widget;
    }
    //?}

}
