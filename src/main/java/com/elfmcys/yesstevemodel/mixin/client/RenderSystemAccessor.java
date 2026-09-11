package com.elfmcys.yesstevemodel.mixin.client;

import org.joml.Vector3f;

// 1.16.5 RenderSystem 无 shaderLightDirections 字段（shader 灯光系 1.17+ 引入，ShaderInstance 时代）；
// 1.16.5 侧退化为普通静态方法返回 null（消费方 GpuRenderPath.refreshLights 有 null 兜底），
// 避免 required=true 配置下 accessor 目标缺失。返回型保持 org.joml.Vector3f 两侧一致
//（1.16.5 类路径已由 m2-render-pipeline-condition 补 JOML 1.10.5，Java 1.2 字节码运行时可载入）。
// 【形态分叉（1.16.5 runClient 实测）】>=1.17 为 @Accessor 接口 mixin；<1.17 若保留「空接口
// mixin」形态，Mixin prepare 报 target type mismatch: RenderSystem is not an interface——
// 空接口 mixin 无 accessor 成员即失去接口 mixin 资格。故 <1.17 降为普通 final 工具类
//（无 @Mixin 注解），同时 1.16.5 processResources 将本类从 mixins.json client 列表剔除。
// 消费方 GpuRenderPath 在 <1.17 有恒 false 闸门，该方法不会被调用。
//? if >=1.17 {
@com.spongepowered.asm.mixin.Mixin(com.mojang.blaze3d.systems.RenderSystem.class)
public interface RenderSystemAccessor {
    @com.spongepowered.asm.mixin.gen.Accessor("shaderLightDirections")
    static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
}
//?} else {
/*public final class RenderSystemAccessor {
    private RenderSystemAccessor() {
    }

    public static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
}
*///?}
