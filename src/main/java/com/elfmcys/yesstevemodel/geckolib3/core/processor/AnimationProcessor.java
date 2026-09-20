package com.elfmcys.yesstevemodel.geckolib3.core.processor;

import com.elfmcys.yesstevemodel.audio.AudioPlayerManager;
import com.elfmcys.yesstevemodel.geckolib3.core.manager.AnimationData;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.IForeignVariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.storage.VariableStorage;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.elfmcys.yesstevemodel.geckolib3.core.util.EulerNlerpScratch;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.BoneTransformProvider;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.TransitionVector3f;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.molang.runtime.Struct;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
// RandomSource/Xoroshiro/RandomSupport 为 1.17/1.18+ 类；1.16.5 用 java.util.Random
// （new Random() 无参构造自带唯一种子，语义对应 RandomSupport.generateUniqueSeed()）
//? if <1.17 {
// import java.util.Random;
//? }
//? if >=1.17 && <1.18.2 {
// import net.minecraft.world.level.levelgen.RandomSource;
// import net.minecraft.world.level.levelgen.WorldgenRandom;
//? }
//? if >=1.18.2 && <1.19 {
// import net.minecraft.world.level.levelgen.RandomSource;
// import net.minecraft.world.level.levelgen.RandomSupport;
// import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
//? }
//? if >=1.19 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
//? }
// ^ RandomSource 为 1.17+ 类（1171 实证有 levelgen.RandomSource 但无 XoroshiroRandomSource/
//   RandomSupport，merged jar javap；WorldgenRandom(long) implements RandomSource 承接 1171 段）；
//   1.16.5 用 java.util.Random（new Random() 无参构造自带唯一种子，语义对应 generateUniqueSeed）
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class AnimationProcessor<TEntity extends Entity> {

    private static final int ROAMING_STRUCT_NAME = StringPool.computeIfAbsent("roaming");

    private final AnimatableEntity<TEntity> animatable;

    private final ReferenceArrayList<BoneTopLevelSnapshot> bones = new ReferenceArrayList<>();

    private Object2ReferenceMap<String, List<IValue>> initExpressions = Object2ReferenceMaps.emptyMap();

    private final Int2ReferenceOpenHashMap<BoneTopLevelSnapshot> boneById = new Int2ReferenceOpenHashMap<>();

    private final ArrayDeque<BoneTopLevelSnapshot> modelRendererList = new ArrayDeque<>();

    private final VariableStorage animationStorage = new VariableStorage();

    private final AudioPlayerManager audioPlayerManager = new AudioPlayerManager();

    // 1171 无 XoroshiroRandomSource/RandomSupport（merged jar 实证）→ WorldgenRandom(long) 实现 RandomSource；
    // 1182 RandomSupport 无 generateUniqueSeed（1.19.2+ 才有）→ nanoTime 种子，语义同唯一种子
    //? if <1.17 {
    /*private final Random random = new Random();
     *///?}
    //? if >=1.17 && <1.18 {
    /*private final RandomSource random = new WorldgenRandom(System.nanoTime());*/
    //? }
    //? if >=1.18 && <1.18.2 {
    /*// 1.18.0 WorldgenRandom 构造已改收 RandomSource（1.17.1 收 long seed）
    private final RandomSource random = new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(System.nanoTime()));*/
    //? }
    //? if >=1.18.2 && <1.19 {
    /*private final RandomSource random = new XoroshiroRandomSource(System.nanoTime());*/
    //? }
    //? if >=1.19 {
    private final RandomSource random = new XoroshiroRandomSource(RandomSupport.generateUniqueSeed());
    //?}

    private final ConcurrentLinkedQueue<PendingExpression> pendingExpressions = new ConcurrentLinkedQueue<>();

    private float lastAudioTickTime = 0.0f;

    private boolean needsInit = false;

    // tickAnimation 给每帧 forEachTransform 复用同一个 Consumer
    private final Consumer<BoneTransformProvider> transformConsumer = this::applyTransform;
    private ExpressionEvaluator<AnimationContext<?>> currentEvaluator;
    private float currentSeekTime;
    private boolean currentDeprecatedMode;
    private final EulerNlerpScratch rotScratch = new EulerNlerpScratch();

    public AnimationProcessor(AnimatableEntity<TEntity> animatable) {
        this.animatable = animatable;
    }

    public void tickAnimation(AnimationEvent<AnimatableEntity<TEntity>> event, AnimationContext<?> context, boolean z, boolean z2) {
        context.setStorage(this.animationStorage);
        context.setRandom(this.random);
        context.setAudioPlayerManager(this.audioPlayerManager);
        ExpressionEvaluator<AnimationContext<?>> evaluator = ExpressionEvaluator.evaluator(context);
        float seekTime = event.currentTick;
        if (seekTime - this.lastAudioTickTime >= 1200.0f) {
            this.audioPlayerManager.tick();
            this.lastAudioTickTime = seekTime;
        } else if (this.lastAudioTickTime > seekTime) {
            this.lastAudioTickTime = seekTime;
        }
        preProcess(evaluator);
        AnimationData manager = this.animatable.getAnimationData();
        this.currentEvaluator = evaluator;
        this.currentSeekTime = seekTime;
        for (IAnimationController controller : manager.getAnimationControllers()) {
            if (this.needsInit) {
                controller.init(this.bones, this.initExpressions);
            }
            if (z) {
                controller.process(event, evaluator, z2);
            }
            this.currentDeprecatedMode = controller.isDeprecatedMode();
            controller.forEachTransform(this.transformConsumer);
        }
        this.currentEvaluator = null;
        this.needsInit = false;
        Iterator<BoneTopLevelSnapshot> iterator = this.modelRendererList.iterator();
        while (iterator.hasNext()) {
            BoneTopLevelSnapshot topLevelSnapshot = iterator.next();
            boolean runningAnimation = false;
            if (topLevelSnapshot.isCurrentlyRunningRotationAnimation) {
                runningAnimation = true;
                topLevelSnapshot.isCurrentlyRunningRotationAnimation = false;
                topLevelSnapshot.prevRotation = null;
            } else {
                if (topLevelSnapshot.prevRotation == null) {
                    topLevelSnapshot.prevRotation = new Vector3f(topLevelSnapshot.rotation);
                }
                float percentageReset = (seekTime - topLevelSnapshot.mostRecentResetRotationTick) / manager.getResetSpeed();
                if (percentageReset < 1.0f) {
                    runningAnimation = true;
                    MathUtil.nlerpEulerAngles(percentageReset, topLevelSnapshot.prevRotation, MathUtil.ZERO, topLevelSnapshot.bone.getInitialRotation(), topLevelSnapshot.rotation, this.rotScratch);
                } else {
                    topLevelSnapshot.rotation.set(MathUtil.ZERO);
                }
            }
            if (topLevelSnapshot.isCurrentlyRunningPositionAnimation) {
                runningAnimation = true;
                topLevelSnapshot.isCurrentlyRunningPositionAnimation = false;
                topLevelSnapshot.prevPosition = null;
            } else {
                if (topLevelSnapshot.prevPosition == null) {
                    topLevelSnapshot.prevPosition = new Vector3f(topLevelSnapshot.position);
                }
                float percentageReset = (seekTime - topLevelSnapshot.mostRecentResetPositionTick) / manager.getResetSpeed();
                if (percentageReset < 1.0f) {
                    runningAnimation = true;
                    MathUtil.lerpValues(percentageReset, topLevelSnapshot.prevPosition, MathUtil.ZERO, topLevelSnapshot.position);
                } else {
                    topLevelSnapshot.position.set(0.0f, 0.0f, 0.0f);
                }
            }
            if (topLevelSnapshot.isCurrentlyRunningScaleAnimation) {
                runningAnimation = true;
                topLevelSnapshot.isCurrentlyRunningScaleAnimation = false;
                topLevelSnapshot.prevScale = null;
            } else {
                if (topLevelSnapshot.prevScale == null) {
                    topLevelSnapshot.prevScale = new Vector3f(topLevelSnapshot.scale);
                }
                float percentageReset = (seekTime - topLevelSnapshot.mostRecentResetScaleTick) / manager.getResetSpeed();
                if (percentageReset < 1.0f) {
                    runningAnimation = true;
                    MathUtil.lerpValues(percentageReset, topLevelSnapshot.prevScale, MathUtil.ONE, topLevelSnapshot.scale);
                } else {
                    topLevelSnapshot.scale.set(1.0f, 1.0f, 1.0f);
                }
            }
            topLevelSnapshot.reset();
            if (!runningAnimation) {
                topLevelSnapshot.isCurrentlyRunningAnimation = false;
                iterator.remove();
            }
        }
        context.setPlaybackFlags(null);
        context.setAnimationControllerContext(null);
        postProcess(evaluator);
    }

    private void applyTransform(BoneTransformProvider provider) {
        final BoneTopLevelSnapshot snapshot = provider.getBoneTarget();
        if (!snapshot.isCurrentlyRunningAnimation) {
            snapshot.isCurrentlyRunningAnimation = true;
            this.modelRendererList.add(snapshot);
        }
        // GeoEntity async evaluator NPE 守卫（跳帧降级，非吞异常）：
        // currentEvaluator 在 tickAnimation 中段置位/复位，正常单线程恒非 null；async 覆写
        // 竞态（GeoEntity.submitAsyncUpdate 覆写 in-flight modelFuture）下另一 worker 已复位
        // 时本 worker 的 forEachTransform 读到 null，对 evaluator 求值即 NPE。守卫后本帧跳过
        // 该 provider 变换（骨保留上一帧值），动画由 GeoEntity 侧同步降级路径继续推进。
        final ExpressionEvaluator<AnimationContext<?>> evaluator = this.currentEvaluator;
        if (evaluator == null) {
            return;
        }
        final float seekTime = this.currentSeekTime;

        TransitionVector3f rot = provider.getRotation(evaluator);
        if (rot != null) {
            Vector3f vector3f = snapshot.currentValue;
            if (!snapshot.isCurrentlyRunningRotationAnimation) {
                snapshot.isCurrentlyRunningRotationAnimation = true;
                snapshot.rotation.set(0.0f, 0.0f, 0.0f);
            }
            snapshot.mostRecentResetRotationTick = seekTime;
            if (this.currentDeprecatedMode) {
                vector3f.add(rot);
                snapshot.rotation.set(vector3f);
            } else {
                rot.applyRotationBlendTo(snapshot.rotation, snapshot.bone.getInitialRotation(), this.rotScratch);
                vector3f.set(snapshot.rotation);
            }
        }

        TransitionVector3f pos = provider.getPosition(evaluator);
        if (pos != null) {
            if (!snapshot.isCurrentlyRunningPositionAnimation) {
                snapshot.isCurrentlyRunningPositionAnimation = true;
                snapshot.position.set(0.0f, 0.0f, 0.0f);
            }
            snapshot.mostRecentResetPositionTick = seekTime;
            pos.applyLinearBlendTo(snapshot.position);
        }

        TransitionVector3f scale = provider.getScale(evaluator);
        if (scale != null) {
            if (!snapshot.isCurrentlyRunningScaleAnimation) {
                snapshot.isCurrentlyRunningScaleAnimation = true;
                snapshot.scale.set(1.0f, 1.0f, 1.0f);
            }
            snapshot.mostRecentResetScaleTick = seekTime;
            scale.applyLinearBlendTo(snapshot.scale);
        }
    }

    @Nullable
    public IBone getBone(int i) {
        BoneTopLevelSnapshot renderer = this.boneById.get(i);
        return renderer != null ? renderer.bone : null;
    }

    public void reset() {
        this.boneById.clear();
        this.modelRendererList.clear();
        this.bones.clear();
        this.animationStorage.initialize(null);
        this.initExpressions = Object2ReferenceMaps.emptyMap();
        this.pendingExpressions.clear();
        this.audioPlayerManager.stopAll();
    }

    public void initBones(AnimatedGeoModel model, Object2ReferenceMap<String, List<IValue>> object2ReferenceMap) {
        reset();
        if (!model.bones().isEmpty()) {
            this.bones.ensureCapacity(model.bones().size());
            this.bones.add(null);
            int boneId = model.getGeoModel().bones.get(0).getBoneId();
            Int2ReferenceMaps.fastForEach(model.bones(), entry -> {
                BoneTopLevelSnapshot boneTopLevelSnapshot = new BoneTopLevelSnapshot(entry.getValue());
                this.boneById.put(entry.getValue().getBoneId(), boneTopLevelSnapshot);
                if (entry.getIntKey() == boneId) {
                    this.bones.set(0, boneTopLevelSnapshot);
                } else {
                    this.bones.add(boneTopLevelSnapshot);
                }
            });
        }
        this.needsInit = true;
        this.initExpressions = object2ReferenceMap;
    }

    public void setRoamingProperties(@Nullable Struct struct) {
        if (struct != null) {
            this.animationStorage.setScoped(ROAMING_STRUCT_NAME, struct);
        }
    }

    public boolean isDisabled() {
        return this.bones.isEmpty();
    }

    private void preProcess(ExpressionEvaluator<AnimationContext<?>> evaluator) {
        Iterator<PendingExpression> it = this.pendingExpressions.iterator();
        while (it.hasNext()) {
            PendingExpression next = it.next();
            if (next.executeBeforeAnimation) {
                postProcess(next, evaluator);
                it.remove();
            }
        }
    }

    private void postProcess(ExpressionEvaluator<AnimationContext<?>> evaluator) {
        Iterator<PendingExpression> it = this.pendingExpressions.iterator();
        while (it.hasNext()) {
            PendingExpression next = it.next();
            if (!next.executeBeforeAnimation) {
                postProcess(next, evaluator);
                it.remove();
            }
        }
    }

    private void postProcess(PendingExpression value, ExpressionEvaluator<AnimationContext<?>> evaluator) {
        String result;
        try {
            Object ret;
            try {
                evaluator.entity().setIsClientSide(value.isClientPlayer);
                ret = value.IValue.evalSafe(evaluator);
            } catch (Throwable th) {
                ret = result = "Error: " + th.getMessage();
                evaluator.entity().setIsClientSide(false);
            }
            if (value.callback == null) {
                evaluator.entity().setIsClientSide(false);
                return;
            }
            result = ret == null ? "null" : ret instanceof String ? "'" + ret + "'" : ret.toString();
            evaluator.entity().setIsClientSide(false);
            value.callback.accept(result);
        } catch (Throwable th2) {
            evaluator.entity().setIsClientSide(false);
            throw th2;
        }
    }

    public void execute(IValue value, boolean isClientPlayer, boolean executeBeforeAnimation, @Nullable Consumer<String> resultConsumer) {
        this.pendingExpressions.add(new PendingExpression(value, isClientPlayer, executeBeforeAnimation, resultConsumer));
    }

    public IForeignVariableStorage getPublicVariableStorage() {
        return this.animationStorage;
    }

    public void forEachPropertyName(Consumer<String> consumer) {
        this.animationStorage.forEachPropertyName(consumer);
    }

    private static final class PendingExpression {
        final IValue IValue;
        final boolean isClientPlayer;
        final boolean executeBeforeAnimation;
        final Consumer<String> callback;

        PendingExpression(IValue IValue, boolean isClientPlayer, boolean executeBeforeAnimation, Consumer<String> callback) {
            this.IValue = IValue;
            this.isClientPlayer = isClientPlayer;
            this.executeBeforeAnimation = executeBeforeAnimation;
            this.callback = callback;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PendingExpression)) {
                return false;
            }
            PendingExpression other = (PendingExpression) obj;
            return this.isClientPlayer == other.isClientPlayer && this.executeBeforeAnimation == other.executeBeforeAnimation
                    && Objects.equals(this.IValue, other.IValue) && Objects.equals(this.callback, other.callback);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.IValue, this.isClientPlayer, this.executeBeforeAnimation, this.callback);
        }

        @Override
        public String toString() {
            return "PendingExpression[IValue=" + this.IValue + ", isClientPlayer=" + this.isClientPlayer
                    + ", executeBeforeAnimation=" + this.executeBeforeAnimation + ", callback=" + this.callback + "]";
        }
    }
}