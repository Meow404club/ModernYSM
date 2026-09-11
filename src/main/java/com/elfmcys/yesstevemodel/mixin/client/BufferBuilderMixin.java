package com.elfmcys.yesstevemodel.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    // frozen native（native/openysm-cpp/dllmain.cpp:355-357）用传入 Class 名拼
    // "L+name.replace('.','/');" 形态描述符做模式字段的 GetFieldID——int 型真字段（描述符 "I"）
    // 对任何类名都不可能命中，nInitSIMD 必失败。实证（dllmain.cpp 全文 grep）：
    // g_modeFieldID 缓存后从不被读取（快路径只用 buffer/vertices/nextElementByte/
    // ensureCapacity，dllmain.cpp:929-956），它纯粹是 init 期门槛。故 1.16.5 侧用
    // @Unique 桩字段（描述符 Ljava/lang/Object;）让 GetFieldID 命中——@Unique 字段
    // 名不被 SRG 重映射（dev/生产 jar 双态同名字），native 零改动、语义零偏移。
    // POC 实证归 m2-native-poc-1165（一致性对比+进世界冒烟）。
    //? if >=1.17 {
    @Shadow @BufferBuilderMapping("buffer_builder_mode")
    private VertexFormat.Mode mode;
    //?} else {
    /*@Unique
    @BufferBuilderMapping("buffer_builder_mode")
    private Object ysmSimdModeStub;
    *///?}
}
