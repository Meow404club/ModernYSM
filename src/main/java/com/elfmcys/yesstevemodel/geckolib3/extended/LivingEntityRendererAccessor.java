package com.elfmcys.yesstevemodel.geckolib3.extended;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
 *///?}

public interface LivingEntityRendererAccessor {
    // 1.21.2 render-state 化：vanilla render(T,F,F,...) 删除 → 桥收 state
    //（vanilla-1.21.3 LivingEntityRenderer.java:71 render(S,...)）
    //? if <1.21.2
    void tlm$renderNameTag(LivingEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight);

    //? if >=1.21.2 {
    /*void tlm$renderNameTag(LivingEntityRenderState pState, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight);*/
    //?}
}
