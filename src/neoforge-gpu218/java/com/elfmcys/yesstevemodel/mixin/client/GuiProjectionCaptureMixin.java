package com.elfmcys.yesstevemodel.mixin.client;

import rip.ysm.gpu.GpuCapability;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * d3-gpu-218-revive：GUI/PiP 正交投影捕获（仅 21.8/21.11 线挂载，src/neoforge-gpu218 RAW 树）。
 * 21.6+ GUI 改延迟网格管线：GuiRenderer 打包 GUI 正交（1218 GuiRenderer.java:203/:336、
 * 21111 :211/:342）与 vanilla 实体 PiP 预览（1218 PictureInPictureRenderer.java:108、
 * 21111 :103）全部经 CachedOrthoProjectionMatrixBuffer.getBuffer(float,float)——捕获后
 * 「当前投影」镜像覆盖 GUI/PiP 场景（<=21.5 线这些点同走 RenderSystem.setProjectionMatrix）。
 * 矩阵在 createProjectionMatrix 内部现算（1218:49/21111:49 单一 return）→ @At("RETURN") 读返回值。
 */
@Mixin(CachedOrthoProjectionMatrixBuffer.class)
public abstract class GuiProjectionCaptureMixin {

    @Inject(method = "createProjectionMatrix", at = @At("RETURN"))
    private void ysm$captureGuiProjection(Matrix4f proj, CallbackInfoReturnable<Matrix4f> cir) {
        GpuCapability.ysm$onProjectionCaptured(proj);
    }
}
