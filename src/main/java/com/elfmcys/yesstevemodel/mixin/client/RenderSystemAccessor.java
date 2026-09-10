package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    // 1.16.5 RenderSystem 无 shaderLightDirections 字段（shader 灯光系 1.17+ 引入，ShaderInstance 时代）；
    // 1.16.5 侧退化为普通静态方法返回 null（消费方 GpuRenderPath.refreshLights 有 null 兜底），
    // 避免 required=true 配置下 accessor 目标缺失。返回型保持 org.joml.Vector3f 两侧一致
    //（1.16.5 类路径已由 m2-render-pipeline-condition 补 JOML 1.10.5，Java 1.2 字节码运行时可载入）。
    //? if >=1.17 {
    @Accessor("shaderLightDirections")
    static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
    //?} else {
    /*static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
    *///?}
}
