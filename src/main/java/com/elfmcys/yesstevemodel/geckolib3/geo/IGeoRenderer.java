package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.CustomEntityTranslucentRenderType;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
// 26.2 submit-dag 换代：MultiBufferSource/RenderBuffers 全删（/tmp/vanilla-262 全树 0 引用），
// 立即模式缓冲由 SubmitNodeCollector 取代；RTB 槽位语义不变（26.2 承载 collector，
// 顶点提交走 submitCustomGeometry 逃生口，/tmp/vanilla-262 SubmitNodeCollection.java:350
// 实证 pose=poseStack.last().copy() 提交期捕获）
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGeoRenderer<T extends AnimatableEntity<?>> {
    //? if >=26.2
    /*SubmitNodeCollector getCurrentRTB();

    default void setCurrentRTB(SubmitNodeCollector bufferSource) {
    }*/

    //? if <26.2
    MultiBufferSource getCurrentRTB();

    //? if <26.2 {
    default void setCurrentRTB(MultiBufferSource bufferSource) {
    }
    //?}

    //? if <26.2 {
    default void renderWithBone(AnimatedGeoModel model, T animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer vertexConsumer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
        renderLate(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    default void renderWithBoneAndRenderType(AnimatedGeoModel model, T animatable, float partialTick, RenderType renderType, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, int i, @Nullable VertexConsumer vertexConsumer, int i2, int i3, float f2, float f3, float f4, float f5) {
        if (vertexConsumer == null) {
            vertexConsumer = bufferSource.getBuffer(renderType);
        }
        animatable.resetAnimationState();
        ResourceLocation tex = animatable.getTextureLocation();
        NativeModelRenderer.renderMesh(vertexConsumer, poseStack.last(), model.getGeoModel(), model.getMatrixData(), model.getAbsPivotData(), i, 0, i2, i3, f2, f3, f4, f5, tex);
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }
    //?}

    // 26.2 collector 形：几何经 submitCustomGeometry 逃生口提交（pose 由提交期捕获，
    // SubmitNodeCollection.java:350 实证），lambda 即原 getBuffer→renderMesh 链
    //? if >=26.2 {
    /*default void renderWithBone(AnimatedGeoModel model, T animatable, float partialTick, PoseStack poseStack, @Nullable SubmitNodeCollector bufferSource, @Nullable VertexConsumer vertexConsumer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
        renderLate(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    default void renderWithBoneAndRenderType(AnimatedGeoModel model, T animatable, float partialTick, RenderType renderType, PoseStack poseStack, @Nullable SubmitNodeCollector bufferSource, int i, @Nullable VertexConsumer vertexConsumer, int i2, int i3, float f2, float f3, float f4, float f5) {
        if (vertexConsumer == null) {
            animatable.resetAnimationState();
            ResourceLocation tex = animatable.getTextureLocation();
            bufferSource.submitCustomGeometry(poseStack, renderType, (pose, vc) ->
                    NativeModelRenderer.renderMesh(vc, pose, model.getGeoModel(), model.getMatrixData(), model.getAbsPivotData(), i, 0, i2, i3, f2, f3, f4, f5, tex));
        } else {
            animatable.resetAnimationState();
            ResourceLocation tex = animatable.getTextureLocation();
            NativeModelRenderer.renderMesh(vertexConsumer, poseStack.last(), model.getGeoModel(), model.getMatrixData(), model.getAbsPivotData(), i, 0, i2, i3, f2, f3, f4, f5, tex);
        }
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
    }
    *///?}

    //? if <26.2 {
    default void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                             @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = animatable.getHeightScale();
            float height = animatable.getWidthScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            @Nullable VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue,
                            float alpha) {
    }
    //?}

    //? if >=26.2 {
    /*default void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                             @Nullable SubmitNodeCollector bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = animatable.getHeightScale();
            float height = animatable.getWidthScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T animatable, PoseStack poseStack, float partialTick, SubmitNodeCollector bufferSource,
                            @Nullable VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue,
                            float alpha) {
    }
    *///?}

    @Nullable
    default RenderType getRenderType(ResourceLocation resourceLocation, boolean z, boolean z2, boolean z3) {
        if (z) {
            if (z3) {
                return CustomEntityTranslucentRenderType.get(resourceLocation);
            }
            return RenderType.entityCutoutNoCull(resourceLocation);
        }
        if (z2) {
            return RenderType.outline(resourceLocation);
        }
        return null;
    }

    //? if <26.2 {
    default Color getRenderColor(T animatable, float partialTick, PoseStack poseStack, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }
    //?}

    //? if >=26.2 {
    /*default Color getRenderColor(T animatable, float partialTick, PoseStack poseStack, @Nullable SubmitNodeCollector bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }*/
    //?}

    @NotNull
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }
}