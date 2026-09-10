package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import rip.ysm.annotations.BufferBuilderMapping;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class) // OYSM PATCH
public abstract class BufferBuilderMixin {
    @Shadow @BufferBuilderMapping("buffer_builder_buffer")
    private ByteBuffer buffer;

    @Shadow @BufferBuilderMapping("buffer_builder_vertices")
    private int vertices;

    @Shadow @BufferBuilderMapping("buffer_builder_nextElementByte")
    private int nextElementByte;

    @Shadow @BufferBuilderMapping("buffer_builder_ensureCapacity")
    protected abstract void ensureCapacity(int size);

    // 1.16.5 BufferBuilder.mode 是 int（GL 枚举值直存），1.17+ 才是 VertexFormat.Mode
    //（1.16.5 反编译源 BufferBuilder.java:30 begin(int,VertexFormat)：166）。
    // 注解值（逻辑键）两侧不变：GeoModel.initSIMD 读 reobf 后的 @Shadow 字段名做 GetFieldID，
    // 跨版本自洽；int 型 mode 下 frozen native 的 GetFieldID 签名不匹配 → 走 catch 优雅降级
    //（快速路径有 g_ensureCapacityMethodID 非空门槛，不会带病运行），联验归 m2-native-poc-1165。
    //? if >=1.17 {
    @Shadow @BufferBuilderMapping("buffer_builder_mode")
    private VertexFormat.Mode mode;
    //?} else {
    /*@Shadow @BufferBuilderMapping("buffer_builder_mode")
    private int mode;
    *///?}
}
