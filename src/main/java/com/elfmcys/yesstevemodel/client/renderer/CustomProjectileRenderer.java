package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin（>=21.9 vanilla 位渲染功能债，仅类型换代）
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.world.entity.projectile.Projectile;

public class CustomProjectileRenderer {
    //? if <26.2 {
    public static boolean renderProjectile(Projectile projectile, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        return ProjectileCapability.get(projectile).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                RendererManager.getProjectileRenderer().render(cap, entityYaw, partialTick, poseStack, multiBufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }
    //?}

    //? if >=26.2 {
    /*public static boolean renderProjectile(Projectile projectile, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector multiBufferSource, int packedLight) {
        return ProjectileCapability.get(projectile).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                RendererManager.getProjectileRenderer().render(cap, entityYaw, partialTick, poseStack, multiBufferSource, packedLight);
                return false;
            }
            return true;
        }).orElse(true);
    }*/
    //?}
}
