package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.simplehats.SimpleHatsHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
//? if <1.19.4 {
import net.minecraft.client.renderer.block.model.ItemTransforms;
//?}
//? if >=1.19.4 {
import net.minecraft.world.item.ItemDisplayContext;
//?}
import net.minecraft.world.item.ItemStack;

public class CustomPlayerArmorLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemInHandRenderer itemRenderer;

    //? if <1.17 {
    // public CustomPlayerArmorLayer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //     this.itemRenderer = new ItemInHandRenderer(net.minecraft.client.Minecraft.getInstance());
    // }
    //? } else {
    public CustomPlayerArmorLayer(EntityRendererProvider.Context context) {
        //? if <1.19.2
        /*this.itemRenderer = new ItemInHandRenderer(net.minecraft.client.Minecraft.getInstance());*/
        //? if >=1.19.2
        this.itemRenderer = context.getItemInHandRenderer();
    }
    //? }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model != null && !model.headBones().isEmpty()) {
            ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemBySlot.isEmpty() && !isArmorItem(itemBySlot)) {
                renderArmorPiece(poseStack, bufferSource, packedLightIn, model, player, itemBySlot);
            }
            ItemStack stack = SimpleHatsHelper.getHatItem(player);
            if (stack != null && !stack.isEmpty()) {
                renderArmorPiece(poseStack, bufferSource, packedLightIn, model, player, stack);
            }
        }
    }

    private boolean isArmorItem(ItemStack stack) {
        Item item = stack.getItem();
        // ArmorItem 取槽位：1.16.5~1.19.2 getSlot()（1165/1182:91/1192:91）；1.19.4 起
        // getEquipmentSlot()（1194 ArmorItem.java:128）
        //? if <1.17
        // return (item instanceof ArmorItem) && ((ArmorItem) item).getSlot() == EquipmentSlot.HEAD;
        //? if >=1.17 && <1.19.4
        /*return (item instanceof ArmorItem) && ((ArmorItem) item).getSlot() == EquipmentSlot.HEAD;*/
        //? if >=1.19.4
        return (item instanceof ArmorItem) && ((ArmorItem) item).getEquipmentSlot() == EquipmentSlot.HEAD;
    }

    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, int i, AnimatedGeoModel model, Player player, ItemStack stack) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, model.headBones());
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.translate(0.0f, 0.25f, 0.0f);
        //? if <1.19.4
        // this.itemRenderer.renderItem(player, stack, ItemTransforms.TransformType.HEAD, false, poseStack, bufferSource, i);
        //? if >=1.19.4
        this.itemRenderer.renderItem(player, stack, ItemDisplayContext.HEAD, false, poseStack, bufferSource, i);
        poseStack.popPose();
    }
}