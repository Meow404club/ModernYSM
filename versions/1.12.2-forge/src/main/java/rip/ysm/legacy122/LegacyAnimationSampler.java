package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.BoneAnimation;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.file.AnimationFile;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * 离线动画采样器（M-U2 r3 语义A 最小档）。
 *
 * <p>主线预览动画链 = setCurrentAnimation(properties.preview_animation)
 * （PlayerModelScreen.java:531）→ AnimationController 逐骨关键帧求值 →
 * matrixData（=本线 boneParams 12 float/骨契约）。1.12.2 不缺解析只缺播放器
 * （共享 YSMClientMapper.buildAnimations 已产 Animation/BoneAnimation/BoneKeyFrame），
 * 本类按时间轴直接采样关键帧写 boneParams。
 *
 * <p>单位/符号已由解析端统一：旋转关键帧过 RotationValue.convert（度→弧度，
 * x/y 取负，RawBoneKeyFrame.createVector 调用点），采样输出可直接作为
 * LegacyModelTranslator 的 rotateX/Y/Z 弧度偏移——与主线
 * NativeModelRenderer.calculateBoneMatrix:356-358 消费端数学逐项相同。
 * 关键帧插值（常量/LINEAR/CATMULLROM）复用 BoneKeyFrame.evaluate 多态，
 * 不另写插值数学。
 *
 * <p>语义对齐主线：
 * <ul>
 * <li>动画名查找=除 fp_arm 外全部动画文件合并（ModelAssemblyFactory.java:62-71
 *     同式），查不到→返回 false（主线 hasAnimation()==false → PlayState.STOP，
 *     PlayerBaseAnimationPredicate.java:16-19=绑定位静像，无 idle 回退）。</li>
 * <li>loop 动画按 animationLength（tick，YSMClientMapper :604 length*20）取模循环；
 *     非 LOOP（PLAY_ONCE/HOLD_ON_LAST_FRAME）播完钳在 len 持末帧——主线仅 LOOP
 *     取模（AnimationControllerInstance.java:148-163），非 LOOP 经
 *     startEndingTransition min(t,len)（:211）+ InterpolationLookup:58 末帧兜底
 *     （转场/混合层不做=研究最小档，GUI 预览不可感知）。</li>
 * <li>molang 表达式关键帧走共享 ExpressionEvaluator（常量关键帧完全不触达求值器；
 *     表达式失败时 evalSafe 降级 0=该骨回中性位，不崩溃）。</li>
 * <li>并行槽常驻动画（fix-122-08sta-scale 追加，1710 蓝本 a13b89a 同款 twin）：
 *     pre_parallel0-7/parallel0-7 与主状态并排常驻施加（applyParallelSlots，主线
 *     PlayerAnimationController:51,77 registerParallelController 对位），模型包据此
 *     挂常驻隐藏件/表情槽。</li>
 * <li>骨骼隐藏双源（r3 追加）：消费侧=LegacyModelTranslator（offset9=hidden/
 *     offset10=skipChildren，父链子树继承，scale0 同判不可见——镜像
 *     NativeModelRenderer.calculateBoneMatrix:328-345）；写入侧=resetParams
 *     置中性 0 + applyDebugHide 验收钩子（env YSM_LEGACY122_HIDE_BONE_TEST）。
 *     静态源（GeoBone 三旗标）在共享 bake 即恒 false（主线同码
 *     YSMClientMapper:457），格式内亦无隐藏关键帧通道（rot/pos/scale 之外
 *     无通道）——两线对称，见交付报告。</li>
 * </ul>
 */
public final class LegacyAnimationSampler {

    private static final ExpressionEvaluator<?> EVALUATOR = ExpressionEvaluator.evaluator();
    private static final Vector3f VEC = new Vector3f();

    private LegacyAnimationSampler() {
    }

