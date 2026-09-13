package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
// 1.21.11 ElytraModel 移 client.model.object.equipment 子包
//? if >=21.11
/*import net.minecraft.client.model.object.equipment.ElytraModel;*/
//? if <21.11
import net.minecraft.client.model.ElytraModel;
// 1.16.5 无 ModelLayers/EntityRendererProvider，ElytraModel 为无参传统构造
//? if >=1.17 {
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
 *///?}
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
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

    // 1.21.2 ElytraModel 去泛型（EntityModel<HumanoidRenderState>，setupAnim 改状态制）
    //? if <1.21.2
    private final ElytraModel<LivingEntity> elytraModel;
    //? if >=1.21.2 {
    /*private final ElytraModel elytraModel;
    */
    //?}

    //? if <1.17 {
    // public CustomPlayerElytraLayer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //     this.elytraModel = new ElytraModel<>();
    // }
    //? } else {
    //? if <1.21.2 {
    public CustomPlayerElytraLayer(EntityRendererProvider.Context context) {
        this.elytraModel = new ElytraModel<>(context.getModelSet().bakeLayer(ModelLayers.ELYTRA));
    }
    //?}
    //? if >=1.21.2 {
    /*public CustomPlayerElytraLayer(EntityRendererProvider.Context context) {
        this.elytraModel = new ElytraModel(context.getModelSet().bakeLayer(ModelLayers.ELYTRA));
    }*/
    //?}
    //? }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation cloakTextureLocation;
        LivingEntity entity = entityLivingBaseIn.getEntity();
        ItemStack stack = CosmeticArmorHelper.getElytraItem(entity);
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (!stack.isEmpty() && animatedGeoModel != null && !animatedGeoModel.elytraBones().isEmpty() && (entity instanceof AbstractClientPlayer)) {
            AbstractClientPlayer abstractClientPlayer = (AbstractClientPlayer) entity;
            // 1.21.9 PlayerSkin 重组：elytraTexture()/capeTexture() 删 → elytra()/cape()
            //（ClientAsset.Texture 可空）+ texturePath()（neoforge-21.10.64 PlayerSkin.java:15 实证）
            //? if neoforge && >=21.9
            /*if (abstractClientPlayer.getSkin().elytra() != null) {*/
            //? if neoforge && <21.9
            /*if (abstractClientPlayer.getSkin().elytraTexture() != null) {*/
            //? if forge
            if (abstractClientPlayer.isElytraLoaded() && abstractClientPlayer.getElytraTextureLocation() != null) {
                //? if neoforge && >=21.9
                /*cloakTextureLocation = abstractClientPlayer.getSkin().elytra().texturePath();*/
                //? if neoforge && <21.9
                /*cloakTextureLocation = abstractClientPlayer.getSkin().elytraTexture();*/
                //? if forge
                cloakTextureLocation = abstractClientPlayer.getElytraTextureLocation();
            //? if neoforge && >=21.9
            /*} else if (abstractClientPlayer.getSkin().cape() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {*/
            //? if neoforge && <21.9
            /*} else if (abstractClientPlayer.getSkin().capeTexture() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {*/
            //? if forge
            } else if (abstractClientPlayer.isCapeLoaded() && abstractClientPlayer.getCloakTextureLocation() != null && abstractClientPlayer.isModelPartShown(PlayerModelPart.CAPE)) {
                //? if neoforge && >=21.9
                /*cloakTextureLocation = abstractClientPlayer.getSkin().cape().texturePath();*/
                //? if neoforge && <21.9
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
            // 1.21.2 setupAnim(HumanoidRenderState) 状态制：elytra 旋转由
            // ElytraAnimationState.getRotX/Y/Z(partialTick) 抽取（vanilla-1.21.3
            // PlayerRenderer.java:62-64/ElytraModel.java:44 实证）
            //? if <1.21.2
            this.elytraModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            //? if >=1.21.2 {
            /*HumanoidRenderState elytraState = new HumanoidRenderState();
            elytraState.isCrouching = entity.isCrouching();
            elytraState.elytraRotX = entity.elytraAnimationState.getRotX(partialTick);
            elytraState.elytraRotY = entity.elytraAnimationState.getRotY(partialTick);
            elytraState.elytraRotZ = entity.elytraAnimationState.getRotZ(partialTick);
            this.elytraModel.setupAnim(elytraState);*/
            //?}
            // 1.21 getArmorFoilBuffer 去 hasFoil 尾参（vanilla-1.21.1 ItemRenderer.java:167）、
            // renderToBuffer 颜色改 int 打包（vanilla-1.21.1 Model.java:23）
            //? if >=1.21 && <21.9
            /*this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, -1);*/
            //? if <1.21
            // 1.21.9 getArmorFoilBuffer → getFoilBuffer（ItemRenderer.java:67 四参，语义同形）
            //? if >=21.9
            /*this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, -1);*/
            //? if <1.21
            this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        }
    }

    public void renderElytra(PoseStack poseStack, AnimatedGeoModel model) {
        RenderUtils.prepMatrixForLocator(poseStack, model.elytraBones());
    }
}