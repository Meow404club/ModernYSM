package com.elfmcys.yesstevemodel.client.renderer.layer;

import rip.ysm.compat.slashblade.SlashBladeRenderer;
import rip.ysm.compat.slashblade.SlashBladeCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import rip.ysm.compat.gun.tacz.TacCompat;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.elfmcys.yesstevemodel.util.accessors.BufferSourceAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
//? if <1.19.4 {
import net.minecraft.client.renderer.block.model.ItemTransforms;
//?}
//? if >=1.19.4 {
import net.minecraft.world.item.ItemDisplayContext;
//?}
import net.minecraft.world.item.ItemStack;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//? }

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemInHandRenderer itemRenderer;

    public CustomPlayerItemInHandLayer(ItemInHandRenderer itemInHandRenderer) {
        this.itemRenderer = itemInHandRenderer;
    }

    //? if <26.2 {
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = entityLivingBaseIn.getEntity();
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (animatedGeoModel == null) {
            return;
        }
        ItemStack offhandItem = entity.getOffhandItem();
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            boolean useExtraPlayer = entityLivingBaseIn.isRenderLayersFirst();
            if (!animatedGeoModel.rightHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
                    SlashBladeRenderer.renderOnEntity(entity, animatedGeoModel, poseStack, bufferSource, packedLightIn, mainHandItem, partialTick);
                } else {
                    TacCompat.handleGunSound(entity, mainHandItem);
                    //? if <1.19.4
                    // renderItem(animatedGeoModel, entity, mainHandItem, ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLightIn);
                    //? if >=1.19.4
                    renderItem(animatedGeoModel, entity, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLightIn);
                    if (useExtraPlayer && !mainHandItem.isEmpty() && (bufferSource instanceof BufferSourceAccessor)) {
                        ((BufferSourceAccessor) bufferSource).initialize();
                    }
                    TacCompat.handleItemSound(mainHandItem);
                }
            }
            if (!animatedGeoModel.leftHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(offhandItem)) {
                    SlashBladeRenderer.renderRightWaist(animatedGeoModel, poseStack, bufferSource, packedLightIn, offhandItem);
                } else {
                    if (!SWarfareCompat.isGunItem(offhandItem)) {
                        //? if <1.19.4
                        // renderItem(animatedGeoModel, entity, offhandItem, ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLightIn);
                        //? if >=1.19.4
                        renderItem(animatedGeoModel, entity, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLightIn);
                    }
                    if (useExtraPlayer && !offhandItem.isEmpty() && (bufferSource instanceof BufferSourceAccessor)) {
                        ((BufferSourceAccessor) bufferSource).initialize();
                    }
                }
            }
            poseStack.popPose();
            TacCompat.applyItemTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
            SWarfareCompat.applyGunTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
        }
    }
    //?}

    //? if >=26.2 {
    /*@Override
    public void render(PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = entityLivingBaseIn.getEntity();
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (animatedGeoModel == null) {
            return;
        }
        ItemStack offhandItem = entity.getOffhandItem();
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            if (!animatedGeoModel.rightHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
                    SlashBladeRenderer.renderOnEntity(entity, animatedGeoModel, poseStack, bufferSource, packedLightIn, mainHandItem, partialTick);
                } else {
                    TacCompat.handleGunSound(entity, mainHandItem);
                    renderItem(animatedGeoModel, entity, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLightIn);
                    TacCompat.handleItemSound(mainHandItem);
                }
            }
            if (!animatedGeoModel.leftHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(offhandItem)) {
                    SlashBladeRenderer.renderRightWaist(animatedGeoModel, poseStack, bufferSource, packedLightIn, offhandItem);
                } else if (!SWarfareCompat.isGunItem(offhandItem)) {
                    renderItem(animatedGeoModel, entity, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLightIn);
                }
            }
            poseStack.popPose();
            TacCompat.applyItemTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
            SWarfareCompat.applyGunTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
        }
    }*/
    //?}

    //? if <1.19.4
    // public void renderItem(AnimatedGeoModel model, LivingEntity livingEntity, ItemStack itemStack, ItemTransforms.TransformType itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
    //? if >=1.19.4 && <26.2 {
    public void renderItem(AnimatedGeoModel model, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        if (!itemStack.isEmpty()) {
            boolean isLeftHand = humanoidArm == HumanoidArm.LEFT;
            poseStack.pushPose();
            if (!applyItemBoneTransform(humanoidArm, poseStack, model)) {
                poseStack.translate(0.0d, -0.0625d, -0.1d);
                //? if <1.19.3
                // poseStack.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(-90.0f));
                //? if >=1.19.3
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                if (SWarfareCompat.isGunItem(itemStack)) {
                    poseStack.translate(0.1d, 0.0d, 0.0d);
                    poseStack.scale(1.25f, 1.25f, 1.25f);
                }
                //? if <21.5
                this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, isLeftHand, poseStack, multiBufferSource, i);
                // 1.21.5 renderItem 删 isLeftHand 参（ItemInHandRenderer.java:132）
                //? if >=21.5 && <21.9
                /*this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, poseStack, multiBufferSource, i);*/
                // 1.21.9 renderItem 尾参 MultiBufferSource → SubmitNodeCollector（render-dag，
                // 2110 ItemInHandRenderer.java:134 实证）→ Static 直绘无 collector，
                // 骨骼绑定手持原版物品 21.9+ 不渲染，功能债入账
                //? if >=21.9 {
                /*// no-op
                 *///?}
            }
            poseStack.popPose();
            (isLeftHand ? model.rightHandChain() : model.leftHandChains()).forEach(list -> {
                poseStack.pushPose();
                if (!RenderUtils.prepMatrixForLocator(poseStack, list)) {
                    poseStack.translate(0.0d, -0.0625d, -0.1d);
                    //? if <1.19.3
                    // poseStack.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(-90.0f));
                    //? if >=1.19.3
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                    if (SWarfareCompat.isGunItem(itemStack)) {
                        poseStack.scale(1.25f, 1.25f, 1.25f);
                    }
                    //? if <21.5
                this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, isLeftHand, poseStack, multiBufferSource, i);
                // 1.21.5 renderItem 删 isLeftHand 参（ItemInHandRenderer.java:132）
                //? if >=21.5 && <21.9
                /*this.itemRenderer.renderItem(livingEntity, itemStack, itemDisplayContext, poseStack, multiBufferSource, i);*/
                // 1.21.9 renderItem 尾参 MultiBufferSource → SubmitNodeCollector（render-dag，
                // 2110 ItemInHandRenderer.java:134 实证）→ Static 直绘无 collector，
                // 骨骼绑定手持原版物品 21.9+ 不渲染，功能债入账
                //? if >=21.9 {
                /*// no-op
                 *///?}
                }
                poseStack.popPose();
            });
        }
    }
    //?}

    // 26.2 collector 形 twin：vanilla 手持渲染 21.9+ 已功能债 no-op，本 twin 仅类型换代保编译面
    //? if >=26.2 {
    /*public void renderItem(AnimatedGeoModel model, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, SubmitNodeCollector multiBufferSource, int i) {
        if (!itemStack.isEmpty()) {
            boolean isLeftHand = humanoidArm == HumanoidArm.LEFT;
            poseStack.pushPose();
            if (!applyItemBoneTransform(humanoidArm, poseStack, model)) {
                poseStack.translate(0.0d, -0.0625d, -0.1d);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                if (SWarfareCompat.isGunItem(itemStack)) {
                    poseStack.translate(0.1d, 0.0d, 0.0d);
                    poseStack.scale(1.25f, 1.25f, 1.25f);
                }
            }
            poseStack.popPose();
            (isLeftHand ? model.rightHandChain() : model.leftHandChains()).forEach(list -> {
                poseStack.pushPose();
                if (!RenderUtils.prepMatrixForLocator(poseStack, list)) {
                    poseStack.translate(0.0d, -0.0625d, -0.1d);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                    if (SWarfareCompat.isGunItem(itemStack)) {
                        poseStack.scale(1.25f, 1.25f, 1.25f);
                    }
                }
                poseStack.popPose();
            });
        }
    }*/
    //?}

    public boolean applyItemBoneTransform(HumanoidArm humanoidArm, PoseStack poseStack, AnimatedGeoModel model) {
        if (humanoidArm == HumanoidArm.LEFT) {
            return RenderUtils.prepMatrixForLocator(poseStack, model.leftHandBones());
        }
        return RenderUtils.prepMatrixForLocator(poseStack, model.rightHandBones());
    }
}