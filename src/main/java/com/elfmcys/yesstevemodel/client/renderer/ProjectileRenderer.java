package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.client.entity.GeckoProjectileEntity;
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
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;

public class ProjectileRenderer extends AbstractProjectileRenderer<Projectile, GeckoProjectileEntity> {
    //? if <1.17 {
    // public ProjectileRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //? } else {
    public ProjectileRenderer(EntityRendererProvider.Context context) {
    //? }
        super(context);
    }

    //? if <26.2 {
    public void render(Projectile projectile, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || projectile.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        ProjectileCapability.get(projectile).ifPresent(cap -> {
            cap.tickModel();
            render(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }
    //?}

    //? if >=26.2 {
    /*public void render(Projectile projectile, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        if (Minecraft.getInstance().player == null || projectile.isInvisibleTo(Minecraft.getInstance().player)) {
            return;
        }
        ProjectileCapability.get(projectile).ifPresent(cap -> {
            cap.tickModel();
            render(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        });
    }*/
    //?}

    @NotNull
    public ResourceLocation getTextureLocation(Projectile projectile) {
        return ProjectileCapability.get(projectile).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
