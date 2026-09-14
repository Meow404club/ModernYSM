package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 1.16.5 RenderSystem 无 shaderLightDirections 字段（shader 灯光系 1.17+ 引入，ShaderInstance 时代）；
// 1.16.5 侧退化为普通静态方法返回 null（消费方 GpuRenderPath.refreshLights 有 null 兜底），
// 避免 required=true 配置下 accessor 目标缺失。返回型保持 org.joml.Vector3f 两侧一致
//（1.16.5 类路径已由 m2-render-pipeline-condition 补 JOML 1.10.5，Java 1.2 字节码运行时可载入）。
// 【形态分叉（1.16.5 runClient 实测）】>=1.17 为 @Accessor 接口 mixin；<1.17 若保留「空接口
// mixin」形态，Mixin prepare 报 target type mismatch: RenderSystem is not an interface——
// 空接口 mixin 无 accessor 成员即失去接口 mixin 资格。故 <1.17 降为普通 final 工具类
//（无 @Mixin 注解），同时 1.16.5 processResources 将本类从 mixins.json client 列表剔除。
// 消费方 GpuRenderPath 在 <1.17 有恒 false 闸门，该方法不会被调用。
//? if >=1.19.3 {
@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections")
    static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
}
//?}
// 1.17~1.19.2：vanilla 尚未切 JOML，字段实际类型 com.mojang.math.Vector3f[]
//（1192 RenderSystem.java:82）——accessor 泛型签名必须与字段一致（AP 校验），消费方降级不读
//? if >=1.17 && <1.19.3 {
/*
@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections")
    static com.mojang.math.Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
}
 *///?}
// 1.19.3 起 shaderLightDirections 元素类型已切 org.joml.Vector3f（1.19.3 merged jar javap 实证），
// 与上方 >=1.19.3 活跃块同一 JOML 签名，1.19.3 由该活跃块直接覆盖（审查修正：恒假冗余块删除）
//? if <1.17 {
/*public final class RenderSystemAccessor {
    private RenderSystemAccessor() {
    }

    public static Vector3f[] ysm$getShaderLightDirections() {
        return null;
    }
}
*///?}
