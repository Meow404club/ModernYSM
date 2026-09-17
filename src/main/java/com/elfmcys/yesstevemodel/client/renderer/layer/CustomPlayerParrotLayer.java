package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
// 1.21.11 ParrotModel 移 client.model.animal.parrot 子包
//? if >=21.11
/*import net.minecraft.client.model.animal.parrot.ParrotModel;*/
//? if <21.11
import net.minecraft.client.model.ParrotModel;
// 1.16.5 无 ModelLayers/EntityRendererProvider（1.17 模型重写），ParrotModel 为无参传统构造
//? if >=1.17 {
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
// 1.21.11 Parrot 移 animal.parrot 子包
//? if >=21.11
/*import net.minecraft.world.entity.animal.parrot.Parrot;*/
//? if <21.11
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//? }

public class CustomPlayerParrotLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private static final String TAG_ID = "id";

    private static final String TAG_VARIANT = "Variant";

    private final ParrotModel parrotModel;

    //? if <1.17 {
    // public CustomPlayerParrotLayer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //     this.parrotModel = new ParrotModel();
    // }
    //? } else {
    public CustomPlayerParrotLayer(EntityRendererProvider.Context context) {
        this.parrotModel = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
    }
    //? }

    //? if <26.2 {
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model == null) {
            return;
        }
        if (!model.leftShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!model.rightShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }
    //?}

    //? if <26.2 {
    private void renderParrot(PoseStack poseStack, MultiBufferSource bufferSource, AnimatedGeoModel model, int packedLightIn, Player player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean isLeftShoulder) {
        // 1.21.9 肩膀鹦鹉数据迁移：getShoulderEntityLeft/Right(CompoundTag) 删 →
        // ClientAvatarEntity#getParrotVariantOnShoulder(boolean left)（AbstractClientPlayer 覆写）。
        // 旧 CompoundTag 链路用空 Tag 保形（byString→empty→ifPresent 不执行，运行时惰性），
        // 21.9+ 实渲染走下方 shoulderVariant 分支——避免 <21.9 外层块内嵌行条件（嵌套变异碎生成树）
        //? if >=21.9 {
        /*Parrot.Variant shoulderVariant =
            ((net.minecraft.client.player.AbstractClientPlayer) player).getParrotVariantOnShoulder(isLeftShoulder);
        CompoundTag shoulderEntityLeft = new CompoundTag();*/
        //?}
        //? if <21.9 {
        CompoundTag shoulderEntityLeft = isLeftShoulder ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        // 1.21.5 CompoundTag getString/getInt 返回 Optional（同 YSMBinding 注）
        //? if <21.5
        EntityType.byString(shoulderEntityLeft.getString(TAG_ID)).filter(entityType -> entityType == EntityType.PARROT).ifPresent(entityType -> {
        //? if >=21.5
        /*EntityType.byString(shoulderEntityLeft.getStringOr(TAG_ID, "")).filter(entityType -> entityType == EntityType.PARROT).ifPresent(entityType -> {*/
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            //? if <1.17
            // poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f));
            // 1.19.3 起 Axis 枚举替换 Vector3f 常量旋转（1194 Axis.java；1192 sources 无此类）
            //? if >=1.17 && <1.19.3
            /*poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f));*/
            //? if >=1.19.3
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            // <1.19.3 无 Parrot.Variant 枚举（getVariant 仍为 int，1192 Parrot.java:413；1.19.4 起才
            // Parrot.Variant + getVariantTexture，1194 Parrot.java:497）：贴图查
            // ParrotRenderer.PARROT_LOCATIONS[variant]（vanilla 1192 ParrotRenderer.java:23 同款索引，越界防护取模）
            //? if <1.19.3
            // this.parrotModel.renderOnShoulder(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.PARROT_LOCATIONS[Math.floorMod(shoulderEntityLeft.getInt(TAG_VARIANT), ParrotRenderer.PARROT_LOCATIONS.length)])), packedLightIn, OverlayTexture.NO_OVERLAY, limbSwing, limbSwingAmount, netHeadYaw, headPitch, player.tickCount);
            //? if >=1.17 && <1.19.3
            /*this.parrotModel.renderOnShoulder(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.PARROT_LOCATIONS[Math.floorMod(shoulderEntityLeft.getInt(TAG_VARIANT), ParrotRenderer.PARROT_LOCATIONS.length)])), packedLightIn, OverlayTexture.NO_OVERLAY, limbSwing, limbSwingAmount, netHeadYaw, headPitch, player.tickCount);*/
            //? if >=1.19.3 && <1.21.2
            this.parrotModel.renderOnShoulder(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(Parrot.Variant.byId(shoulderEntityLeft.getInt(TAG_VARIANT))))), packedLightIn, OverlayTexture.NO_OVERLAY, limbSwing, limbSwingAmount, netHeadYaw, headPitch, player.tickCount);
            // 1.21.2 ParrotModel.renderOnShoulder(PoseStack,VertexConsumer,...) 九参删除 →
            // ParrotOnShoulderLayer 同款状态制（ParrotRenderState.pose=ON_SHOULDER +
            // setupAnim + renderToBuffer，vanilla-1.21.3 ParrotOnShoulderLayer.java:50-68/
            // ParrotModel.java:83 实证）
            //? if >=21.2 && <21.5 {
            /*net.minecraft.client.renderer.entity.state.ParrotRenderState parrotState = new net.minecraft.client.renderer.entity.state.ParrotRenderState();
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            parrotState.ageInTicks = player.tickCount;
            parrotState.yRot = netHeadYaw;
            parrotState.xRot = headPitch;
            this.parrotModel.setupAnim(parrotState);
            this.parrotModel.renderToBuffer(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(Parrot.Variant.byId(shoulderEntityLeft.getInt(TAG_VARIANT))))), packedLightIn, OverlayTexture.NO_OVERLAY);*/
            //?}
            // 1.21.5 getInt 返回 Optional<Integer>（CompoundTag.java:325）→ getIntOr
            //? if >=21.5 {
            /*net.minecraft.client.renderer.entity.state.ParrotRenderState parrotState = new net.minecraft.client.renderer.entity.state.ParrotRenderState();
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            parrotState.ageInTicks = player.tickCount;
            parrotState.yRot = netHeadYaw;
            parrotState.xRot = headPitch;
            this.parrotModel.setupAnim(parrotState);
            this.parrotModel.renderToBuffer(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(Parrot.Variant.byId(shoulderEntityLeft.getIntOr(TAG_VARIANT, 0))))), packedLightIn, OverlayTexture.NO_OVERLAY);*/
            //?}

            poseStack.popPose();
        });
        //?}
        //? if >=21.9 && <26.2 {
        /*if (shoulderVariant != null) {
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            net.minecraft.client.renderer.entity.state.ParrotRenderState parrotState = new net.minecraft.client.renderer.entity.state.ParrotRenderState();
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            parrotState.ageInTicks = player.tickCount;
            parrotState.yRot = netHeadYaw;
            parrotState.xRot = headPitch;
            this.parrotModel.setupAnim(parrotState);
            this.parrotModel.renderToBuffer(poseStack, bufferSource.getBuffer(this.parrotModel.renderType(ParrotRenderer.getVariantTexture(shoulderVariant))), packedLightIn, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        */
        //?}
        // 26.2 getBuffer → submitCustomGeometry 逃生口（pose 提交期捕获，SubmitNodeCollection
        // .java:350 实证）；lambda 内按捕获位姿重建 PoseStack（mulPose(Matrix4fc) 刚体变换下
        // 法线重算=原值）供 Model.renderToBuffer 消费
        //? if >=26.2 {
        /*if (shoulderVariant != null) {
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            net.minecraft.client.renderer.entity.state.ParrotRenderState parrotState = new net.minecraft.client.renderer.entity.state.ParrotRenderState();
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            parrotState.ageInTicks = player.tickCount;
            parrotState.yRot = netHeadYaw;
            parrotState.xRot = headPitch;
            this.parrotModel.setupAnim(parrotState);
            bufferSource.submitCustomGeometry(poseStack, this.parrotModel.renderType(ParrotRenderer.getVariantTexture(shoulderVariant)), (pose, vc) -> {
                PoseStack inner = new PoseStack();
                inner.mulPose(pose.pose());
                this.parrotModel.renderToBuffer(inner, vc, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
            });
            poseStack.popPose();
        }
        */
        //?}
    }
    //?}

    //? if >=26.2 {
    /*@Override
    public void render(PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model == null) {
            return;
        }
        if (!model.leftShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!model.rightShoulderBones().isEmpty()) {
            renderParrot(poseStack, bufferSource, model, packedLightIn, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }

    private void renderParrot(PoseStack poseStack, SubmitNodeCollector bufferSource, AnimatedGeoModel model, int packedLightIn, Player player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean isLeftShoulder) {
        Parrot.Variant shoulderVariant =
            ((net.minecraft.client.player.AbstractClientPlayer) player).getParrotVariantOnShoulder(isLeftShoulder);
        if (shoulderVariant != null) {
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            net.minecraft.client.renderer.entity.state.ParrotRenderState parrotState = new net.minecraft.client.renderer.entity.state.ParrotRenderState();
            parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
            parrotState.ageInTicks = player.tickCount;
            parrotState.yRot = netHeadYaw;
            parrotState.xRot = headPitch;
            this.parrotModel.setupAnim(parrotState);
            bufferSource.submitCustomGeometry(poseStack, this.parrotModel.renderType(ParrotRenderer.getVariantTexture(shoulderVariant)), (pose, vc) -> {
                PoseStack inner = new PoseStack();
                inner.mulPose(pose.pose());
                this.parrotModel.renderToBuffer(inner, vc, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
            });
            poseStack.popPose();
        }
    }
    *///?}

    public void applyParrotTransform(PoseStack poseStack, AnimatedGeoModel model, boolean isLeftShoulder) {
        if (isLeftShoulder) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftShoulderBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, model.rightShoulderBones());
        }
    }
}