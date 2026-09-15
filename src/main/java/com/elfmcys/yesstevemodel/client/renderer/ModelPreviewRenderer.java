package com.elfmcys.yesstevemodel.client.renderer;

import rip.ysm.util.RenderCompat;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.oculus.OculusCompat;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import com.elfmcys.yesstevemodel.client.animation.AnimationTracker;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.util.AnimatableCacheUtil;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if >=1.20 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
//? }
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;
//? if >=1.20.5 {
/*import org.joml.Matrix4fStack;*/
//? }

import java.util.List;
import java.util.concurrent.ExecutionException;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//? }
// Axis（1.19.3+）在 1.16.5 以 com.mojang.math.Vector3f/Quaternion 轴角构造等价表达

public final class ModelPreviewRenderer {

    private static boolean isPreviewMode = false;

    private static boolean isExtraPlayerMode = false;

    private static boolean isFirstPersonMode = false;

    public static void setPreviewMode(boolean previewMode) {
        isPreviewMode = previewMode;
    }

    public static boolean isPreview() {
        return isPreviewMode;
    }

    public static void setExtraPlayerMode(boolean extraPlayerMode) {
        isExtraPlayerMode = extraPlayerMode;
    }

    public static boolean isExtraPlayer() {
        return isExtraPlayerMode;
    }

    public static void setFirstPersonMode(boolean firstPersonMode) {
        isFirstPersonMode = firstPersonMode;
    }

    public static boolean isFirstPerson() {
        return isFirstPersonMode || OculusCompat.isPBRActive() || FirstPersonCompat.isFirstPersonActive();
    }

    public static boolean isFirstPersonOnRenderThread() {
        // 1.16.5 无 assertOnRenderThread（1.17+），等价 assertThread(R::isOnRenderThread)
        //? if <1.17
        // RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        //? if >=1.17
        // //? if <1.17
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.17 && <1.18
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.18
        RenderSystem.assertOnRenderThread();
        return isFirstPersonMode && !FirstPersonCompat.isFirstPersonActive();
    }

