package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.audio.*;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangEventDispatcher;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import rip.ysm.compat.oculus.OculusCompat;
import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.client.animation.molang.MolangWatchRegistry;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.AnimationProcessor;
import com.elfmcys.yesstevemodel.util.*;
import com.elfmcys.yesstevemodel.util.log.ChatLogger;
import com.elfmcys.yesstevemodel.util.log.ILogger;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

public abstract class GeoEntity<T extends Entity> extends AnimatableEntity<T> {
    private String modelId;

    private ModelAssembly modelAssembly;

    private ModelWrapper renderShape;

    private boolean loaded;

    private int updateTicks;

    @Nullable
    private PhysicsManager bones;

    @Nullable
    private MolangWatchRegistry boneLookup;

    @Nullable
    private List<IValue> renderLayers;

    @Nullable
    private Future<AnimationEvent<?>> modelFuture;

    /** 最近一次 async 提交的 partialTick（同步降级重算用，仅渲染线程读写） */
    private float lastSubmittedPartialTick;

    @Nullable
    public abstract GeoEntity.ModelWrapper buildRenderShape(ModelAssembly modelAssembly, boolean isDefault);

    public abstract GeoModel getAnimationProcessor();

    public GeoEntity(T t, boolean registerWithCache) {
        super(t);
        this.modelId = "default";
        if (registerWithCache) {
            EntityRenderCache.register(this);
        }
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        if (ModelPreviewRenderer.isFirstPerson() || ModelPreviewRenderer.isExtraPlayer()) {
            return this.physicsManager;
        }
        if (this.bones == null) {
            this.bones = new PhysicsManager();
        }
        return this.bones;
    }

    @Nullable
    public List<IValue> getRenderLayers() {
        return this.renderLayers;
    }

    public void setBoneLookup(@Nullable MolangWatchRegistry watchRegistry) {
        this.boneLookup = watchRegistry;
    }

    @Override
    public void setupAnim(float seekTime, boolean isFirstPerson) {
        super.setupAnim(seekTime, isFirstPerson);
        if (this.boneLookup != null) {
            AnimationProcessor<T> processor = getEvaluationContext();
            processor.execute(evaluator -> {
                this.boneLookup.evauatePreAnimation(evaluator);
                return null;
            }, false, true, null);
            processor.execute(it -> {
                this.boneLookup.evaluatePostAnimation(it);
                return null;
            }, false, false, null);
        }
    }

    public void tickModel() {
        if (this.updateTicks < this.entity.tickCount) {
            refreshModel();
            this.updateTicks = this.entity.tickCount;
        }
    }

    public final ModelAssembly getModelAssembly() {
        return this.modelAssembly;
    }

    public final void setModelId(String str) {
        this.modelId = str;
        refreshModel();
    }

    private void refreshModel() {
        Optional<ModelAssembly> modelContext = ClientModelManager.getModelContext(this.modelId);
        if (modelContext.isPresent()) {
            ModelAssembly assembly = modelContext.get();
            if (this.renderShape == null || this.renderShape.isDefault || assembly != this.renderShape.context) {
                this.renderShape = buildRenderShape(assembly, false);
            }
        } else {
            ModelAssembly modelAssembly = ClientModelManager.getLocalModelContext();
            // 本地默认模型上下文可能尚未就绪（builtin 模型异步解析中，getLocalModelContext 可返回
            // null，1.16.5 dev runClient 进世界同步 setModelId 实测 NPE 于 TexturedModelWrapper
            // 对 null modelAssembly 解引用）——null 时跳过本次构建，待模型就绪后的刷新路径再建。
            if (modelAssembly != null && (this.renderShape == null || !this.renderShape.isDefault || modelAssembly != this.renderShape.context)) {
                this.renderShape = buildRenderShape(modelAssembly, true);
            }
        }
        if (this.renderShape != null) {
            if ((this.renderShape.context != this.modelAssembly || this.renderShape.isDefault != this.loaded) && this.renderShape.isValid()) {
                this.modelAssembly = this.renderShape.context;
                this.loaded = this.renderShape.isDefault;
                onModelLoaded(this.modelAssembly);
                initAnimationControllers(getAnimationProcessor(), this.modelAssembly.getExpressionCache().getEvents());
                return;
            }
            return;
        }
        if (this.modelAssembly != null) {
            clearModel();
        }
    }

    public final ModelWrapper getRenderShape() {
        return this.renderShape;
    }

    public void onModelLoaded(ModelAssembly modelAssembly) {
        this.renderShape.audioProvider = AudioStreamCache.getOrCreateProvider(modelAssembly);
        this.renderLayers = modelAssembly.getExpressionCache().getEvents().get(MolangEventDispatcher.DEFER);
    }

    public void clearModel() {
        this.modelAssembly = null;
        this.renderLayers = null;
        this.renderShape = null;
        this.loaded = false;
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.bones = null;
        this.updateTicks = 0;
    }

