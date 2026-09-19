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
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
//? if <26 {
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}
// 26.x：ItemRenderer 类删；getFoilBuffer 同签名迁 ItemFeatureRenderer
//? if >=26 {
/*import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
 *///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
//? if >=1.19.3 {
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

    //? if <26.2 {
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
    //?}
    //? if >=26.2 {
    /*@Override
    public void render(PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {*/
    //?}
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
            //? if >=1.17 && <1.19.3
            /*poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f));*/
            //? if >=1.19.3
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
            // 1.21.9 getArmorFoilBuffer → getFoilBuffer（ItemRenderer.java:67 四参，语义同形）
            //? if >=21.9 && <26
            /*this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, -1);*/
            // 26.1：getFoilBuffer 同签名迁 ItemFeatureRenderer（static）
            //? if >=26 && <26.2
            /*this.elytraModel.renderToBuffer(poseStack, ItemFeatureRenderer.getFoilBuffer(bufferSource, net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, -1);*/
            // 26.2：getFoilBuffer 私有实例化不可达 → submitCustomGeometry 逃生口直绘
            //（pose 提交期捕获+lambda 内重建 PoseStack）。光膜对齐 vanilla-26.2 双提交机制
            //（WingsLayer→EquipmentLayerRenderer.renderLayers：base armorCutoutNoCull 后
            // hasFoil() 再提交 armorEntityGlint 同几何，EquipmentLayerRenderer.java renderFoil
            // 段实证；GLINT 管线 depth CompareOp.EQUAL 贴 base 深度、hasBlending→translucent 相，
            // solid→translucent 固定相序即 vanilla order+1 语义，SubmitNodeCollection.java:349-357）
            //? if >=26.2 && <26.3
            /*bufferSource.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(cloakTextureLocation), (pose, vc) -> {
                PoseStack inner = new PoseStack();
                inner.mulPose(pose.pose());
                this.elytraModel.renderToBuffer(inner, vc, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
            });
            if (stack.hasFoil()) {
                bufferSource.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.armorEntityGlint(), (pose, vc) -> {
                    PoseStack inner = new PoseStack();
                    inner.mulPose(pose.pose());
                    this.elytraModel.renderToBuffer(inner, vc, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
                });
            }*/
            // 26.3：光膜并入单管线——hasFoil 时 base render type 直接换
            // armorCutoutNoCullGlint（=ENTITY+GLINT 双 snippet 单次提交，Sampler0=层贴图、
            // GlintSampler=ENCHANTED_GLINT_ARMOR、TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING，
            // EquipmentLayerRenderer.java:86-90 renderShaderGlint 三元实证），lambda 零变
            //? if >=26.3
            /*bufferSource.submitCustomGeometry(poseStack, stack.hasFoil() ? net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCullGlint(cloakTextureLocation) : net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(cloakTextureLocation), (pose, vc) -> {
                PoseStack inner = new PoseStack();
                inner.mulPose(pose.pose());
                this.elytraModel.renderToBuffer(inner, vc, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
            });*/
            //? if <1.21
            this.elytraModel.renderToBuffer(poseStack, ItemRenderer.getArmorFoilBuffer(bufferSource, RenderType.armorCutoutNoCull(cloakTextureLocation), false, stack.hasFoil()), packedLightIn, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        }
    }

    public void renderElytra(PoseStack poseStack, AnimatedGeoModel model) {
        RenderUtils.prepMatrixForLocator(poseStack, model.elytraBones());
    }
}