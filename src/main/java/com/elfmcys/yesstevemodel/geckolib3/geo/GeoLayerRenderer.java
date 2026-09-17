package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
// 26.2 submit-dag 换代（MultiBufferSource 删）：26.2 分支层签名收 SubmitNodeCollector，
// 语义=提交期上下文（几何经 submitCustomGeometry/submitModel 提交）
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;

public abstract class GeoLayerRenderer<T extends AnimatableEntity<?>> {
    //? if <26.2
    public abstract void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLightIn, T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch);

    //? if >=26.2
    /*public abstract void render(PoseStack poseStack, SubmitNodeCollector multiBufferSource, int packedLightIn, T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch);*/
}