    /** 按名查动画（fp_arm 文件除外=主线 mainAnimations 合并序）。 */
    public static Animation findAnimation(ClientModelInfo bundle, String name) {
        if (bundle == null || name == null || name.isEmpty()) {
            return null;
        }
        MainModelData data = bundle.getMainModelData();
        Map<String, AnimationFile> files = data == null ? null : data.getAnimations();
        if (files == null) {
            return null;
        }
        for (Map.Entry<String, AnimationFile> entry : files.entrySet()) {
            if ("fp_arm".equals(entry.getKey())) {
                continue;
            }
            Animation anim = entry.getValue().getAnimations().get(name);
            if (anim != null) {
                return anim;
            }
        }
        return null;
    }

    /**
     * 采样：先重置全部骨到绑定位（中性），再施加常驻并行动画（fix-122-08sta-scale），
     * 最后按 tick 施加主状态动画。
     *
     * @param tick     动画时间轴（tick；调用方由 ms/50 推得，跨帧单调）
     * @return false=无此动画（params 已是绑定位+并行槽=主线 STOP 语义）
     */
    public static boolean sample(GeoModel model, float[] params, ClientModelInfo bundle,
                                 String animName, float tick) {
        resetParams(model, params);
        applyParallelSlots(model, params, bundle, tick);
        return applyAnimation(model, params, bundle, animName, tick);
    }

    /**
     * YSM 并行槽机制（主线 PlayerAnimationController.java:51,77 registerParallelController
     * "pre_parallel"/"parallel" 注册；ParallelProcessor 常驻求值）：动画文件内名为
     * pre_parallel0-7 / parallel0-7 的动画与主状态动画并排常驻播放，模型包用它挂
     * 常驻态——隐藏件（08_sta 的 Root_car/Root_car2 载具系在 pre_parallel1/2 恒
     * scale0/表达式选值）、表情槽（molang 变量选值，未定义变量经共享 evalSafe
     * 降级 0=隐藏，与主线同一 molang 运行时行为一致）。
     * 修复前此机制缺失→08_sta 载具骨停绑定位 scale1 全量绘制（quadsDrawn=25103
     * 十格巨影，/tmp/f08-122-run2.log 实证）。先并行槽后主状态：重叠骨主状态覆盖
     * （1710 蓝本同序；主线注册序 pre_parallel<main<parallel 为 parallel 赢——
     * 并行槽骨域与状态骨域不相交前提下无观察差，parallel_order_note 记档）。
     */
    private static void applyParallelSlots(GeoModel model, float[] params,
                                           ClientModelInfo bundle, float tick) {
        for (int i = 0; i < 8; i++) {
            applyAnimation(model, params, bundle, "pre_parallel" + i, tick);
        }
        for (int i = 0; i < 8; i++) {
            applyAnimation(model, params, bundle, "parallel" + i, tick);
        }
    }

    /** 单动画施加（sample 主路径与并行槽共用；含 LOOP 取模/非 LOOP 钳末帧）。 */
    private static boolean applyAnimation(GeoModel model, float[] params, ClientModelInfo bundle,
                                          String animName, float tick) {
        Animation anim = findAnimation(bundle, animName);
        if (anim == null || anim.boneAnimations.isEmpty()
                || model == null || model.bakedBones == null || params == null) {
            return false;
        }
        float len = anim.animationLength;
        if (len <= 0.0f) {
            return false;
        }
        boolean loop = anim.loop == ILoopType.EDefaultLoopTypes.LOOP;
        float t;
        if (loop) {
            t = tick % len;
            if (t < 0.0f) {
                t += len;
            }
        } else {
            // PLAY_ONCE/HOLD_ON_LAST_FRAME：播完持末帧（钳 len，非取模——
            // 取模会使非 LOOP 无限重播，审查阻塞项修正）
            t = Math.min(Math.max(tick, 0.0f), len);
        }
        for (int i = 0; i < anim.boneAnimations.size(); i++) {
            BoneAnimation bone = anim.boneAnimations.get(i);
            int idx = boneIndex(model, bone.boneName);
            if (idx < 0) {
                continue;
            }
            int p = idx * 12;
            if (p + 11 >= params.length) {
                break;
            }
            applyChannel(bone.rotationKeyFrames, t, params, p);
            applyChannel(bone.positionKeyFrames, t, params, p + 3);
            applyChannel(bone.scaleKeyFrames, t, params, p + 6);
        }
        return true;
    }

