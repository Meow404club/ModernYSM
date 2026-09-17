package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.CustomEntityTranslucentRenderType;
import com.elfmcys.yesstevemodel.client.renderer.CustomPlayerRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//?}
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.elfmcys.yesstevemodel.event.api.EventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class RenderFirstPlayerBackground {
    // 因为RenderHandEvent可有几率会渲染多次，所以为了避免多次渲染，这样设计
    private static boolean currentFrameRendered = false;

    private RenderFirstPlayerBackground() {
    }

    public static void resetFrame() {
        currentFrameRendered = false;
    }

    //? if <26.2 {
    public static void onRenderHand(PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, float partialTick) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || currentFrameRendered) {
            return;
        }
        currentFrameRendered = true;
        PlayerCapability.get(player).ifPresent(cap -> {
            if (!cap.isModelActive()) {
                return;
            }
            String modelId = cap.getModelId();
            ModelAssembly modelAssembly = cap.getModelAssembly();
            if (modelAssembly == null || !modelAssembly.getAnimationBundle().getArmModel().hasCustomLimbs) {
                return;
            }
            CustomPlayerRenderer instance = RendererManager.getPlayerRenderer();
            EventResult result = SpecialPlayerRenderEvent.post(new SpecialPlayerRenderEvent(player, cap, modelId));
            if (result.isFalse()) {
                return;
            }
            ResourceLocation resourceLocationB_ = cap.getTextureLocation();
            int textureIndex = cap.getTextureIndex();
            VertexConsumer buffer = multiBufferSource.getBuffer(CustomEntityTranslucentRenderType.get(resourceLocationB_));
            if (instance != null) {
                poseStack.pushPose();
                // 1.16.5 Options.bobView 为 public boolean 字段（1.17+ 才是 OptionInstance 供应商风格）
                //? if <1.17
                // if (Minecraft.getInstance().options.bobView) {
                //? if >=1.17 && <1.19
                /*if (Minecraft.getInstance().options.bobView) {*/
                // 1.19.0 bobView 字段私有化 + bobView() 访问器同入（f119 Options.java:711 实证）
                //? if >=1.19
                if (Minecraft.getInstance().options.bobView().get()) {
                    applyHandTransform(poseStack, partialTick, player);
                }
                poseStack.translate(0.0d, -1.5d, 0.0d);
                NativeModelRenderer.renderMesh(buffer, poseStack.last(), modelAssembly.getAnimationBundle().getArmModel(), modelAssembly.getAnimationBundle().getArmModel().getBoneTransformData(), null, textureIndex, 3, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f, resourceLocationB_);
                poseStack.popPose();
            }
        });
    }
    //?}

    // 26.2 collector 形 twin：getBuffer → submitCustomGeometry（RenderHandEvent 携带 collector）
    //? if >=26.2 {
    /*public static void onRenderHand(PoseStack poseStack, SubmitNodeCollector multiBufferSource, int packedLight, float partialTick) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_MODEL.get()) {
            return;
        }
        if (GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || currentFrameRendered) {
            return;
        }
        currentFrameRendered = true;
        PlayerCapability.get(player).ifPresent(cap -> {
            if (!cap.isModelActive()) {
                return;
            }
            String modelId = cap.getModelId();
            ModelAssembly modelAssembly = cap.getModelAssembly();
            if (modelAssembly == null || !modelAssembly.getAnimationBundle().getArmModel().hasCustomLimbs) {
                return;
            }
            CustomPlayerRenderer instance = RendererManager.getPlayerRenderer();
            EventResult result = SpecialPlayerRenderEvent.post(new SpecialPlayerRenderEvent(player, cap, modelId));
            if (result.isFalse()) {
                return;
            }
            ResourceLocation resourceLocationB_ = cap.getTextureLocation();
            int textureIndex = cap.getTextureIndex();
            if (instance != null) {
                poseStack.pushPose();
                if (Minecraft.getInstance().options.bobView().get()) {
                    applyHandTransform(poseStack, partialTick, player);
                }
                poseStack.translate(0.0d, -1.5d, 0.0d);
                multiBufferSource.submitCustomGeometry(poseStack, CustomEntityTranslucentRenderType.get(resourceLocationB_), (pose, vc) ->
                        NativeModelRenderer.renderMesh(vc, pose, modelAssembly.getAnimationBundle().getArmModel(), modelAssembly.getAnimationBundle().getArmModel().getBoneTransformData(), null, textureIndex, 3, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f, resourceLocationB_));
                poseStack.popPose();
            }
        });
    }*/
    //?}

    private static void applyHandTransform(PoseStack poseStack, float partialTick, Player player) {
        // 1.21.2 Entity.walkDist/walkDistO 删除（LivingEntity.walkAnimation WalkAnimationState
        // 接管，vanilla-1.21.3 LivingEntity.java:213/WalkAnimationState 实证）：
        // walkDist ≈ walkAnimation.position()（含 positionScale，平地=1）、
        // walkDist-walkDistO ≈ walkAnimation.speed()（update(): position+=speed 同源）
        //? if >=1.21.2
        /*float walkPhase = -(player.walkAnimation.position() + (player.walkAnimation.speed() * partialTick));*/
        //? if <1.21.2
        float walkPhase = -(player.walkDist + ((player.walkDist - player.walkDistO) * partialTick));
        // 1.21.9 oBob/bob 移 ClientAvatarState → getInterpolatedBob(f)（同 QueryBinding 注）
        //? if >=21.9
        /*float fLerp = (player instanceof net.minecraft.client.player.AbstractClientPlayer)
            ? ((net.minecraft.client.player.AbstractClientPlayer) player).avatarState().getInterpolatedBob(partialTick) : 0.0f;*/
        //? if <21.9
        float fLerp = Mth.lerp(partialTick, player.oBob, player.bob);
        poseStack.translate((-Mth.sin(walkPhase * 3.1415927f)) * fLerp * 0.5f, Math.abs(Mth.cos(walkPhase * 3.1415927f) * fLerp), 0.0d);
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.ZN.rotationDegrees(Mth.sin(walkPhase * 3.1415927f) * fLerp * 3.0f));
        //? if >=1.17 && <1.19.3
        /*poseStack.mulPose(com.mojang.math.Vector3f.ZN.rotationDegrees(Mth.sin(walkPhase * 3.1415927f) * fLerp * 3.0f));*/
        //? if >=1.19.3
        poseStack.mulPose(Axis.ZN.rotationDegrees(Mth.sin(walkPhase * 3.1415927f) * fLerp * 3.0f));
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.XN.rotationDegrees(Math.abs(Mth.cos((walkPhase * 3.1415927f) - 0.2f) * fLerp) * 5.0f));
        //? if >=1.17 && <1.19.3
        // poseStack.mulPose(com.mojang.math.Vector3f.XN.rotationDegrees(Math.abs(Mth.cos((walkPhase * 3.1415927f) - 0.2f) * fLerp) * 5.0f));
        //? if >=1.19.3
        poseStack.mulPose(Axis.XN.rotationDegrees(Math.abs(Mth.cos((walkPhase * 3.1415927f) - 0.2f) * fLerp) * 5.0f));
    }
}
