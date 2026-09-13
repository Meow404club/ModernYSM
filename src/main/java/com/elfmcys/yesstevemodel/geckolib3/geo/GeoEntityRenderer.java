package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
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
// 1.21.2 render-state 化：EntityRenderer 泛型 1→2 参（<T, S extends EntityRenderState>，
// vanilla-1.21.3 EntityRenderer.java:29），render(S,...) 取代 render(T,F,F,...)；
// 本类实例不走 vanilla dispatcher 注册（Static 驱动路径），state 仅为 super.render 合成面
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
 *///?}
// Axis（1.19.3+）1.16.5/中段走 com.mojang.math.Vector3f.YP/ZP.rotationDegrees（返回 moj Quaternion）；
// 本文件 Axis 触点仅存在于注释（mulPose 旋转由 NativeModelRenderer 管线承接）
//? if >=1.19.4 {
import com.mojang.math.Axis;
//?}
// Axis（1.19.3+）1.16.5 走 com.mojang.math.Vector3f.YP/ZP.rotationDegrees（返回 moj Quaternion）
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
//? if >=1.21.2 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
 *///?}
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

//? if <1.21.2
public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity> implements IGeoRenderer<T> {
//? if >=1.21.2 {
/*public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity, EntityRenderState> implements IGeoRenderer<T> {

    private Entity currentEntity;

    private float currentPartialTick;

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(TEntity entity, EntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.currentEntity = entity;
        this.currentPartialTick = partialTick;
    }

    // 无 @Override：EntityRenderer 基类无 getTextureLocation 抽象（仅 LivingEntityRenderer 有，
    // vanilla-1.21.3 EntityRenderer/EntityRenderer.java 实证）——此为实现占位
    public ResourceLocation getTextureLocation(EntityRenderState state) {
        // 本类实例走 Static 驱动路径（renderEntity 显式传实体），vanilla dispatch 永不触发；
        // 纹理由 AnimatableEntity.getTextureLocation() 提供（renderEntityWithTexture 内）
        return MissingTextureAtlasSprite.getLocation();
    }*/
//?}

    public Matrix4f worldMatrix;

    public Matrix4f modelMatrix;

    private IRenderCycle renderState;

    public MultiBufferSource bufferSource;

    //? if <1.17 {
    // public GeoEntityRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher context) {
    //     super(context);
    //     this.worldMatrix = new Matrix4f();
    //     this.modelMatrix = new Matrix4f();
    //     this.renderState = EModelRenderCycle.INITIAL;
    //     this.bufferSource = null;
    // }
    //? } else {
    public GeoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.worldMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
        this.renderState = EModelRenderCycle.INITIAL;
        this.bufferSource = null;
    }
    //? }

    public void renderEntity(T t, float f, float f2, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        AnimationEvent<?> event = t.processAnimation(f2);
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            Entity entity = t.getEntity();
            boolean z = !entity.isInvisibleTo(minecraft.player);
            boolean zShouldEntityAppearGlowing = minecraft.shouldEntityAppearGlowing(entity);
            RenderType renderType = getRenderType(t.getTextureLocation(), z, zShouldEntityAppearGlowing, t.getCurrentModel().getGeoModel().isTranslucentTexture(0));
            if (renderType != null && (z || zShouldEntityAppearGlowing)) {
                Color color = getRenderColor(t, f2, poseStack, multiBufferSource, null, i);
                AnimatedGeoModel model = t.getCurrentModel();
                this.worldMatrix = new Matrix4f(MatrixBridge.pose(poseStack.last()));
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                poseStack.pushPose();
                //? if <1.17
                // poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0f - f));
                //? if >=1.17 && <1.19.4
                /*poseStack.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(180.0f - f));*/
                //? if >=1.19.4
                // poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - f));
                renderWithBoneAndRenderType(model, t, f2, renderType, poseStack, multiBufferSource, 0, null, i, packOverlayCoords(entity, 0.0f), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                poseStack.popPose();
            }
        }
        //? if <1.21.2
        super.render(t.getEntity(), f, f2, poseStack, multiBufferSource, i);
        //? if >=1.21.2
        /*super.render(this.createRenderState(t.getEntity(), f2), poseStack, multiBufferSource, i);*/
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.modelMatrix = new Matrix4f(MatrixBridge.pose(poseStack.last()));
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public static int packOverlayCoords(Entity entity, float f) {
        return OverlayTexture.pack(OverlayTexture.u(f), OverlayTexture.v(false));
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