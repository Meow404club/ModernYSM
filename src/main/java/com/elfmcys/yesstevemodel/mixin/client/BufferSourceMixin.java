package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.util.accessors.BufferSourceAccessor;
import com.mojang.blaze3d.vertex.BufferBuilder;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2：目标类 MultiBufferSource.BufferSource 删（render-dag 换代）→ 26.2 编译为空类
//（mixin 应用经 MixinTweaker.shouldApplyMixin 按线跳过，26.2 无 endBatch 消费场景）
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.Map;

//? if <26.2
@Mixin({MultiBufferSource.BufferSource.class})
//? if >=26.2 {
/*public class BufferSourceMixin implements BufferSourceAccessor {

    @Override
    public void initialize() {
    }
}*/
//?}
//? if <26.2 {
public class BufferSourceMixin implements BufferSourceAccessor {

    // 1.21 fixedBuffers 变 SequencedMap<RenderType, ByteBufferBuilder>
    //（vanilla-1.21.1 MultiBufferSource.java:31），@Shadow 字段描述符按版本二选一
    //? if <1.21 {
    @Shadow
    @Final
    public Map<RenderType, BufferBuilder> fixedBuffers;
    //?}
    //? if >=1.21 {
    /*@Shadow
    @Final
    public java.util.SequencedMap<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> fixedBuffers;*/
    //?}

    @Override
    @Unique
    public void initialize() {
        Iterator<RenderType> it = this.fixedBuffers.keySet().iterator();
        while (it.hasNext()) {
            ((MultiBufferSource.BufferSource) (Object) this).endBatch(it.next());
        }
    }
}