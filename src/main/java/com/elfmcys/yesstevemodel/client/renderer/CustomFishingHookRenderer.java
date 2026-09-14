package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import rip.ysm.compat.oculus.OculusCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.MultiBufferSource;
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import rip.ysm.api.item.ToolActionBridge;
import org.spongepowered.asm.mixin.Unique;

public class CustomFishingHookRenderer {
    public static boolean tryRenderCustomHook(FishingHook fishingHook, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return ProjectileCapability.get(fishingHook).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                // 1.16.5 xRot 为公共字段（setXRot 为 1.17+）
                //? if <1.17
                // fishingHook.xRot = 0.0f;
                //? if >=1.17
                fishingHook.setXRot(0.0f);
                fishingHook.xRotO = 0.0f;
                RendererManager.getProjectileRenderer().render(cap, entityYaw, partialTick, poseStack, bufferSource, packedLight);
                Player playerOwner = fishingHook.getPlayerOwner();
                if (playerOwner != null) {
                    poseStack.pushPose();
                    renderFishingLine(fishingHook, partialTick, poseStack, bufferSource, playerOwner);
                    poseStack.popPose();
                }
                return false;
            }
            return true;
        }).orElse(true);
    }

    private static void renderFishingLine(FishingHook fishingHook, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, Player player) {
        int hand = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!ToolActionBridge.canFishingRodCast(player.getMainHandItem())) {
            hand = -hand;
        }
        float swingProgressSqrt = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * 3.1415927f);
        float yawOffset = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * 0.017453292f;
        double dSin = Mth.sin(yawOffset);
        double dCos = Mth.cos(yawOffset);
        double handOffset = hand * 0.35d;
        double anglerX;
        double anglerY;
        double anglerZ;
        float anglerEye;
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Options options = entityRenderDispatcher.options;
        // 1.16.1 无 Options.getCameraType（CameraUtil <1.16.2 反射分支承接）
        //? if <1.16.2
        /*if (options == null || !com.elfmcys.yesstevemodel.util.CameraUtil.isFirstPersonView() || player != Minecraft.getInstance().player) {*/
        //? if >=1.16.2
        if (options == null || !options.getCameraType().isFirstPerson() || player != Minecraft.getInstance().player) {
            anglerX = (Mth.lerp(partialTick, player.xo, player.getX()) - (dCos * handOffset)) - (dSin * 0.8d);
            anglerY = ((player.yo + player.getEyeHeight()) + ((player.getY() - player.yo) * partialTick)) - 0.45d;
            anglerZ = (Mth.lerp(partialTick, player.zo, player.getZ()) - (dSin * handOffset)) + (dCos * 0.8d);
            anglerEye = player.isCrouching() ? -0.1875f : 0.0f;
        } else {
            // 1.16.5 无 NearPlane（1.18+）/OptionInstance.fov()（1.17+）：
            // vanilla-1.16.5 FishingHookRenderer 同场景手算（options.fov int /100 缩放 + 手偏移向量按视角旋转）
            //? if <1.17 {
            // double fovScale = options.fov / 100.0d;
            // Vec3 vec3XRot = new Vec3(hand * -0.36d * fovScale, -0.045d * fovScale, 0.4d)
            //         .xRot(-Mth.lerp(partialTick, player.xRotO, player.xRot) * 0.017453292f)
            //         .yRot(-Mth.lerp(partialTick, player.yRotO, player.yRot) * 0.017453292f)
            //         .yRot(swingProgressSqrt * 0.5f)
            //         .xRot(-swingProgressSqrt * 0.7f);
            //? } else {
            // options.fov() 访问器 1.19.0 起（f119 Options.java:739 实证）；1.18.x 为 public double fov 字段
            //? if >=1.19
            Vec3 vec3XRot = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(hand * 0.525f, -0.1f).scale(960.0d / options.fov().get().intValue()).yRot(swingProgressSqrt * 0.5f).xRot((-swingProgressSqrt) * 0.7f);
            //? if <1.19
            /*Vec3 vec3XRot = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(hand * 0.525f, -0.1f).scale(960.0d / options.fov).yRot(swingProgressSqrt * 0.5f).xRot((-swingProgressSqrt) * 0.7f);*/
            //? }
            anglerX = Mth.lerp(partialTick, player.xo, player.getX()) + vec3XRot.x;
            anglerY = Mth.lerp(partialTick, player.yo, player.getY()) + vec3XRot.y;
            anglerZ = Mth.lerp(partialTick, player.zo, player.getZ()) + vec3XRot.z;
            anglerEye = player.getEyeHeight();
        }
        float startX = (float) (anglerX - Mth.lerp(partialTick, fishingHook.xo, fishingHook.getX()));
        float startY = ((float) (anglerY - (Mth.lerp(partialTick, fishingHook.yo, fishingHook.getY()) + 0.25d))) + anglerEye;
        float startZ = (float) (anglerZ - Mth.lerp(partialTick, fishingHook.zo, fishingHook.getZ()));
        float[] color = lineColor(fishingHook);
        PoseStack.Pose poseLast = poseStack.last();
        // BLOCKER-2：1.16.5 无 RenderType.lineStrip()（1.17+）；vanilla-1.16.5 FishingHookRenderer
        // :89/:93-110 用 lines()（POSITION_COLOR，GL_LINES）且每段成对提交 2 顶点——顶点组织
        // 不能沿用 1.20.1 lineStrip 的单顶点循环，否则 GL_LINES 下鱼线隔段断裂。
        //? if <1.17 {
        // VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        // for (int size = 0; size < 16; size++) {
        //     // 每段 2 顶点：段起点 + 段终点（vanilla-1165 同构：stringVertex(f(i)) + stringVertex(f(i+1))）
        //     stringVertex(startX, startY, startZ, buffer, poseLast, fraction(size), fraction(size + 1), color[0], color[1], color[2]);
        //     stringVertex(startX, startY, startZ, buffer, poseLast, fraction(size + 1), fraction(size + 1), color[0], color[1], color[2]);
        // }
        //? } else {
        // 1.21.11 RenderType.lineStrip 删（2111 rendertype 全域零命中，线渲染走 RenderPipelines
        // 换代）→ 鱼线绘制 21.11+ 降级 no-op（功能债入账）
        //? if <21.11 {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lineStrip());
        for (int size = 0; size <= 16; size++) {
            stringVertex(startX, startY, startZ, buffer, poseLast, fraction(size), fraction(size + 1), color[0], color[1], color[2]);
        }
        //?}
        if (OculusCompat.isLoaded()) {
            //? if >=1.21 && <21.11
            /*buffer.addVertex(0.0f, 0.0f, 0.0f).setColor(0, 0, 0, 255).setNormal(0.0f, 0.0f, 0.0f);*/
            //? if <1.21
            buffer.vertex(0.0d, 0.0d, 0.0d).color(0, 0, 0, 255).normal(0.0f, 0.0f, 0.0f).endVertex();
        }
        //? }
    }

    @Unique
    private static float[] lineColor(FishingHook fishingHook) {
        return new float[]{0.0f, 0.0f, 0.0f};
    }

    @Unique
    private static float fraction(int i) {
        return i / 16.0f;
    }

    @Unique
    private static void stringVertex(float x, float y, float z, VertexConsumer vertexConsumer, PoseStack.Pose pose, float startFrac, float endFrac, float red, float green, float blue) {
        float vx = x * startFrac;
        float vy = (y * ((startFrac * startFrac) + startFrac) * 0.5f) + 0.25f;
        float vz = z * startFrac;
        // 1.16.5 lines()=POSITION_COLOR（无 NORMAL 元素；vanilla-1165 stringVertex 仅 pos+color，
        // 线段方向由 GL_LINES 两顶点拓扑承载），endFrac 仅 1.20.1 分支用于 normal——
        // 在 1.16.5 调 .normal() 会顶点错位，必须只发 pos+color。
        //? if <1.17 {
        // vertexConsumer.vertex(pose.pose(), vx, vy, vz).color(red, green, blue, 1.0f).endVertex();
        //? } else {
        float dx = (x * endFrac) - vx;
        float dy = (((y * ((endFrac * endFrac) + endFrac)) * 0.5f) + 0.25f) - vy;
        float dz = (z * endFrac) - vz;
        float length = Mth.sqrt((dx * dx) + (dy * dy) + (dz * dz));
        // 1.20.5+ 删 normal(Matrix3f,...)（vanilla-1.20.6 VertexConsumer.java:133 仅余 Pose 重载）；
        // 1.21 起 vertex/color/normal 改名 addVertex/setColor/setNormal 且无 endVertex
        //（vanilla-1.21.1 VertexConsumer.java:16-49）
        //? if >=1.21
        /*vertexConsumer.addVertex(pose.pose(), vx, vy, vz).setColor(red, green, blue, 1.0f).setNormal(pose, dx / length, dy / length, dz / length);*/
        //? if >=1.17 && <1.20.5
        vertexConsumer.vertex(pose.pose(), vx, vy, vz).color(red, green, blue, 1.0f).normal(pose.normal(), dx / length, dy / length, dz / length).endVertex();
        //? if >=1.20.5 && <1.21
        /*vertexConsumer.vertex(pose.pose(), vx, vy, vz).color(red, green, blue, 1.0f).normal(pose, dx / length, dy / length, dz / length).endVertex();*/
        //? }
    }
}
