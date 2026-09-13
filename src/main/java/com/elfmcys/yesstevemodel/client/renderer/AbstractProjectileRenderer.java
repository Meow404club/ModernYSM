package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.IGeoRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
 *///?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
//? if >=1.19.4 {
import com.mojang.math.Axis;
//? }

//? if <1.21.2
public abstract class AbstractProjectileRenderer<TEntity extends Projectile, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity> implements IGeoRenderer<T> {
//? if >=1.21.2 {
/*public abstract class AbstractProjectileRenderer<TEntity extends Projectile, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity, EntityRenderState> implements IGeoRenderer<T> {

    private TEntity currentEntity;

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(TEntity entity, EntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.currentEntity = entity;
    }

    // 无 @Override：EntityRenderer 基类无 getTextureLocation 抽象（实现占位）
    public ResourceLocation getTextureLocation(EntityRenderState state) {
        // vanilla dispatch 永不触发（dispatcher mixin Static 驱动路径）
        return MissingTextureAtlasSprite.getLocation();
    }*/
//?}

    public Matrix4f modelViewMatrix;

    public Matrix4f projectionMatrix;

    private IRenderCycle renderState;

    public MultiBufferSource bufferSource;

    //? if <1.17 {
    // public AbstractProjectileRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //? } else {
    public AbstractProjectileRenderer(EntityRendererProvider.Context context) {
    //? }
        super(context);
        this.modelViewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.renderState = EModelRenderCycle.INITIAL;
        this.bufferSource = null;
    }

    public void render(T animatable, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        AnimationEvent<?> event = animatable.processAnimation(partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            Projectile projectile = animatable.getEntity();
            boolean isVisible = !projectile.isInvisibleTo(minecraft.player);
            boolean zShouldEntityAppearGlowing = minecraft.shouldEntityAppearGlowing(projectile);
            RenderType renderType = getRenderType(animatable.getTextureLocation(), isVisible, zShouldEntityAppearGlowing, animatable.getCurrentModel().getGeoModel().isTranslucentTexture(0));
            if (renderType != null && (isVisible || zShouldEntityAppearGlowing)) {
                Color color = getRenderColor(animatable, partialTick, poseStack, bufferSource, null, packedLight);
                AnimatedGeoModel model = animatable.getCurrentModel();
                this.modelViewMatrix = new Matrix4f(MatrixBridge.pose(poseStack.last()));
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.yRot) - 90.0f));
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot()) - 90.0f));*/
                //? if >=1.19.4
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot()) - 90.0f));
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(Mth.lerp(partialTick, projectile.xRotO, projectile.xRot)));
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot())));*/
                //? if >=1.19.4
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot())));
                renderWithBoneAndRenderType(model, animatable, partialTick, renderType, poseStack, bufferSource, 0, null, packedLight, getPackedLight(projectile, 0.0f), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                poseStack.popPose();
            }
        }
        //? if <1.21.2
        super.render(animatable.getEntity(), entityYaw, partialTick, poseStack, bufferSource, packedLight);
        //? if >=1.21.2
        /*super.render(this.createRenderState(animatable.getEntity(), partialTick), poseStack, bufferSource, packedLight);*/
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.projectionMatrix = new Matrix4f(MatrixBridge.pose(poseStack.last()));
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public static int getPackedLight(Entity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(false));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.renderState;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.renderState = cycle;
    }

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.bufferSource;
    }
}