    /** 全骨重置=绑定位（rot 0/pos 0/scale 1/旗标 0；12 float/骨契约）。 */
    public static void resetParams(GeoModel model, float[] params) {
        if (model == null || model.bakedBones == null || params == null) {
            return;
        }
        for (int i = 0; i < model.bakedBones.size() && (i * 12 + 11) < params.length; i++) {
            int p = i * 12;
            params[p] = 0f;
            params[p + 1] = 0f;
            params[p + 2] = 0f;
            params[p + 3] = 0f;
            params[p + 4] = 0f;
            params[p + 5] = 0f;
            params[p + 6] = 1f;
            params[p + 7] = 1f;
            params[p + 8] = 1f;
            params[p + 9] = 0f;
            params[p + 10] = 0f;
            params[p + 11] = 0f;
        }
        applyDebugHide(model, params);
    }

    // ponytail: 隐藏旗标消费侧验收钩子——环境变量 YSM_LEGACY122_HIDE_BONE_TEST=
    // <骨名>[,<骨名>]，skip: 前缀写 offset10。对命名骨写 offset9=1。格式内无隐藏
    // 关键帧通道（builtin 全量扫描 rot/pos/scale 三通道），静态源在主线 bake 侧同为
    // 恒 false（YSMClientMapper:457 GeoBone(...,false,false,false)），此钩子是
    // offset9/10 写入面的唯一活路径=消费语义的可证伪驱动器，升级路径=控制器/
    // 脚本面落地时替换。走环境变量（gradle run 子进程自动继承，免动 build 文件）。
    private static void applyDebugHide(GeoModel model, float[] params) {
        String spec = System.getenv("YSM_LEGACY122_HIDE_BONE_TEST");
        if (spec == null || spec.isEmpty()) {
            return;
        }
        for (String entry : spec.split(",")) {
            boolean skip = entry.startsWith("skip:");
            String boneName = skip ? entry.substring(5) : entry;
            for (int i = 0; i < model.bakedBones.size(); i++) {
                if (boneName.equals(model.bakedBones.get(i).name)) {
                    params[i * 12 + (skip ? 10 : 9)] = 1.0f;
                }
            }
        }
    }


    /** 覆盖 t 的关键帧求值写契约（关键帧按 startTick 有序连续，线性扫足够）。 */
    private static void applyChannel(List<BoneKeyFrame> frames, float t, float[] params, int off) {
        if (frames == null || frames.isEmpty()) {
            return;
        }
        BoneKeyFrame chosen = frames.get(frames.size() - 1);
        for (int i = 0; i < frames.size(); i++) {
            BoneKeyFrame kf = frames.get(i);
            if (t < kf.endTick) {
                chosen = kf;
                break;
            }
        }
        float percent = (t - chosen.startTick) / chosen.totalTick;
        if (percent < 0.0f) {
            percent = 0.0f;
        } else if (percent > 1.0f) {
            percent = 1.0f;
        }
        Vector3f v = chosen.evaluate(EVALUATOR, percent, VEC);
        params[off] = v.x;
        params[off + 1] = v.y;
        params[off + 2] = v.z;
    }

    private static int boneIndex(GeoModel model, String boneName) {
        if (boneName == null) {
            return -1;
        }
        for (int i = 0; i < model.bakedBones.size(); i++) {
            if (boneName.equals(model.bakedBones.get(i).name)) {
                return i;
            }
        }
        return -1;
    }
}
