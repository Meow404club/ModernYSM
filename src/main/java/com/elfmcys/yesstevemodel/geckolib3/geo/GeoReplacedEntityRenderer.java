package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.extended.LivingEntityRendererAccessor;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.elfmcys.yesstevemodel.mixin.client.LivingEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
// 1.21.11 PlayerModel 移 client.model.player 子包
//? if >=21.11
/*import net.minecraft.client.model.player.PlayerModel;*/
//? if <21.11
import net.minecraft.client.model.PlayerModel;
// 1.16.5 无 EntityRendererProvider/ModelLayers（1.17 模型重写产物），渲染器构造参数为
// EntityRenderDispatcher、模型为传统手写构造（new PlayerModel(0.0f, slim)，1.16.5 vanilla
// PlayerRenderer 同款）；两类 import 仅 1.17+ 存在，1.16.5 生成分支用 EntityRenderDispatcher
//? if >=1.17 {
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：MultiBufferSource 删，collector 形 twin（本文件 <26.2 分支不动）
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;
 */// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if >=1.21.2 && <21.9 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
 *///?}
// 1.21.9 PlayerRenderState → AvatarRenderState（neoforge-21.10.64 state/AvatarRenderState.java
// 实证，PlayerRenderState 全线删除）
//? if >=21.9 && <26.2 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
 *///?}
// 26.2 PiP 预览（debt-262-preview-pip）：state=YsmPreviewRenderState（非 Avatar，理由见类声明注）
//? if >=26.2 {
/*import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.elfmcys.yesstevemodel.client.renderer.YsmPreviewRenderState;
import net.minecraft.resources.ResourceLocation;
 *///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.api.client.RenderLivingBridge;

import java.util.List;
import java.util.Optional;

