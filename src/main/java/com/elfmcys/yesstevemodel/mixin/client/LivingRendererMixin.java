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
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
 *///?}
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if <1.21.2
@Mixin({LivingEntityRenderer.class})
// 1.21.2 render-state 化：mixin shadow 类泛型同步 EntityRenderer<LivingEntity, EntityRenderState>
//（vanilla-1.21.3 EntityRenderer.java:29）
//? if >=1.21.2 {
/*@Mixin({LivingEntityRenderer.class})
*///?}
public abstract class LivingRendererMixin
//? if <1.21.2
    extends EntityRenderer<LivingEntity> implements LivingEntityRendererAccessor {
//? if >=1.21.2 {
/*    extends EntityRenderer<LivingEntity, EntityRenderState> implements LivingEntityRendererAccessor {
*///?}
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

    //? if <1.21.2 {
    @Override
    @Unique
    public void tlm$renderNameTag(LivingEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, pPackedLight);
    }
    //?}

    //? if >=1.21.2 {
    /*@Override
    @Unique
    public void tlm$renderNameTag(LivingEntityRenderState pState, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pState, pPoseStack, pBuffer, pPackedLight);
    }*/
    //?}
}
