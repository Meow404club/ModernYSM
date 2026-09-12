package com.elfmcys.yesstevemodel.client.renderer;

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

import java.util.List;
import java.util.concurrent.ExecutionException;
//? if >=1.19.4 {
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
        //? if >=1.17
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
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0f - bodyRotation));*/
                //? if >=1.19.4
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRotation));
                RenderUtils.prepMatrixForLocator(poseStack, list);
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YN.rotationDegrees(180.0f - bodyRotation));
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.YN.rotationDegrees(180.0f - bodyRotation));*/
                //? if >=1.19.4
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0f - bodyRotation));
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
        //? if >=1.17
        // PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //? if >=1.17
        // modelViewStack.pushPose();
        //? if <1.17
        // RenderSystem.translatef((float) x, (float) y, 1250.0f);
        //? if <1.17
        // RenderSystem.scalef(1.0f, 1.0f, -1.0f);
        //? if >=1.17
        // modelViewStack.translate(x, y, 1250.0d);
        //? if >=1.17
        // modelViewStack.scale(1.0f, 1.0f, -1.0f);
        //? if >=1.17
        // RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0d, 0.8d, 0.0d);

        // joml Axis.XP.rotationDegrees(deg) ≡ 1.16.5 new Quaternion(Vector3f.XP, deg, true)（轴角，度）
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // com.mojang.math.Quaternion rotationX = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, (-10.0f) + pitch, true);
        //? } else {
        Quaternionf rotationZ = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/ZP.rotationDegrees(180.0f);
        Quaternionf rotationX = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/XP.rotationDegrees((-10.0f) + pitch);
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
        //? if >=1.17
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        //? if <1.17
        // rotationX.conj();
        //? if >=1.17
        rotationX.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationX);
        entityRenderDispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.runAsFancy(() -> {
            AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();
            if (animationTracker.isCurrentAnimation("sleep")) {
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw - 90.0f));
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw - 90.0f));*/
                //? if >=1.19.4
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
        entityRenderDispatcher.setRenderShadow(true);
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
        //? if >=1.17
        // modelViewStack.popPose();
        //? if >=1.17
        // RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
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
        Quaternionf rotationZ = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/ZP.rotationDegrees(180.0f);
        rotationZ.mul(/*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/XP.rotationDegrees((-10.0f) + pitch));
        //? }
        poseStack.mulPose(rotationZ);
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw + 180.0f));
        //? if >=1.17 && <1.19.4
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw + 180.0f));*/
        //? if >=1.19.4
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0f));
        poseStack.translate(-0.5d, 0.0d, 0.5d);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_BED.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
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
        Quaternionf rotationZ = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/ZP.rotationDegrees(180.0f);
        rotationZ.mul(/*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/XP.rotationDegrees((-10.0f) + pitch));
        //? }
        poseStack.mulPose(rotationZ);
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));
        //? if >=1.17 && <1.19.4
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));*/
        //? if >=1.19.4
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5d, -1.0d, -2.5d);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0.0f, 0.0f, 1.0f);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            }
            poseStack.translate(1.0f, 0.0f, -3.0f);
        }

        poseStack.translate(-1.0f, 1.0f, 1.0f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GRASS.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        poseStack.translate(0.0f, 0.0f, 1.0f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.RED_TULIP.defaultBlockState(), poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
    }

    private static void renderVehicleForAnimation(float yaw, AnimatableEntity animatableEntity, float partialTick, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource) throws ExecutionException {
        Entity entity = animatableEntity.getEntity();
        AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();

        if (animationTracker.isCurrentAnimation("ride")) {
            //? if <1.17
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(entity.level)), partialTick);
            //? if >=1.17
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.HORSE), () -> EntityType.HORSE.create(entity.level())), partialTick);
        } else if (animationTracker.isCurrentAnimation("ride_pig")) {
            //? if <1.17
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(entity.level)), partialTick);
            //? if >=1.17
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.PIG), () -> EntityType.PIG.create(entity.level())), partialTick);
        } else if (animationTracker.isCurrentAnimation("boat")) {
            //? if <1.17
            // renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(entity.level)), partialTick);
            //? if >=1.17
            renderVehicleEntity(yaw, entity, poseStack, entityRenderDispatcher, bufferSource, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityType.BOAT), () -> EntityType.BOAT.create(entity.level())), partialTick);
        }
    }

    private static void renderVehicleEntity(float yaw, Entity riderEntity, PoseStack poseStack, EntityRenderDispatcher entityRenderDispatcher, MultiBufferSource.BufferSource bufferSource, Entity vehicleEntity, float partialTick) {
        poseStack.pushPose();
        //? if <1.17
        // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));
        //? if >=1.17 && <1.19.4
        /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(yaw));*/
        //? if >=1.19.4
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        entityRenderDispatcher.render(vehicleEntity, 0.0d, (-vehicleEntity.getPassengersRidingOffset()) - riderEntity.getMyRidingOffset(), 0.0d, 0.0f, partialTick, poseStack, bufferSource, 15728880);
        poseStack.popPose();
    }

    // 模型预览页面
    public static <T extends LivingEntity, TAnimatable extends LivingAnimatable<T>> void renderLivingEntityPreview(float x, float y, float scale, float partialTick, TAnimatable animatable, GeoReplacedEntityRenderer<T, TAnimatable> renderer, boolean disablePreviewRotation, boolean hideEquipment) {
        ItemStack[] savedEquipment;
        setPreviewMode(true);
        LivingEntity livingEntity = animatable.getEntity();
        //? if <1.17
        // RenderSystem.pushMatrix();
        //? if >=1.17
        // PoseStack modelViewStack = RenderSystem.getModelViewStack();
        //? if >=1.17
        // modelViewStack.pushPose();
        //? if <1.17
        // RenderSystem.translatef((float) x, (float) y, 1050.0f);
        //? if <1.17
        // RenderSystem.scalef(1.0f, 1.0f, -1.0f);
        //? if >=1.17
        // modelViewStack.translate(x, y, 1050.0d);
        //? if >=1.17
        // modelViewStack.scale(1.0f, 1.0f, -1.0f);
        //? if >=1.17
        // RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, disablePreviewRotation ? 5.5d : 0.0d, 1000.0d);
        poseStack.scale(scale, scale, scale);
        //? if <1.17 {
        // com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.0f, true);
        // com.mojang.math.Quaternion rotationX = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.XP, disablePreviewRotation ? 0.0f : -10.0f, true);
        //? } else {
        Quaternionf rotationZ = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/ZP.rotationDegrees(180.0f);
        Quaternionf rotationX = /*? if <1.19.4 {*/ /*com.mojang.math.Vector3f.*//*?} else {*/ Axis./*?}*/XP.rotationDegrees(disablePreviewRotation ? 0.0f : -10.0f);
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
                    //? if >=1.17
                    player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
                } else if (equipmentSlot == EquipmentSlot.OFFHAND) {
                    //? if <1.17
                    // player.inventory.offhand.set(0, ItemStack.EMPTY);
                    //? if >=1.17
                    player.getInventory().offhand.set(0, ItemStack.EMPTY);
                } else {
                    //? if <1.17
                    // NonNullList<ItemStack> armorList = player.inventory.armor;
                    //? if >=1.17
                    NonNullList<ItemStack> armorList = player.getInventory().armor;
                    if (armorList.size() > equipmentSlot.getIndex()) {
                        armorList.set(equipmentSlot.getIndex(), ItemStack.EMPTY);
                    }
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
            //? if >=1.17 && <1.19.4
            /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(vehicleYaw - previewYaw));*/
            //? if >=1.19.4
            poseStack.mulPose(Axis.YP.rotationDegrees(vehicleYaw - previewYaw));
            livingEntity.yHeadRot = vehicleYaw;
            livingEntity.yHeadRotO = vehicleYaw;
        }

        // 1.16.5 无 setupForEntityInInventory（1.17+），GUI 平光用 setupForFlatItems
        //? if <1.17
        // Lighting.setupForFlatItems();
        //? if >=1.17
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        //? if <1.17
        // rotationX.conj();
        //? if >=1.17
        rotationX.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationX);
        entityRenderDispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.runAsFancy(() -> {
            renderer.renderEntity(animatable, 0.0f, partialTick, poseStack, bufferSource, 15728880);
        });

        bufferSource.endBatch();
        entityRenderDispatcher.setRenderShadow(true);
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
                    //? if >=1.17
                    player.getInventory().items.set(player.getInventory().selected, itemStack);
                } else if (equipmentSlot == EquipmentSlot.OFFHAND) {
                    //? if <1.17
                    // player.inventory.offhand.set(0, itemStack);
                    //? if >=1.17
                    player.getInventory().offhand.set(0, itemStack);
                } else {
                    //? if <1.17
                    // NonNullList<ItemStack> armorList = player.inventory.armor;
                    //? if >=1.17
                    NonNullList<ItemStack> armorList = player.getInventory().armor;
                    if (armorList.size() > equipmentSlot.getIndex()) {
                        armorList.set(equipmentSlot.getIndex(), itemStack);
                    }
                }
                slotIndex++;
            }
        }

        //? if <1.17
        // RenderSystem.popMatrix();
        //? if >=1.17
        // modelViewStack.popPose();
        //? if >=1.17
        // RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setPreviewMode(false);
    }

    // 纸娃娃
    // GuiGraphics 为 1.20+；1.16.5 变体直接吃 PoseStack（HUD 调用方随 GUI/HUD 卡适配），
    // 渲染管线语义一致：push 自身变换栈 → 相机四元数 → dispatcher.render → endBatch
    //? if <1.17 {
    // public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
    //     setExtraPlayerMode(true);
    //     RenderSystem.pushMatrix();
    //     RenderSystem.translatef((float) (x + (scale * 0.5d)), (float) (y + (scale * 2.0f)), 0.0f);
    //     RenderSystem.scalef(1.0f, 1.0f, -1.0f);
    //     poseStack.pushPose();
    //     poseStack.translate(0.0f, 0.0f, -zDepth);
    //     poseStack.scale(scale, scale, scale);
    //     com.mojang.math.Quaternion rotationZ = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.ZP, 180.1f, true);
    //     com.mojang.math.Quaternion rotationY = new com.mojang.math.Quaternion(com.mojang.math.Vector3f.YP, (Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f, true);
    //     rotationZ.mul(rotationY);
    //     poseStack.mulPose(rotationZ);
    //     Lighting.setupForFlatItems();
    //     EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    //     rotationY.conj();
    //     entityRenderDispatcher.overrideCameraOrientation(rotationY);
    //     entityRenderDispatcher.setRenderShadow(false);
    //     RenderSystem.runAsFancy(() -> {
    //         entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
    //     });
    //     Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    //     entityRenderDispatcher.setRenderShadow(true);
    //     poseStack.popPose();
    //     RenderSystem.popMatrix();
    //     Lighting.setupFor3DItems();
    //     setExtraPlayerMode(false);
    // }
    //? }
    // 中段（1.17~1.19.2）：getModelViewStack(PoseStack)+mojang Quaternion（1192 PoseStack.mulPose(Quaternion):46）；
    // 1.16.5 走上方 pushMatrix 版（现役产物形态不变）
    //? if >=1.17 && <1.19.4 {
    /*public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -zDepth);
        poseStack.scale(scale, scale, scale);
        com.mojang.math.Quaternion rotationZ = com.mojang.math.Vector3f.ZP.rotationDegrees(180.1f);
        com.mojang.math.Quaternion rotationY = com.mojang.math.Vector3f.YP.rotationDegrees((Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f);
        rotationZ.mul(rotationY);
        poseStack.mulPose(rotationZ);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conj();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        });
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setExtraPlayerMode(false);
    }
     *///?}
    // 1.19.4：Axis 枚举 + JOML Quaternionf（1.19.3 JOML 内置，mulPose(Quaternionf)）
    //? if >=1.19.4 && <1.20 {
    /*
    public static void renderPlayerOverlay(PoseStack poseStack, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -zDepth);
        poseStack.scale(scale, scale, scale);
        Quaternionf rotationZ = Axis.ZP.rotationDegrees(180.1f);
        Quaternionf rotationY = Axis.YP.rotationDegrees((Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset) - 180.0f);
        rotationZ.mul(rotationY);
        poseStack.mulPose(rotationZ);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        });
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setExtraPlayerMode(false);
    }
     *///?}
    //? if >=1.20 {
    public static void renderPlayerOverlay(GuiGraphics guiGraphics, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        setExtraPlayerMode(true);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(x + (scale * 0.5d), y + (scale * 2.0f), 0.0d);
        modelViewStack.scale(1.0f, 1.0f, -1.0f);
        RenderSystem.applyModelViewMatrix();

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
        //? if >=1.17
        // Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        rotationY.conjugate();
        entityRenderDispatcher.overrideCameraOrientation(rotationY);
        entityRenderDispatcher.setRenderShadow(false);

        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(localPlayer, 0.0d, 0.0d, 0.0f, 0.0f, partialTick, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
        });

        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        setExtraPlayerMode(false);
    }
    //? }
}