//? if <1.21.2
public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, PlayerModel<TEntity>> implements IGeoRenderer<T> {
//? if >=1.21.2 && <21.9 {
/*public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, PlayerRenderState, PlayerModel> implements IGeoRenderer<T> {

    // render-state 化（1.21.2+）：vanilla dispatch 链 createRenderState(entity,partialTick)→
    // render(state) 丢失实体引用，本类经 extractRenderState 暂存三元组供 renderEntityWithTexture/
    // setupRotations/事件桥使用（复用单 state 实例，EntityRenderer.reusedState 同款生命周期）
    protected TEntity ysmEntity;

    protected PlayerRenderState ysmState;

    protected float ysmPartialTick;

    @Override
    public PlayerRenderState createRenderState() {
        return new PlayerRenderState();
    }

    @Override
    public void extractRenderState(TEntity entity, PlayerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.ysmEntity = entity;
        this.ysmState = state;
        this.ysmPartialTick = partialTick;
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerRenderState state) {
        // vanilla dispatch 永不触发（Static 驱动路径，CustomPlayerRenderer.render 直调）；
        // 纹理由 capability/t.getTextureLocation() 提供
        return net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
    }*/
//?}
//? if >=21.9 && <26.2 {
/*public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, AvatarRenderState, PlayerModel> implements IGeoRenderer<T> {

    // render-state 化（1.21.2+）：vanilla dispatch 链 createRenderState(entity,partialTick)→
    // render(state) 丢失实体引用，本类经 extractRenderState 暂存三元组供 renderEntityWithTexture/
    // setupRotations/事件桥使用（复用单 state 实例，EntityRenderer.reusedState 同款生命周期）
    // 1.21.9 起 state 类型为 AvatarRenderState（PlayerRenderState 改名）
    protected TEntity ysmEntity;

    protected AvatarRenderState ysmState;

    protected float ysmPartialTick;

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(TEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.ysmEntity = entity;
        this.ysmState = state;
        this.ysmPartialTick = partialTick;
    }

    @Override
    public ResourceLocation getTextureLocation(AvatarRenderState state) {
        // vanilla dispatch 永不触发（Static 驱动路径，CustomPlayerRenderer.render 直调）；
        // 纹理由 capability/t.getTextureLocation() 提供
        return net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
    }*/
//?}
// 26.2 PiP 预览（debt-262-preview-pip）：S=YsmPreviewRenderState——dispatcher.getRenderer(S)
// 对 AvatarRenderState 恒走 vanilla playerRenderers 表（EntityRenderDispatcher.java:112-118），
// 非 Avatar state 按 state.entityType 查 renderers 表（:118），预览注册钩子
// （PreviewRendererRegisterHook，262 代树）把 CustomPlayerRenderer 登记在 EntityTypes.PLAYER 下
// → PiP 绘制期解析回我方渲染器；vanilla 玩家实体/state 全走 avatar 分支对本表项零可观测。
// M 界随之换代：PlayerModel=EntityModel<AvatarRenderState> 不满足 EntityModel<? super S>
// → EntityModel<LivingEntityRenderState> 空模型占位（模型面仅 vanilla submit 链消费，
// 本类自定义几何完全旁路，262 树构造器实证）。
//? if >=26.2 {
/*public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, YsmPreviewRenderState, EntityModel<LivingEntityRenderState>> implements IGeoRenderer<T> {

    // render-state 化（1.21.2+）：vanilla dispatch 链 createRenderState(entity,partialTick)→
    // render(state) 丢失实体引用，本类经 extractRenderState 暂存三元组供 renderEntityWithTexture/
    // setupRotations/事件桥使用（复用单 state 实例，EntityRenderer.reusedState 同款生命周期）。
    // 26.2 预览抽取窗内另捕获 animatable 入 state（PiP 绘制期 submit 消费；
    // 实体→animatable 无反查表，帧内窗式暂存=最小通路），非窗提取恒 null。
    protected TEntity ysmEntity;

    protected YsmPreviewRenderState ysmState;

    protected float ysmPartialTick;

    @Override
    public YsmPreviewRenderState createRenderState() {
        return new YsmPreviewRenderState();
    }

    @Override
    public void extractRenderState(TEntity entity, YsmPreviewRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.ysmEntity = entity;
        this.ysmState = state;
        this.ysmPartialTick = partialTick;
        state.ysmAnimatable = ModelPreviewRenderer.takePipAnimatable262();
    }

    @Override
    public ResourceLocation getTextureLocation(YsmPreviewRenderState state) {
        // vanilla dispatch 永不触发（Static 驱动路径，CustomPlayerRenderer.render/submit 直调）；
        // 纹理由 capability/t.getTextureLocation() 提供
        return net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
    }*/
//?}
//? if >=1.21.2 && <21.9 {
/*    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();
*/
//?}
//? if >=21.9 {
/*    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();
*/
//?}
//? if <1.21.2 {
    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();
//?}

    public Matrix4f dispatchedMat = new Matrix4f();

    public Matrix4f renderEarlyMat = new Matrix4f();

// 26.2 submit-dag：RTB 槽位承载 collector（语义同 26.1 立即缓冲槽位）
//? if <26.2
    public MultiBufferSource rtb;

//? if >=26.2
    /*public SubmitNodeCollector rtb;*/

    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    //? if <1.17 {
    // public GeoReplacedEntityRenderer(net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher) {
    //     super(dispatcher, new PlayerModel(0.0f, true), 0.5f);
    //     this.rtb = null;
    // }
    //? } else {
    //? if <1.21.2 {
    public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        this.rtb = null;
    }
    //?}
    //? if >=1.21.2 && <26.2 {
    /*public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        this.rtb = null;
    }*/
    //?}
    //? if >=26.2 {
    /*public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        // M 界=EntityModel<LivingEntityRenderState>（S=YsmPreviewRenderState 非 Avatar，
        // PlayerModel 不满足界）；模型面仅 vanilla submit 链消费，本类自定义几何完全旁路
        // → 无部件空模型占位（EntityModel 无抽象方法，ModelPart 空立方/空子件，26.2 源实证）
        super(context, new EntityModel<LivingEntityRenderState>(new net.minecraft.client.model.geom.ModelPart(java.util.List.of(), java.util.Map.of())) {
        }, 0.5f);
        this.rtb = null;
    }*/
    //?}
    //? }

    public static int packOverlayCoords(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.currentModelRenderCycle = cycle;
    }

    //? if <26.2 {
    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
        this.renderEarlyMat.set(MatrixBridge.pose(poseStack.last()));
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }
    //?}

    //? if >=26.2 {
    /*@Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, SubmitNodeCollector bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.renderEarlyMat.set(MatrixBridge.pose(poseStack.last()));
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }*/
    //?}

    //? if <26.2 {
    public void renderEntity(T t, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderEntityWithTexture(t, null, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    //?}

    //? if >=26.2 {
    /*public void renderEntity(T t, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        renderEntityWithTexture(t, null, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }*/
    //?}

    //? if <26.2 {
    public void renderEntityWithTexture(T t, @Nullable ResourceLocation resourceLocation, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight) {
        Direction bedOrientation;
        boolean fireRenderEvents = !ModelPreviewRenderer.isPreview();
        // 1.21.2+ 本类为 Static 驱动路径（不走 vanilla dispatcher），state 自建：
        // createRenderState(entity,partialTick) 抽取+暂存，供 firePre/super.render(桥内)/
        // setupRotations 事件链使用
        //? if >=1.21.2 {
        /*this.ysmEntity = t.getEntity();
        this.ysmState = this.createRenderState(this.ysmEntity, partialTick);
        this.ysmPartialTick = partialTick;*/
        //?}
        //? if <1.21.2 {
        if (fireRenderEvents && RenderLivingBridge.firePre(t.getEntity(), this, partialTick, poseStack, multiBufferSource, packedLight)) {
            return;
        }
        //?}
        //? if >=1.21.2 {
        /*if (fireRenderEvents && RenderLivingBridge.firePre(t.getEntity(), this.ysmState, this, partialTick, poseStack, multiBufferSource, packedLight)) {
            return;
        }*/
        //?}
        AnimationEvent<?> event = t.processAnimation(partialTick);
        TEntity entity = t.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            EntityModelData modelData = event.getModelData();
            // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
            this.dispatchedMat.set(MatrixBridge.pose(poseStack.last()));
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            if (entity.getPose() == Pose.SLEEPING && (bedOrientation = entity.getBedOrientation()) != null) {
                float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                poseStack.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
            }
            setupRotations(entity, poseStack, modelData.lerpedAge, modelData.lerpBodyRot, partialTick);
            if (t.getEntity().getVehicle() != null) {
                VehicleCapability.get(t.getEntity().getVehicle()).ifPresent(cap -> {
                    Vector3f vector3f = cap.getExpressionOffset();
                    if (vector3f != null) {
                        // 1.16.5~1.19.2 moj Quaternion(x,y,z,degrees=false) 与 joml rotateZYX(z,y,x) 同为
                    // Rx·Ry·Rz 列向量约定（两版源码推导对照），单位四元数 conj==invert
                    //? if <1.17 {
                    // com.mojang.math.Quaternion vehicleRot = new com.mojang.math.Quaternion(vector3f.z, 0.0f, vector3f.x, false);
                    // vehicleRot.conj();
                    // poseStack.mulPose(vehicleRot);
                    //? }
                    //? if >=1.17 && <1.19.3 {
                    /*
                    com.mojang.math.Quaternion vehicleRot = new com.mojang.math.Quaternion(vector3f.z, 0.0f, vector3f.x, false);
                    vehicleRot.conj();
                    poseStack.mulPose(vehicleRot);
                     *///? }
                    //? if >=1.19.3 {
                    poseStack.mulPose(new Quaternionf().rotateZYX(vector3f.z, 0.0f, vector3f.x).invert());
                    //? }
                    }
                });
            }
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0.0f, 0.01f, 0.0f);
            AnimatedGeoModel animatedGeoModel = t.getCurrentModel();
            int textureIndex = resourceLocation == null ? t.getTextureIndex() : 0;
            // 1.21.2+ isBodyVisible(S)=!state.isInvisible（vanilla-1.21.3
            // LivingEntityRenderer.java:137-139）→ 实体面等价 !entity.isInvisible()
            //? if <1.21.2
            RenderType renderType = getRenderType(resourceLocation == null ? t.getTextureLocation() : resourceLocation, isBodyVisible(entity) && !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), t.getCurrentModel().getGeoModel().isTranslucentTexture(textureIndex));
            //? if >=1.21.2
            /*RenderType renderType = getRenderType(resourceLocation == null ? t.getTextureLocation() : resourceLocation, !entity.isInvisible() && !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), t.getCurrentModel().getGeoModel().isTranslucentTexture(textureIndex));*/
            boolean useExtraPlayer = t.isRenderLayersFirst();
            Color color = getRenderColor(t, partialTick, poseStack, multiBufferSource, null, packedLight);
            renderWithBone(animatedGeoModel, t, partialTick, poseStack, multiBufferSource, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            if (useExtraPlayer && !entity.isSpectator()) {
                render(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            if (renderType != null) {
                renderWithBoneAndRenderType(animatedGeoModel, t, partialTick, renderType, poseStack, multiBufferSource, textureIndex, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            }
            if (!useExtraPlayer && !entity.isSpectator()) {
                render(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            poseStack.popPose();
        }
        //? if <1.21.2
        ((LivingEntityRendererAccessor) this).tlm$renderNameTag(entity, entityYaw, partialTick, poseStack, multiBufferSource, packedLight);
        //? if >=1.21.2 && <21.9 {
        /*((LivingEntityRendererAccessor) this).tlm$renderNameTag(this.ysmState, poseStack, multiBufferSource, packedLight);*/
        //?}
        // 1.21.9+ vanilla renderNameTag(S,...) 删（submitNameTag/SubmitNodeCollector 换代），
        // 名牌交由 vanilla submitNameTag 默认链，Static 预览路径不再桥接（功能债同批入账）
        if (fireRenderEvents) {
            //? if <1.21.2
            RenderLivingBridge.firePost(entity, this, partialTick, poseStack, multiBufferSource, packedLight);
            //? if >=1.21.2 {
            /*RenderLivingBridge.firePost(entity, this.ysmState, this, partialTick, poseStack, multiBufferSource, packedLight);*/
            //?}
        }
    }
    //?}

    // 26.2 submit-dag collector 形（twin of renderEntityWithTexture）：
    // - 类型换代 MultiBufferSource→SubmitNodeCollector（RenderPlayerEvent 携带，
    //   neoforge-26.2 RenderPlayerEvent.java:30 实证）
    // - 名牌 21.9+ 已退回 vanilla submitNameTag 默认链，此处不桥接
    // - firePre/firePost 传真 collector（Static 预览路径 collector=null 消费端不可用之债不变）
    //? if >=26.2 {
    /*public void renderEntityWithTexture(T t, @Nullable ResourceLocation resourceLocation, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector multiBufferSource, int packedLight) {
        Direction bedOrientation;
        boolean fireRenderEvents = !ModelPreviewRenderer.isPreview();
        this.ysmEntity = t.getEntity();
        this.ysmState = this.createRenderState(this.ysmEntity, partialTick);
        this.ysmPartialTick = partialTick;
        if (fireRenderEvents && RenderLivingBridge.firePre(t.getEntity(), this.ysmState, this, partialTick, poseStack, multiBufferSource, packedLight)) {
            return;
        }
        AnimationEvent<?> event = t.processAnimation(partialTick);
        TEntity entity = t.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            EntityModelData modelData = event.getModelData();
            this.dispatchedMat.set(MatrixBridge.pose(poseStack.last()));
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            if (entity.getPose() == Pose.SLEEPING && (bedOrientation = entity.getBedOrientation()) != null) {
                float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                poseStack.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
            }
            setupRotations(entity, poseStack, modelData.lerpedAge, modelData.lerpBodyRot, partialTick);
            if (t.getEntity().getVehicle() != null) {
                VehicleCapability.get(t.getEntity().getVehicle()).ifPresent(cap -> {
                    Vector3f vector3f = cap.getExpressionOffset();
                    if (vector3f != null) {
                        poseStack.mulPose(new Quaternionf().rotateZYX(vector3f.z, 0.0f, vector3f.x).invert());
                    }
                });
            }
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0.0f, 0.01f, 0.0f);
            AnimatedGeoModel animatedGeoModel = t.getCurrentModel();
            int textureIndex = resourceLocation == null ? t.getTextureIndex() : 0;
            RenderType renderType = getRenderType(resourceLocation == null ? t.getTextureLocation() : resourceLocation, !entity.isInvisible() && !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), t.getCurrentModel().getGeoModel().isTranslucentTexture(textureIndex));
            boolean useExtraPlayer = t.isRenderLayersFirst();
            Color color = getRenderColor(t, partialTick, poseStack, multiBufferSource, null, packedLight);
            renderWithBone(animatedGeoModel, t, partialTick, poseStack, multiBufferSource, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            if (useExtraPlayer && !entity.isSpectator()) {
                render(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            if (renderType != null) {
                renderWithBoneAndRenderType(animatedGeoModel, t, partialTick, renderType, poseStack, multiBufferSource, textureIndex, null, packedLight, packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick)), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            }
            if (!useExtraPlayer && !entity.isSpectator()) {
                render(t, partialTick, poseStack, multiBufferSource, packedLight, event, modelData);
            }
            poseStack.popPose();
        }
        if (fireRenderEvents) {
            RenderLivingBridge.firePost(entity, this.ysmState, this, partialTick, poseStack, multiBufferSource, packedLight);
        }
    }
    *///?}

    //? if <26.2 {
    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLightIn, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.render(poseStack, bufferSource, packedLightIn, entity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick, data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch);
        }
    }
    //?}

    //? if >=26.2 {
    /*public void render(T entity, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLightIn, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.render(poseStack, bufferSource, packedLightIn, entity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick, data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch);
        }
    }
    *///?}

    public float getHurtOverlayProgress(TEntity entity, float partialTick) {
        return 0.0f;
    }

    public void preRenderCallback(TEntity entity, PoseStack poseStack, float partialTick) {
    }

    public void setupRotations(TEntity tentity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        int t = tentity.deathTime;
        boolean zIsAutoSpinAttack = tentity.isAutoSpinAttack();
        if (t > 0) {
            tentity.deathTime = 0;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, false);
        }
        if (tentity.onClimbable()) {
            Optional<BlockPos> lastClimbablePos = tentity.getLastClimbablePos();
            if (lastClimbablePos.isPresent()) {
                //? if <1.17
                // Optional<Direction> optionalValue = tentity.level.getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                //? if >=1.17 && <1.18.2
                /*Optional<Direction> optionalValue = tentity.level.getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);*/
                //? if >=1.18.2 && <1.20
                /*Optional<Direction> optionalValue = tentity.getLevel().getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);*/
                //? if >=1.20
                Optional<Direction> optionalValue = tentity.level().getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (optionalValue.isPresent()) {
                    rotationYaw = optionalValue.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }
        // 1.20.5+ setupRotations 增 scale 尾参（vanilla-1.20.6 LivingEntityRenderer.java:175，
        // render 内调值 = entity.getScale()，:91-95 实证）；1.21.2 render-state 化改
        // setupRotations(S, PoseStack, age, rot)（vanilla-1.21.3 LivingEntityRenderer.java:160，
        // 无 partialTick/scale——scale 进 state.ageScale，partialTick 由 state 暂存面承接）
        //? if <1.20.5
        super.setupRotations(tentity, poseStack, ageInTicks, rotationYaw, partialTicks);
        //? if >=1.20.5 && <1.21.2
        /*super.setupRotations(tentity, poseStack, ageInTicks, rotationYaw, partialTicks, tentity.getScale());*/
        //? if >=1.21.2
        /*super.setupRotations(this.ysmState, poseStack, ageInTicks, rotationYaw);*/
        if (t > 0) {
            tentity.deathTime = t;
        }
        if (zIsAutoSpinAttack) {
            ((LivingEntityAccessor) tentity).invokeSetLivingEntityFlag(4, true);
        }
    }

    // 1.21.2 shouldShowName 增相机距离尾参（vanilla-1.21.3 EntityRenderer.java:202）
    //? if <1.21.2
    @Override
    public boolean shouldShowName(TEntity entity) {
        double d = entity.isDiscrete() ? 32.0d : 64.0d;
        return this.entityRenderDispatcher.distanceToSqr(entity) < d * d && entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && Minecraft.renderNames();
    }
    //? if >=1.21.2 {
    /*@Override
    public boolean shouldShowName(TEntity entity, double distanceToCameraSq) {
        double d = entity.isDiscrete() ? 32.0d : 64.0d;
        return distanceToCameraSq < d * d && entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && Minecraft.renderNames();
    }*/
    //?}

    public final boolean addLayerRenderer(GeoLayerRenderer<T> layerRenderer) {
        return this.layerRenderers.add(layerRenderer);
    }

    //? if <26.2 {
    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.rtb = bufferSource;
    }
    //?}

    //? if >=26.2 {
    /*@Override
    public SubmitNodeCollector getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(SubmitNodeCollector bufferSource) {
        this.rtb = bufferSource;
    }*/
    //?}
}
