package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.CustomFishingHookRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomVehicleRenderer;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomProjectileRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
 *///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
/*import com.llamalad7.mixinextras.sugar.Local;
 *///?}

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
    //? if <1.21.2 {
    @WrapWithCondition(method = {"render"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")})
    private boolean render(EntityRenderer<?> renderer, Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        if (!YesSteveModel.isAvailable()) {
            return true;
        }
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (!GeneralConfig.DISABLE_PROJECTILE_MODEL.get()) {
                if (projectile instanceof FishingHook) {
                    FishingHook fishingHook = (FishingHook) projectile;
                    return CustomFishingHookRenderer.tryRenderCustomHook(fishingHook, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
                }
                return CustomProjectileRenderer.renderProjectile(projectile, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
            }
        }
        if (!GeneralConfig.DISABLE_VEHICLE_MODEL.get().booleanValue()) {
            ModelPreviewRenderer.renderVehicleModel(entity, poseStack, partialTicks);
            return CustomVehicleRenderer.renderVehicle(entity, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
        }
        return true;
    }
    //?}

    // 1.21.2 render-state 化：INVOKE 目标改 renderer.render(EntityRenderState,...)，实体上下文
    // 经 MixinExtras @Local 从私有重载 render(E,DDD,F,PoseStack,MultiBufferSource,I,EntityRenderer)
    // 参数捕获（entity/float 各唯一；yaw 从 state 化前的实体插值自算）。
    // 1.21.9+ EntityRenderer.render(state,...) 删（render-dag/SubmitNodeCollector 换代）→ 本
    // wrap 目标不存在，21.9+ 载具/抛射物原版位渲染不挂载（submit 移植=功能债，M4 级）
    //? if >=1.21.2 && <21.9 {
    /*@WrapWithCondition(method = {"render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")})
    private boolean render(@Local(argsOnly = true) Entity entity, @Local(argsOnly = true) float partialTicks, EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        if (!YesSteveModel.isAvailable()) {
            return true;
        }
        float entityYaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (!GeneralConfig.DISABLE_PROJECTILE_MODEL.get()) {
                if (projectile instanceof FishingHook) {
                    FishingHook fishingHook = (FishingHook) projectile;
                    return CustomFishingHookRenderer.tryRenderCustomHook(fishingHook, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
                }
                return CustomProjectileRenderer.renderProjectile(projectile, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
            }
        }
        if (!GeneralConfig.DISABLE_VEHICLE_MODEL.get().booleanValue()) {
            ModelPreviewRenderer.renderVehicleModel(entity, poseStack, partialTicks);
            return CustomVehicleRenderer.renderVehicle(entity, entityYaw, partialTicks, poseStack, multiBufferSource, packedLight);
        }
        return true;
    }*/
    //?}
}