    public void resetModel() {
        this.modelId = "default";
        this.modelInitialized = false;
        clearModel();
    }

    public final String getModelId() {
        return this.modelId;
    }

    public boolean isModelReady() {
        return this.renderShape != null && !this.renderShape.isDefault && this.renderShape.isValid();
    }

    @Override
    public boolean shouldSkipAnimation(AnimationEvent<?> event) {
        return event.isFirstPerson() || OculusCompat.isPBRActive();
    }

    @Override
    @Nullable
    public final IValue resolveExpression(String str) {
        return getModelAssembly().getExpressionCache().getFunctions().get(str);
    }

    @Override
    public Optional<IAudioStreamFactory> getAudioStreamFactory(String str) {
        AudioTrackData trackData;
        if (this.renderShape.audioProvider != null && (trackData = getModelAssembly().getExpressionCache().getSoundEffects().get(str)) != null && trackData.getData() != null && trackData.getCodec() != AudioCodec.UNDEFINED) {
            IAudioStreamProvider streamProvider = this.renderShape.audioProvider;
            return Optional.of(() -> {
                return streamProvider.createAudioStream(trackData);
            });
        }
        return Optional.empty();
    }

    @Override
    public ILogger getLogger() {
        if (AnimationDebugOverlay.isDebugActive()) {
            return ChatLogger.INSTANCE;
        }
        return null;
    }

    public void submitAsyncUpdate(float partialTick) {
        // 防 in-flight 覆写（GeoEntity async NPE 根因链①）：EntityRenderCache.tick 每帧调用本方法，
        // 但 awaitAsyncResult 只在实体渲染帧（processAnimation）/EntityRenderCache.clear 时消费
        // modelFuture——实体被视锥剔除/第一人称本体不渲染的帧里 modelFuture 悬空，下一帧无条件
        // 覆写引用会让两个 YSM Worker 并发进入同一 AnimationProcessor：tickAnimation:127 置
        // currentEvaluator、:139 复位 null，另一 worker 的 applyTransform 读到 null evaluator
        // 即 NPE（栈内 GeoEntity.lambda$submitAsyncUpdate$0 = GeoEntity.java:220 帧，dev 存量
        // 非 FPM 引入）。in-flight 时跳过本帧提交（上一帧结果仍会被渲染帧消费，动画不停摆）。
        if (this.modelFuture != null) {
            return;
        }
        this.lastSubmittedPartialTick = partialTick;
        UnsafeUtil.storeFence();
        this.modelFuture = YSMThreadPool.submitCallable(() -> {
            try {
                AnimationEvent<?> event = super.processAnimationImpl(partialTick, true);
                UnsafeUtil.storeFence();
                return event;
            } catch (Throwable th) {
                UnsafeUtil.storeFence();
                throw th;
            }
        });
    }

    /**
     * async 求值失败后的同步降级（渲染线程直调）：processAnimationImpl(false) 走同步路径
     * 重算本帧动画，异常继续上抛由调用方 null 守卫兜底——保证失败帧后动画状态机仍前进而
     * 非停摆（wasAnimationActiveLastTick/snapshot 标志位在同步路径上单线程自洽）。
     */
    @Nullable
    private AnimationEvent<?> fallbackSyncEvaluate() {
        return super.processAnimationImpl(this.lastSubmittedPartialTick, false);
    }

    @Override
    @Nullable
    public AnimationEvent<?> processAnimationImpl(float partialTick, boolean isFirstPerson) {
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
        if (isFirstPerson && this.modelFuture != null) {
            return awaitAsyncResult();
        }
        awaitAsyncResult();
        return super.processAnimationImpl(partialTick, isFirstPerson);
    }

    public AnimationEvent<?> awaitAsyncResult() {
        if (this.modelFuture != null) {
            AnimationEvent<?> event = null;
            try {
                event = this.modelFuture.get();
                UnsafeUtil.loadFence();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable th) {
                // 根因链②（GeoEntity async evaluator NPE）：原实现吞 Throwable 后返回 null，
                // 动画异常（含并发 evaluator NPE）只刷栈零痕迹、 bones snapshot 标志位被并发
                // 破坏后动画永久停摆。降级=同步重算一次动画（渲染线程上、无并发），结果仍
                // 作为本帧 event 供消费方使用——异常帧不吞、动画可自愈，调用方 null 守卫兜底。
                th.printStackTrace();
                event = this.fallbackSyncEvaluate();
            }
            this.modelFuture = null;
            return event;
        }
        return null;
    }

    public boolean supportsAsync() {
        return true;
    }

    public static class ModelWrapper {

        public final ModelAssembly context;

        public final boolean isDefault;

        @Nullable
        public IAudioStreamProvider audioProvider;

        public ModelWrapper(ModelAssembly modelAssembly, boolean isDefault) {
            this.context = modelAssembly;
            this.isDefault = isDefault;
        }

        public boolean isValid() {
            return true;
        }
    }
}