package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin（>=21.9 vanilla 位渲染功能债，仅类型换代）
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class VehicleRenderer extends GeoEntityRenderer<Entity, GeckoVehicleEntity> {
    //? if <1.17 {
    // public VehicleRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //? } else {
    public VehicleRenderer(EntityRendererProvider.Context context) {
    //? }
        super(context);
    }

    //? if <26.2 {
    public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || entity.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        VehicleCapability.get(entity).ifPresent(cap -> {
            cap.tickModel();
            renderEntity(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }
    //?}

    //? if >=26.2 {
    /*public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || entity.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        VehicleCapability.get(entity).ifPresent(cap -> {
            cap.tickModel();
            renderEntity(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }*/
    //?}

    @NotNull
    public ResourceLocation getTextureLocation(Entity entity) {
        return VehicleCapability.get(entity).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
