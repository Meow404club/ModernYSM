package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ElytraModel;
// 1.16.5 无 ModelLayers/EntityRendererProvider，ElytraModel 为无参传统构造
//? if >=1.17 {
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
//? if >=1.19.4 {
import com.mojang.math.Axis;
//? }

public class CustomPlayerElytraLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    //? if >=1.21
    /*private static final ResourceLocation WINGS_LOCATION = ResourceLocation.parse("textures/entity/elytra.png");*/
    //? if <1.21
    private static final ResourceLocation WINGS_LOCATION = new ResourceLocation("textures/entity/elytra.png");

    private final ElytraModel<LivingEntity> elytraModel;

    //? if <1.17 {
    // public CustomPlayerElytraLayer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //     this.elytraModel = new ElytraModel<>();
    // }
    //? } else {
    public CustomPlayerElytraLayer(EntityRendererProvider.Context context) {
        this.elytraModel = new ElytraModel<>(context.getModelSet().bakeLayer(ModelLayers.ELYTRA));
    }
    //? }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation cloakTextureLocation;
        LivingEntity entity = entityLivingBaseIn.getEntity();
        ItemStack stack = CosmeticArmorHelper.getElytraItem(entity);
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (!stack.isEmpty() && animatedGeoModel != null && !animatedGeoModel.elytraBones().isEmpty() && (entity instanceof AbstractClientPlayer)) {
            AbstractClientPlayer abstractClientPlayer = (AbstractClientPlayer) entity;
            //? if neoforge
            /*if (abstractClientPlayer.getSkin().elytraTexture() != null) {*/
            //? if forge
            if (abstractClientPlayer.isElytraLoaded() && abstractClientPlayer.getElytraTextureLocation() != null) {
                //? if neoforge
                /*cloakTextureLocation = abstractClientPlayer.getSkin().elytraTexture();*/
                //? if forge
                cloakTextureLocation = abstractClientPlayer.getElytraTextureLocation();
            //? if neoforge
            /*} else if (abstractClientPlayer.getSkin().capeTexture() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {*/
            //? if forge
            } else if (abstractClientPlayer.isCapeLoaded() && abstractClientPlayer.getCloakTextureLocation() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {
                //? if neoforge
                /*cloakTextureLocation = abstractClientPlayer.getSkin().capeTexture();*/
                //? if forge
                cloakTextureLocation = abstractClientPlayer.getCloakTextureLocation();
            } else {
                cloakTextureLocation = WINGS_LOCATION;
            }
            poseStack.pushPose();
            renderElytra(poseStack, animatedGeoModel);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            //? if <1.17
            // poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f));
            // 1.19.3 起 Axis 枚举替换 Vector3f 常量旋转（1194 Axis.java；1192 sources 无此类）
            //? if >=1.17 && <1.19.4
            /*poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f));*/
            //? if >=1.19.4
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            poseStack.scale(2.0f, 2.0f, 2.0f);
            this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            // 1.21 getArmorFoilBuffer 去 hasFoil 尾参（vanilla-1.21.1 ItemRenderer.java:167）、
            // renderToBuffer 颜色改 int 打包（vanilla-1.21.1 Model.java:23）
            //? if >=1.21
            /*this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, -1);*/
            //? if <1.21
            this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        }
    }

    public void renderElytra(PoseStack poseStack, AnimatedGeoModel model) {
        RenderUtils.prepMatrixForLocator(poseStack, model.elytraBones());
    }
}