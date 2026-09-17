package com.elfmcys.yesstevemodel.geckolib3.extended;

import com.mojang.blaze3d.vertex.PoseStack;
// 26.2 submit-dag 换代：collector 形 twin
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.world.entity.LivingEntity;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
 *///?}

public interface LivingEntityRendererAccessor {
    // 1.21.2 render-state 化：vanilla render(T,F,F,...) 删除 → 桥收 state
    //（vanilla-1.21.3 LivingEntityRenderer.java:71 render(S,...)）
    //? if <1.21.2
    void tlm$renderNameTag(LivingEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight);

    //? if >=1.21.2 && <26.2 {
    /*void tlm$renderNameTag(LivingEntityRenderState pState, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight);*/
    //?}
    // 26.2 collector 形 twin（21.9+ 桥实现为空壳，纯类型换代）
    //? if >=26.2 {
    /*void tlm$renderNameTag(LivingEntityRenderState pState, PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pPackedLight);*/
    //?}
}
