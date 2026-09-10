package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.geckolib3.extended.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//?} else {
/*import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
*///?}
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingRendererMixin extends EntityRenderer<LivingEntity> implements LivingEntityRendererAccessor {
    // 1.16.5 无 EntityRendererProvider（1.17+ 引入）：渲染器构造直接收 EntityRenderDispatcher，
    // 父类构造 EntityRenderer(EntityRenderDispatcher)（1.16.5 EntityRenderer.java:23）
    //? if >=1.17 {
    public LivingRendererMixin(EntityRendererProvider.Context pContext) {
        super(pContext);
    }
    //?} else {
    /*public LivingRendererMixin(EntityRenderDispatcher pDispatcher) {
        super(pDispatcher);
    }
    *///?}

    @Override
    @Unique
    public void tlm$renderNameTag(LivingEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }
}