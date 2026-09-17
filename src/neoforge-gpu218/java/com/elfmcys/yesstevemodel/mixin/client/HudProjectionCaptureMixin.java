package com.elfmcys.yesstevemodel.mixin.client;

import rip.ysm.gpu.GpuCapability;
import net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * d3-gpu-218-revive：hud3d 透视投影捕获（仅 21.8/21.11 线挂载，src/neoforge-gpu218 RAW 树）。
 * GameRenderer.renderLevel 尾段 hud3d 投影打包（1218 GameRenderer.java:685-689 经
 * CachedPerspectiveProjectionMatrixBuffer.getBuffer(int,int,float)）发生在世界实体绘制之后、
 * GUI 屏绘制之前——是 GUI 屏内立即式实体预览（我方 ModelPreviewRenderer 一族）绘制时刻的
 * 「当前投影」（<=21.5 线 RenderSystem.getProjectionMatrix() 在该时刻的值），故必须捕获才能
 * 让 GUI 预览的 GpuRenderPath 投影语义对齐。矩阵在 createProjectionMatrix 内部现算
 * （1218:49/21111:49 单一 return）→ @At("RETURN") 读返回值。
 */
@Mixin(CachedPerspectiveProjectionMatrixBuffer.class)
public abstract class HudProjectionCaptureMixin {

    @Inject(method = "createProjectionMatrix", at = @At("RETURN"))
    private void ysm$captureHudProjection(int width, int height, float fov, CallbackInfoReturnable<Matrix4f> cir) {
        GpuCapability.ysm$onProjectionCaptured(cir.getReturnValue());
    }
}
