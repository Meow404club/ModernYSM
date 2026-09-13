package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.util.accessors.BufferSourceAccessor;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.Map;

@Mixin({MultiBufferSource.BufferSource.class})
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