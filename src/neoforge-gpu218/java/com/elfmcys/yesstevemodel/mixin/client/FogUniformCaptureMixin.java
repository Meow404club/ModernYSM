package com.elfmcys.yesstevemodel.mixin.client;

import rip.ysm.gpu.GpuCapability;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

/**
 * d3-gpu-218-revive：雾参数捕获（仅 21.8/21.11 线挂载，src/neoforge-gpu218 RAW 树）。
 * vanilla 每帧雾仍 CPU 全参现算后打包 UBO：setupFog（1218 FogRenderer.java:166/21111 :162）
 * 算 fogColor(Vector4f)+FogData 全字段，经 updateBuffer 写入雾 UBO（1218 :188→def :213、
 * 21111 :184→def :205，两线逐参同构——setupFog 签名两线不同（1218 六参含 foggy 布尔，
 * 21111 五参）故捕获点落 updateBuffer 避开分代）。envStart/End=旧 FogParameters.start/end
 * 语义槽位（旧线 RenderSystem.getShaderFogStart/End 消费值）；雾形状已随 FogParameters
 * 代删除，消费侧取 0=sphere。updateBuffer 唯二调用=构造器（:59/:57，NONE 态全 MAX_VALUE，
 * 捕获后即「当前雾=无雾」语义自洽）与 setupFog（每帧世界雾）。
 */
@Mixin(FogRenderer.class)
public abstract class FogUniformCaptureMixin {

    @Inject(method = "updateBuffer", at = @At("HEAD"))
    private void ysm$captureFogParams(ByteBuffer buf, int offset, Vector4f color,
                                      float envStart, float envEnd,
                                      float renderDistStart, float renderDistEnd,
                                      float skyEnd, float cloudEnd, CallbackInfo ci) {
        GpuCapability.ysm$onFogCaptured(color.x(), color.y(), color.z(), color.w(), envStart, envEnd);
    }
}