    public static void renderVehicleModel(Entity entity, PoseStack poseStack, float partialTick) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            VehicleCapability.get(vehicle).ifPresent(cap -> {
                int index;
                AnimatedGeoModel model;
                List<IBone> list;
                if (!cap.isModelInitialized() || !cap.isModelReady() || (index = vehicle.getPassengers().indexOf(entity)) < 0 || (model = cap.getCurrentModel()) == null || model.passengerGroupChains().isEmpty() || index >= model.passengerGroupChains().size() || (list = model.passengerGroupChains().get(index)) == null) {
                    return;
                }
                //? if <1.17
                // float bodyRotation = CustomVehicleRenderer.getBodyRotation(vehicle, Mth.lerp(partialTick, vehicle.yRotO, vehicle.yRot), partialTick);
                //? if >=1.17
                float bodyRotation = CustomVehicleRenderer.getBodyRotation(vehicle, Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()), partialTick);
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0f - bodyRotation));
                //? if >=1.17 && <1.19.3
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0f - bodyRotation));*/
                //? if >=1.19.3
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRotation));
                RenderUtils.prepMatrixForLocator(poseStack, list);
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YN.rotationDegrees(180.0f - bodyRotation));
                //? if >=1.17 && <1.19.3
                /*poseStack.mulPose(com.mojang.math.Vector3f.YN.rotationDegrees(180.0f - bodyRotation));*/
                //? if >=1.19.3
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0f - bodyRotation));
                //? if neoforge && >=1.19.3 && <1.20.5
                /*double myRidingOffset = (-vehicle.getMyRidingOffset(entity)) - entity.getMyRidingOffset(vehicle);*/
                // 1.20.5+ 骑乘偏移 API：getMyRidingOffset(Entity) 删 → getVehicleAttachmentPoint(Entity)=Vec3
                //（vanilla-1.20.6 Entity.java:1877），预览取 y 分量（视觉近似，差异已入接续账）
                //? if neoforge && >=1.20.5
                /*double myRidingOffset = (-vehicle.getVehicleAttachmentPoint(entity).y) - entity.getVehicleAttachmentPoint(vehicle).y;*/
                //? if forge
                double myRidingOffset = (-vehicle.getPassengersRidingOffset()) - entity.getMyRidingOffset();
                if (((entity instanceof Player) && PlayerCapability.get(entity).isPresent()) || TouhouLittleMaidCompat.isMaidRideable(entity)) {
                    myRidingOffset -= 0.5d;
                }
                poseStack.translate(0.0d, myRidingOffset, 0.0d);
            });
        }
    }

    // 动画测试界面的模型
    public static void renderEntityPreview(float x, float y, float scale, float pitch, float yaw, float partialTick, AnimatableEntity animatableEntity, GeoReplacedEntityRenderer renderer, boolean renderGround) {
        setPreviewMode(true);
        LivingEntity livingEntity = (LivingEntity) animatableEntity.getEntity();
        // 1.16.5 无 RenderSystem.getModelViewStack（1.17+），GL_MODELVIEW 直推
        //? if <1.17
        // RenderSystem.pushMatrix();
        //? if >=1.17 && <1.20.5
        // PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //? if >=1.17 && <1.20.5
        // modelViewStack.pushPose();
        //? if >=1.20.5
        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();*/
        //? if >=1.20.5
        /*modelViewStack.pushMatrix();*/
        //? if <1.17
        // RenderSystem.translatef((float) x, (float) y, 1250.0f);
        //? if <1.17
        // RenderSystem.scalef(1.0f, 1.0f, -1.0f);
        //? if >=1.17 && <1.20.5
        // modelViewStack.translate(x, y, 1250.0d);
        //? if >=1.17 && <1.20.5
        // modelViewStack.scale(1.0f, 1.0f, -1.0f);
        //? if >=1.20.5
        /*modelViewStack.translate((float) x, (float) y, 1250.0f);*/
        //? if >=1.20.5
        /*modelViewStack.scale(1.0f, 1.0f, -1.0f);*/
        //? if >=1.17
        // RenderCompat.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0d, 0.8d, 0.0d);

        // joml Axis.XP.rotationDegrees(deg) ≡ 1.16.5 new Quaternion(Vector3f.XP, deg, true)（轴角，度）
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // com.mojang.math.Quaternion rotationX = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, (-10.0f) + pitch, true);
        //? } else {
        // 中段 mojang Quaternion（rotationDegrees 返回值 1192 Vector3f.java:192）/ 1.19.4+ JOML Quaternionf，
        // else 体（活跃区）内用行条件段化（块条件嵌套会闭合外层，YsmTag 实测）
        //? if >=1.17 && <1.19.3
        /*com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f);
        com.mojang.math.Quaternion rotationX = com.mojang.math.Vector3f.XP.rotationDegrees((-10.0f) + pitch);*/
        //? if >=1.19.3
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.0f);
        //? if >=1.19.3
        Quaternionf rotationX = Axis.XP.rotationDegrees((-10.0f) + pitch);
        //? }
        rotationZ.mul(rotationX);
        poseStack.mulPose(rotationZ);

        float oldBodyRot = livingEntity.yBodyRot;
        float oldBodyRotO = livingEntity.yBodyRotO;
        //? if <1.17
        // float oldYRot = livingEntity.yRot;
        //? if >=1.17
        float oldYRot = livingEntity.getYRot();
        float oldYRotO = livingEntity.yRotO;
        //? if <1.17
        // float oldXRot = livingEntity.xRot;
        //? if >=1.17
        float oldXRot = livingEntity.getXRot();
        float oldXRotO = livingEntity.xRotO;
        float oldHeadRotO = livingEntity.yHeadRotO;
        float oldHeadRot = livingEntity.yHeadRot;
        Pose oldPose = livingEntity.getPose();
        livingEntity.yBodyRot = -yaw;
        livingEntity.yBodyRotO = -yaw;
        //? if <1.17
        // livingEntity.yRot = 180.0f;
        //? if >=1.17
        livingEntity.setYRot(180.0f);
        livingEntity.yRotO = 180.0f;
        //? if <1.17
        // livingEntity.xRot = 0.0f;
        //? if >=1.17
        livingEntity.setXRot(0.0f);
        livingEntity.xRotO = 0.0f;
        livingEntity.yHeadRot = -yaw;
        livingEntity.yHeadRotO = -yaw;

        // 1.16.5 无 setupForEntityInInventory（1.17+），GUI 平光用 setupForFlatItems
        //? if <1.17
        // Lighting.setupForFlatItems();
        //? if >=1.17 && <21.6
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        //? if <1.17
        // rotationX.conj();
        //? if >=1.17 && <1.19.3
        /*rotationX.conj();*/
        //? if >=1.19.3
        rotationX.conjugate();
        //? if <21.9
        entityRenderDispatcher.overrideCameraOrientation(rotationX);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        //? if <21.9
        entityRenderDispatcher.setRenderShadow(false);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderCompat.runAsFancy(() -> {
            AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();
            if (animationTracker.isCurrentAnimation("sleep")) {
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw - 90.0f));
                //? if >=1.17 && <1.19.3
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw - 90.0f));*/
                //? if >=1.19.3
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
                poseStack.translate(0.5d, 0.5625d, 0.0d);
                livingEntity.setPose(Pose.SLEEPING);
            }
            if (animationTracker.isCurrentAnimation("swim") || animationTracker.isCurrentAnimation("swim_stand")) {
                livingEntity.setPose(Pose.SWIMMING);
            }
            if (animationTracker.isCurrentAnimation("sneak") || animationTracker.isCurrentAnimation("sneaking")) {
                livingEntity.setPose(Pose.CROUCHING);
            }
            if (animationTracker.isCurrentAnimation("sit")) {
                poseStack.translate(0.0d, -0.5d, 0.0d);
            }
            if (animationTracker.isCurrentAnimation("ride")) {
                poseStack.translate(0.0d, 0.85d, 0.0d);
            }
            if (animationTracker.isCurrentAnimation("ride_pig")) {
                poseStack.translate(0.0d, 0.3125d, 0.0d);
            }
            if (animationTracker.isCurrentAnimation("boat")) {
                poseStack.translate(0.0d, -0.45d, 0.0d);
            }
            try {
                renderVehicleForAnimation(yaw, animatableEntity, partialTick, poseStack, entityRenderDispatcher, bufferSource);
                if (animationTracker.isCurrentAnimation("sleep")) {
                    renderBedPreview(scale, pitch, yaw, bufferSource);
                }
                if (renderGround) {
                    renderGroundPreview(scale, pitch, yaw, bufferSource);
                }
                bufferSource.endBatch();
                renderer.renderEntity((LivingAnimatable) animatableEntity, 0.0f, partialTick, poseStack, bufferSource, 15728880);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        bufferSource.endBatch();
        //? if <21.9
        entityRenderDispatcher.setRenderShadow(true);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        livingEntity.yBodyRot = oldBodyRot;
        livingEntity.yBodyRotO = oldBodyRotO;
        //? if <1.17
        // livingEntity.yRot = oldYRot;
        //? if >=1.17
        livingEntity.setYRot(oldYRot);
        livingEntity.yRotO = oldYRotO;
        //? if <1.17
        // livingEntity.xRot = oldXRot;
        //? if >=1.17
        livingEntity.setXRot(oldXRot);
        livingEntity.xRotO = oldXRotO;
        livingEntity.yHeadRotO = oldHeadRotO;
        livingEntity.yHeadRot = oldHeadRot;
        livingEntity.setPose(oldPose);

        //? if <1.17
        // RenderSystem.popMatrix();
        //? if >=1.17 && <1.20.5
        // modelViewStack.popPose();
        //? if >=1.20.5
        /*modelViewStack.popMatrix();*/
        //? if >=1.17
        // RenderCompat.applyModelViewMatrix();
        //? if <21.6
        Lighting.setupFor3DItems();
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        setPreviewMode(false);
    }

    private static void renderBedPreview(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0d, 0.8d, 0.0d);
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // rotationZ.mul(new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, (-10.0f) + pitch, true));
        //? } else {
        //? if >=1.17 && <1.19.3
        /*com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f);
        rotationZ.mul(com.mojang.math.Vector3f.XP.rotationDegrees((-10.0f) + pitch));*/
        //? if >=1.19.3
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.0f);
        //? if >=1.19.3
        rotationZ.mul(Axis.XP.rotationDegrees((-10.0f) + pitch));
        //? }
        poseStack.mulPose(rotationZ);
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw + 180.0f));
        //? if >=1.17 && <1.19.3
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw + 180.0f));*/
        //? if >=1.19.3
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0f));
        poseStack.translate(-0.5d, 0.0d, 0.5d);
        //? if <26
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_BED.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        // 26.x：BlockRenderDispatcher.renderSingleBlock 全套删（26.1 块渲染改
        // BlockModelRenderState/model-set 制，vanilla-26.1 无 renderSingleBlock 符号）
        // → 床型预览装饰 no-op（功能债 debt-26x）
    }

    private static void renderGroundPreview(float scale, float pitch, float yaw, MultiBufferSource.BufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0d, 0.8d, 0.0d);
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // rotationZ.mul(new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, (-10.0f) + pitch, true));
        //? } else {
        //? if >=1.17 && <1.19.3
        /*com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f);
        rotationZ.mul(com.mojang.math.Vector3f.XP.rotationDegrees((-10.0f) + pitch));*/
        //? if >=1.19.3
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.0f);
        //? if >=1.19.3
        rotationZ.mul(Axis.XP.rotationDegrees((-10.0f) + pitch));
        //? }
        poseStack.mulPose(rotationZ);
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));
        //? if >=1.17 && <1.19.3
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));*/
        //? if >=1.19.3
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5d, -1.0d, -2.5d);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0.0f, 0.0f, 1.0f);
                //? if <26
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
                // 26.x：renderSingleBlock 删 → 草方块地面预览 no-op（功能债 debt-26x）
            }
            poseStack.translate(1.0f, 0.0f, -3.0f);
        }

        poseStack.translate(-1.0f, 1.0f, 1.0f);
        //? if neoforge && <26
        /*Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.SHORT_GRASS.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);*/
        // 26.x：renderSingleBlock 删 → 短草预览 no-op（功能债 debt-26x）
        //? if forge && <26
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        // 26.x：renderSingleBlock 删 → 草丛预览 no-op（功能债 debt-26x；forge 行 26.x 无注册线，守卫简化）
        poseStack.translate(0.0f, 0.0f, 1.0f);
        //? if <26
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_TULIP.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        // 26.x：renderSingleBlock 删 → 红郁金香预览 no-op（功能债 debt-26x）
    }

    // 1.21.9+ dispatcher.render 直绘删（render-dag 换代）→ 载具动画预览降级 no-op
    //（真身实现仅 <21.9，功能债同 renderVehicleEntity）
    //? if >=21.9 {
    /*
    private static void renderVehicleForAnimation(float yaw, AnimatableEntity animatableEntity, float partialTick, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource) throws ExecutionException {
    }
    *///?}
    //? if <21.9 {
    private static void renderVehicleForAnimation(float yaw, AnimatableEntity animatableEntity, float partialTick, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource) throws ExecutionException {
        Entity entity = animatableEntity.getEntity();
        AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();

        if (animationTracker.isCurrentAnimation("ride")) {
            //? if <1.18.2
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(entity.level)), partialTick);
            //? if >=1.18.2 && <1.20
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(entity.getLevel())), partialTick);*/
            //? if >=1.20 && <1.21.2
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(entity.level())), partialTick);
            // 1.21.2 EntityType.create(Level) 删除（EntitySpawnReason 重构，仅剩
            // ServerLevel 全参形）→ 预览实体走公有 (EntityType, Level) 构造
            //（vanilla-1.21.3 Horse.java:42/Pig.java:55 实证）
            //? if >=1.21.2
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> new net.minecraft.world.entity.animal.horse.Horse(EntityType.HORSE, entity.level())), partialTick);*/
        } else if (animationTracker.isCurrentAnimation("ride_pig")) {
            //? if <1.18.2
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(entity.level)), partialTick);
            //? if >=1.18.2 && <1.20
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(entity.getLevel())), partialTick);*/
            //? if >=1.20 && <1.21.2
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(entity.level())), partialTick);
            //? if >=1.21.2
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> new net.minecraft.world.entity.animal.Pig(EntityType.PIG, entity.level())), partialTick);*/
        } else if (animationTracker.isCurrentAnimation("boat")) {
            //? if <1.18.2
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(entity.level)), partialTick);
            //? if >=1.18.2 && <1.20
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(entity.getLevel())), partialTick);*/
            //? if >=1.20 && <1.21.2
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(entity.level())), partialTick);
            // 1.21.2 船分树种（BOAT 删 → OAK_BOAT 等，vanilla-1.21.3 EntityType.java:679）+
            // Boat 构造三参（木种掉落物 Supplier，Boat.java:10）
            //? if >=1.21.2
            /*renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.OAK_BOAT), () -> new net.minecraft.world.entity.vehicle.Boat(EntityType.OAK_BOAT, entity.level(), () -> net.minecraft.world.item.Items.OAK_BOAT)), partialTick);*/
        }
    }
    //?}

    // 1.21.9+ EntityRenderDispatcher.render 直绘删（render-dag/SubmitNodeCollector 换代，
    // 2110 EntityRenderDispatcher.java 方法面实证）→ 载具预览降级 no-op
    //（正规迁移=extractEntity+submit 重构，功能债入账）；真身实现仅 <21.9
    //? if >=21.9 {
    /*
    private static void renderVehicleEntity(float yaw, Entity riderEntity, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource, Entity vehicleEntity, float partialTick) {
    }
    *///?}
    //? if <21.9 {
    private static void renderVehicleEntity(float yaw, Entity riderEntity, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource, Entity vehicleEntity, float partialTick) {
        poseStack.pushPose();
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));
        //? if >=1.17 && <1.19.3
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));*/
        //? if >=1.19.3
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        //? if neoforge && >=1.19.3 && <1.20.5
        /*entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getMyRidingOffset(riderEntity) - riderEntity.getMyRidingOffset(vehicleEntity)), 0.0d, 0.0f, partialTick, poseStack, bufferSource, 15728880);*/
        //? if neoforge && >=1.20.5 && <1.21.2
        /*entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getVehicleAttachmentPoint(riderEntity).y - riderEntity.getVehicleAttachmentPoint(vehicleEntity).y), 0.0d, 0.0f, partialTick, poseStack, bufferSource, 15728880);*/
        // 1.21.2 dispatcher.render 去头浮点（yaw 进 render state；单浮点=partialTick，
        // vanilla-1.21.3 EntityRenderDispatcher.java:148 公有 render 八参）
        //? if neoforge && >=1.21.2
        /*entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getVehicleAttachmentPoint(riderEntity).y - riderEntity.getVehicleAttachmentPoint(vehicleEntity).y), 0.0d, partialTick, poseStack, bufferSource, 15728880);*/
        // forge<1.19.3 分支（基线=无条件行；4e0a2d9 分支化时漏掉 <1.19.3 段导致
        // 1.16.5~1.19.2 载具预览 render 丢失——1.16.5 产物 javap 对比实证，此处补回）
        //? if forge && <1.19.3
        /*entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getPassengersRidingOffset()) - riderEntity.getMyRidingOffset(), 0.0d, 0.0f, partialTick, poseStack, bufferSource, 15728880);*/
        //? if forge && >=1.19.3
        entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getPassengersRidingOffset()) - riderEntity.getMyRidingOffset(), 0.0d, 0.0f, partialTick, poseStack, bufferSource, 15728880);
        poseStack.popPose();
    }
    //?}

    // 模型预览页面
    public static <T extends LivingEntity, TAnimatable extends LivingAnimatable<T>> void renderLivingEntityPreview(float x, float y, float scale, float partialTick, TAnimatable animatable, GeoReplacedEntityRenderer<T, TAnimatable> renderer, boolean disablePreviewRotation, boolean hideEquipment) {
        ItemStack[] savedEquipment;
        setPreviewMode(true);
        LivingEntity livingEntity = animatable.getEntity();
        //? if <1.17
        // RenderSystem.pushMatrix();
        //? if >=1.17 && <1.20.5
        // PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //? if >=1.17 && <1.20.5
        // modelViewStack.pushPose();
        //? if >=1.20.5
        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();*/
        //? if >=1.20.5
        /*modelViewStack.pushMatrix();*/
        //? if <1.17
        // RenderSystem.translatef((float) x, (float) y, 1050.0f);
        //? if <1.17
        // RenderSystem.scalef(1.0f, 1.0f, -1.0f);
        //? if >=1.17 && <1.20.5
        // modelViewStack.translate(x, y, 1050.0d);
        //? if >=1.17 && <1.20.5
        // modelViewStack.scale(1.0f, 1.0f, -1.0f);
        //? if >=1.20.5
        /*modelViewStack.translate((float) x, (float) y, 1050.0f);*/
        //? if >=1.20.5
        /*modelViewStack.scale(1.0f, 1.0f, -1.0f);*/
        //? if >=1.17
        // RenderCompat.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, disablePreviewRotation ? 5.5d : 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // com.mojang.math.Quaternion rotationX = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, disablePreviewRotation ? 0.0f : -10.0f, true);
        //? } else {
        //? if >=1.17 && <1.19.3
        /*com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.0f);
        com.mojang.math.Quaternion rotationX = com.mojang.math.Vector3f.XP.rotationDegrees(disablePreviewRotation ? 0.0f : -10.0f);*/
        //? if >=1.19.3
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.0f);
        //? if >=1.19.3
        Quaternionf rotationX = Axis.XP.rotationDegrees(disablePreviewRotation ? 0.0f : -10.0f);
        //? }
        rotationZ.mul(rotationX);
        poseStack.mulPose(rotationZ);

        float oldBodyRot = livingEntity.yBodyRot;
        float oldBodyRotO = livingEntity.yBodyRotO;
        //? if <1.17
        // float oldYRot = livingEntity.yRot;
        //? if >=1.17
        float oldYRot = livingEntity.getYRot();
        float oldYRotO = livingEntity.yRotO;
        //? if <1.17
        // float oldXRot = livingEntity.xRot;
        //? if >=1.17
        float oldXRot = livingEntity.getXRot();
        float oldXRotO = livingEntity.xRotO;
        float oldHeadRotO = livingEntity.yHeadRotO;
        float oldHeadRot = livingEntity.yHeadRot;
        if (hideEquipment && (livingEntity instanceof Player)) {
            Player player = (Player) livingEntity;
            savedEquipment = new ItemStack[EquipmentSlot.values().length];
            int slotIndex = 0;
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (equipmentSlot == EquipmentSlot.MAINHAND) {
                    //? if <1.17
                    // player.inventory.items.set(player.inventory.selected, ItemStack.EMPTY);
                    //? if >=1.17 && <21.5
                    player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
                    // 1.21.5 Inventory selected/items/offhand/armor 字段私有化/移除
                    //（neoforge-21.5.98-sources Inventory.java:47-50/getSelectedSlot:58/setSelectedItem:74）
                    //? if >=21.5
                    /*player.getInventory().setSelectedItem(ItemStack.EMPTY);*/
                } else if (equipmentSlot == EquipmentSlot.OFFHAND) {
                    //? if <1.17
                    // player.inventory.offhand.set(0, ItemStack.EMPTY);
                    //? if >=1.17 && <21.5
                    player.getInventory().offhand.set(0, ItemStack.EMPTY);
                    //? if >=21.5
                    /*player.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, ItemStack.EMPTY);*/
                } else {
                    //? if <1.17
                    // NonNullList<ItemStack> armorList = player.inventory.armor;
                    //? if >=1.17 && <21.5
                    NonNullList<ItemStack> armorList = player.getInventory().armor;
                    //? if <21.5 {
                    if (armorList.size() > equipmentSlot.getIndex()) {
                        armorList.set(equipmentSlot.getIndex(), ItemStack.EMPTY);
                    }
                    //?}
                    // 21.5 盔甲槽 = 36+EquipmentSlot.getIndex（Inventory.java:33 EQUIPMENT_SLOT_MAPPING FEET→36）
                    //? if >=21.5
                    /*player.getInventory().setItem(36 + equipmentSlot.getIndex(), ItemStack.EMPTY);*/
                }
                savedEquipment[slotIndex] = player.getItemBySlot(equipmentSlot);
                slotIndex++;
            }
        } else {
            savedEquipment = null;
        }

        float previewYaw = disablePreviewRotation ? 180.0f : 200.0f;
        livingEntity.yBodyRot = previewYaw;
        livingEntity.yBodyRotO = previewYaw;
        //? if <1.17
        // livingEntity.yRot = previewYaw;
        //? if >=1.17
        livingEntity.setYRot(previewYaw);
        livingEntity.yRotO = previewYaw;
        //? if <1.17
        // livingEntity.xRot = 0.0f;
        //? if >=1.17
        livingEntity.setXRot(0.0f);
        livingEntity.xRotO = 0.0f;
        //? if <1.17
        // livingEntity.yHeadRot = livingEntity.yRot;
        //? if <1.17
        // livingEntity.yHeadRotO = livingEntity.yRot;
        //? if >=1.17
        livingEntity.yHeadRot = livingEntity.getYRot();
        //? if <1.17
        // livingEntity.yHeadRotO = livingEntity.yRot;
        //? if >=1.17
        livingEntity.yHeadRotO = livingEntity.getYRot();

        Entity vehicle = livingEntity.getVehicle();
        if (vehicle instanceof LivingEntity) {
            //? if <1.17
            // float vehicleYaw = vehicle.yRot;
            //? if >=1.17
            float vehicleYaw = vehicle.getYRot();
            //? if <1.17
            // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(vehicleYaw - previewYaw));
            //? if >=1.17 && <1.19.3
            /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(vehicleYaw - previewYaw));*/
            //? if >=1.19.3
            poseStack.mulPose(Axis.YP.rotationDegrees(vehicleYaw - previewYaw));
            livingEntity.yHeadRot = vehicleYaw;
            livingEntity.yHeadRotO = vehicleYaw;
        }

        // 1.16.5 无 setupForEntityInInventory（1.17+），GUI 平光用 setupForFlatItems
        //? if <1.17
        // Lighting.setupForFlatItems();
        //? if >=1.17 && <21.6
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        //? if <1.17
        // rotationX.conj();
        //? if >=1.17 && <1.19.3
        /*rotationX.conj();*/
        //? if >=1.19.3
        rotationX.conjugate();
        //? if <21.9
        entityRenderDispatcher.overrideCameraOrientation(rotationX);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        //? if <21.9
        entityRenderDispatcher.setRenderShadow(false);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderCompat.runAsFancy(() -> {
            renderer.renderEntity(animatable, 0.0f, partialTick, poseStack, bufferSource, 15728880);
        });

        bufferSource.endBatch();
        //? if <21.9
        entityRenderDispatcher.setRenderShadow(true);
        // 1.21.9+ EntityRenderDispatcher 直绘面删（render-dag 换代）→ no-op
        livingEntity.yBodyRot = oldBodyRot;
        livingEntity.yBodyRotO = oldBodyRotO;
        //? if <1.17
        // livingEntity.yRot = oldYRot;
        //? if >=1.17
        livingEntity.setYRot(oldYRot);
        livingEntity.yRotO = oldYRotO;
        //? if <1.17
        // livingEntity.xRot = oldXRot;
        //? if >=1.17
        livingEntity.setXRot(oldXRot);
        livingEntity.xRotO = oldXRotO;
        livingEntity.yHeadRotO = oldHeadRotO;
        livingEntity.yHeadRot = oldHeadRot;
        if (savedEquipment != null) {
            Player player = (Player) livingEntity;
            int slotIndex = 0;
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = savedEquipment[slotIndex];
                if (equipmentSlot == EquipmentSlot.MAINHAND) {
                    //? if <1.17
                    // player.inventory.items.set(player.inventory.selected, itemStack);
                    //? if >=1.17 && <21.5
                    player.getInventory().items.set(player.getInventory().selected, itemStack);
                    //? if >=21.5
                    /*player.getInventory().setSelectedItem(itemStack);*/
                } else if (equipmentSlot == EquipmentSlot.OFFHAND) {
                    //? if <1.17
                    // player.inventory.offhand.set(0, itemStack);
                    //? if >=1.17 && <21.5
                    player.getInventory().offhand.set(0, itemStack);
                    //? if >=21.5
                    /*player.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, itemStack);*/
                } else {
                    //? if <1.17
                    // NonNullList<ItemStack> armorList = player.inventory.armor;
                    //? if >=1.17 && <21.5
                    NonNullList<ItemStack> armorList = player.getInventory().armor;
                    //? if <21.5 {
                    if (armorList.size() > equipmentSlot.getIndex()) {
                        armorList.set(equipmentSlot.getIndex(), itemStack);
                    }
                    //?}
                    //? if >=21.5
                    /*player.getInventory().setItem(36 + equipmentSlot.getIndex(), itemStack);*/
                }
                slotIndex++;
            }
        }

        //? if <1.17
        // RenderSystem.popMatrix();
        //? if >=1.17 && <1.20.5
        // modelViewStack.popPose();
        //? if >=1.20.5
        /*modelViewStack.popMatrix();*/
        //? if >=1.17
        // RenderCompat.applyModelViewMatrix();
        //? if <21.6
        Lighting.setupFor3DItems();
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        setPreviewMode(false);
    }

    // 纸娃娃
    // GuiGraphics 为 1.20+；1.16.5 变体直接吃 PoseStack（HUD 调用方=ExtraPlayerOverlay，
    // Screen 调用方=ExtraPlayerRenderScreen 拖拽预览）。
    // 配方=vanilla-mc-1165 InventoryScreen.renderEntityInInventory:101-138 的镜像（runClient
    // 截图实证：原版生存背包纸娃娃在本环境正常渲染，本方法与其同配方后亦正常）：
    // RenderSystem.translatef z 必须取 1050、poseStack.translate z 必须取 +1000——
    // GUI 正交 near=1000/far=3000（GameRenderer.render）下 eye_z≈-1950→clip≈-0.05，
    // 早于 HUD 的全屏元素已写深度≈0.5，取值更深（如 z=0/500 → clip≥+0.5 → 深度 0.75）
    // 会被深度测试挡掉（首版 z=0/500 纸娃娃不可见、同点位原版可见的对照实证）。
    // 其余逐 API 实证：Quaternion(Vector3f,float,boolean) 构造/mul/conj、
    // overrideCameraOrientation（EntityRenderDispatcher:214）、Lighting.setupForFlatItems/
    // setupFor3DItems（blaze3d.platform.Lighting:33/37）、renderBuffers().bufferSource().endBatch。
    // zDepth 参数在 <1.17 轴不参与映射（原版定数优先），仅保签名双轴一致。
    //? if <1.17 {
    /*public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) (x + (scale * 0.5d)), (float) (y + (scale * 2.0d)), 1050.0f);
        RenderSystem.scalef(1.0f, 1.0f, -1.0f);
        poseStack.pushPose();
        poseStack.translate(0.0d, 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.1f, true);
        com.mojang.math.Quaternion rotationY = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.YP, (Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f, true);
        rotationZ.mul(rotationY);
        poseStack.mulPose(rotationZ);
        Lighting.setupForFlatItems();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conj();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);
        // HUD 语境雾残留（事件点实测）：GL_FOG 关但世界雾参数残留——mode=GL_EXP2(2049)、
        // density 残留、start=132/end=176。实体 RenderType setupRenderState 会 enableFog，
        // eye_z≈-1950 在 GL_EXP2 下雾因子 exp(-(density·z)²)=0 → 整模被雾成天空色
        //（对天空不可见；像素回读实证 px=雾色×光照）。指数雾只认 density 不认 start/end
        //（fogStart/End 压平无效——首版修复踩坑实证）→ 绘制前置 density=0（雾因子=1），画后还原。
        java.nio.FloatBuffer fogBuf = java.nio.ByteBuffer.allocateDirect(4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_FOG_DENSITY, (java.nio.FloatBuffer) fogBuf.clear());
        float fogDensityBackup = fogBuf.get(0);
        org.lwjgl.opengl.GL11.glFogf(org.lwjgl.opengl.GL11.GL_FOG_DENSITY, 0.0f);
        RenderCompat.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        });
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        org.lwjgl.opengl.GL11.glFogf(org.lwjgl.opengl.GL11.GL_FOG_DENSITY, fogDensityBackup);
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
        RenderSystem.popMatrix();
        Lighting.setupFor3DItems();
        setExtraPlayerMode(false);
    }
     *///?}
    // 中段（1.17~1.19.2）：getModelViewStack(PoseStack)+mojang Quaternion（1192 PoseStack.mulPose(Quaternion):46）；
    // 1.16.5 走上方 pushMatrix 版
    //? if >=1.17 && <1.19.3 {
    /*public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderCompat.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -zDepth);
        poseStack.scale(scale, scale, scale);
        com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.1f);
        com.mojang.math.Quaternion rotationY = com.mojang.math.Vector3f.YP.rotationDegrees((Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f);
        rotationZ.mul(rotationY);
        poseStack.mulPose(rotationZ);
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        //? if <21.6
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conj();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);
        RenderCompat.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        });
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
        modelViewStack.popPose();
        RenderCompat.applyModelViewMatrix();
        //? if <21.6
        Lighting.setupFor3DItems();
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        setExtraPlayerMode(false);
    }
     *///?}
    // 1.19.4：Axis 枚举 + JOML Quaternionf（1.19.3 JOML 内置，mulPose(Quaternionf)）
    //? if >=1.19.3 && <1.20 {
    /*
    public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderCompat.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -zDepth);
        poseStack.scale(scale, scale, scale);
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.1f);
        Quaternionf rotationY = Axis.YP.rotationDegrees((Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f);
        rotationZ.mul(rotationY);
        poseStack.mulPose(rotationZ);
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        //? if <21.6
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);
        RenderCompat.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        });
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
        modelViewStack.popPose();
        RenderCompat.applyModelViewMatrix();
        //? if <21.6
        Lighting.setupFor3DItems();
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        setExtraPlayerMode(false);
    }
     *///?}
    //? if >=1.20 && <21.6 {
    public static void renderPlayerOverlay(GuiGraphics guiGraphics, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        //? if >=1.20.5
        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();*/
        //? if >=1.20.5
        /*modelViewStack.pushMatrix();*/
        //? if >=1.20.5
        /*modelViewStack.translate((float) (x + (scale * 0.5d)), (float) (y + (scale * 2.0f)), 0.0f);*/
        //? if >=1.20.5
        /*modelViewStack.scale(1.0f, 1.0f, -1.0f);*/
        //? if >=1.20 && <1.20.5
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //? if >=1.20 && <1.20.5
        modelViewStack.pushPose();
        //? if >=1.20 && <1.20.5
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        //? if >=1.20 && <1.20.5
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderCompat.applyModelViewMatrix();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, -zDepth);
        guiGraphics.pose().scale(scale, scale, scale);

        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.1f);
        Quaternionf rotationY = Axis.YP.rotationDegrees((Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f);
        rotationZ.mul(rotationY);
        guiGraphics.pose().mulPose(rotationZ);

        // 1.16.5 无 setupForEntityInInventory（1.17+），GUI 平光用 setupForFlatItems
        //? if <1.17
        // Lighting.setupForFlatItems();
        //? if >=1.17 && <21.6
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);

        // 1.21.2 GuiGraphics.bufferSource() 删除（bufferSource 私有化，vanilla-1.21.3
        // GuiGraphics.java:62）+ dispatcher.render 八参化 → drawSpecial（:1100，自带 endBatch）
        //? if <1.21.2 {
        RenderCompat.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
        });

        guiGraphics.flush();
        //?}
        //? if >=1.21.2 {
        /*guiGraphics.drawSpecial(buffer -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0d, partialTick, guiGraphics.pose(), buffer, 15728880);
        });*/
        //?}
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        //? if >=1.20.5
        /*modelViewStack.popMatrix();*/
        //? if >=1.20 && <1.20.5
        modelViewStack.popPose();
        RenderCompat.applyModelViewMatrix();
        //? if <21.6
        Lighting.setupFor3DItems();
        // 1.21.6+ Lighting 静态置光删（UBO 化）→ no-op
        setExtraPlayerMode(false);
    }
    //?}
    // 1.21.6+ GUI 全状态化（pose()→Matrix3x2fStack、drawSpecial 删）→ 纸娃娃 HUD 整体降级
    // no-op（正规迁移=GuiEntityRenderState PIP，功能债入账）
    //? if >=21.6 {
    /*public static void renderPlayerOverlay(GuiGraphics guiGraphics, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
    }*/
    //?}
}
