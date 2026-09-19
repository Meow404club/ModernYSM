package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.ysm.gpu.GpuPreviewQueue;

/**
 * gui-tail-flush：21.6+ 延迟 GUI 网格管线预览尾 flush（仅 21.8/21.11 线挂载，src/neoforge-gpu218
 * RAW 树，挂载方式沿 GuiProjectionCaptureMixin 先例；单类双线共用——GuiRenderer.render 两线同签名，
 * 1218 GuiRenderer.java:146 / 2111 :150）。
 * 21.8 起 GUI 延迟网格化：Screen.render 只是收集相（1218 GameRenderer.java:516），预览的立即直绘
 * 发生在本窗口之前，被后绘 GUI 批次盖掉（executeDrawRange 1218 :223/:239）；GUI 正交在本类目标方法内
 * draw（1218 :199/:203、2111 :207/:211）才打包。
 * HEAD=挂载线 GUI 绘制窗打开（armed，GpuRenderPath 预览分支据此切 pending）；
 * RETURN=窗尾 flush pending 预览队列——预览与当帧 GUI 正交同窗、绘制序在 GUI 批次之后不被遮挡。
 * 渲染线程调用（GameRenderer.render 内），与 GpuPreviewQueue enqueue 同线程，无锁。
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererTailMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
    private void ysm$armTailHook(GpuBufferSlice fogSlice, CallbackInfo ci) {
        GpuPreviewQueue.ysm$armTailHook();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("RETURN"))
    private void ysm$flushPendingPreview(GpuBufferSlice fogSlice, CallbackInfo ci) {
        // 当帧 GUI 正交逻辑尺寸（GuiRenderer.draw:204 同式 window/guiScale），供当帧正交指纹比对
        Window window = Minecraft.getInstance().getWindow();
        GpuPreviewQueue.ysm$flush((float) window.getWidth() / window.getGuiScale(),
                (float) window.getHeight() / window.getGuiScale());
    }
}
