package com.elfmcys.yesstevemodel.mixin.client;

import rip.ysm.gpu.GpuCapability;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * d3-gpu-218-revive：世界投影捕获（仅 21.8/21.11 线挂载，src/neoforge-gpu218 RAW 树）。
 * vanilla 21.8 删 RenderSystem CPU 投影读取（GpuBufferSlice 化）后，每帧世界投影仍在
 * CPU 现算后打包 UBO（vanilla-mc-1218 GameRenderer.java:647 现算含 fov/bob/portal 全量
 * → :671 levelProjectionMatrixBuffer.getBuffer(matrix4f)；21111 :749/:771 同构）。
 * 本 mixin 在打包点捕获该 Matrix4f 喂回 GpuCapability 捕获面，语义=旧线
 * RenderSystem.getProjectionMatrix() 逐点镜像。getBuffer(Matrix4f) 单一重载（1218:25/21111:25）。
 */
@Mixin(PerspectiveProjectionMatrixBuffer.class)
public abstract class LevelProjectionCaptureMixin {

    @Inject(method = "getBuffer", at = @At("HEAD"))
    private void ysm$captureLevelProjection(Matrix4f proj, CallbackInfo ci) {
        GpuCapability.ysm$onProjectionCaptured(proj);
    }
}
