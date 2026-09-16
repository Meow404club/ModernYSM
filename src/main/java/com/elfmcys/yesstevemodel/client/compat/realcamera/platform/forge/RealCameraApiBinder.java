package com.elfmcys.yesstevemodel.client.compat.realcamera.platform.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.19.3 {
import com.mojang.math.Axis;
//? }
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * RealCamera B1 修复（fix-fpm-rc）：向 RealCamera 反射软注册绑定函数，从 YSM 骨骼链
 * （ViewLocator / headBones）直产 BindResult，绕开 UV 探针捕获（探针在世界内两路径都失败，
 * 诊断账 C4；GPU 直写顶点不进 catcher，R5）。函数 available 时 RealCameraCore.computeCamera
 * 跳过整段探针渲染（8b82d0deec18 RealCameraCore.java:86-95），省每帧 4+ 次全模型重渲染。
 *
 * <p>关键语义（任务卡硬约束）：BindResult 挂在<b>用户当前选的 target</b> 上——
 * ConfigFile.config().getBindTargetList(textureId)（0.7.5 ModConfig.java:220-226）返回的
 * 首位即 GUI activeConfigIndex 选中的匹配 target（contains 匹配语义 ModConfig.java:223；
 * activeConfigIndex<=0 时=最高优先匹配，与探针首选项一致），
 * offsets/bindConfig（逐轴门）随 target 在 BindResult.computeCamera / MixinCamera 里照常生效
 * =绑定 GUI 手动微调完整保留。无匹配 target → 返回 EMPTY 让位 UV 兜底（=现状行为）。
 *
 * <p>API 形态以 realcamera-dev.jar（0.7.8）反编译为准
 * （tmp/compat-debug/decomp/realcamera-src）：RealCameraAPI.registerFunction(int,BiFunction)
 * priority 降序首个 available() 胜出（RealCameraAPI.java:24-33）；BindResult 构造
 * (BindTarget)+setForward/setUpward 归一化（BindResult.java:18-58）；rotation 默认单位阵
 * （CameraTransform.java:13）→ available() 行列式检查天然通过；编译期 libs/ 只有 0.6.11a
 * （无 registerFunction/BindTarget，本类全反射，缺席/漂移静默禁用）。
 *
 * <p>debug-win-gpu-round2 修正：用户真机 realcamera 0.7.5-beta-8b82d0d 的 BindResult
 * 只有双参构造 {@code (BindTarget, boolean mirrored)}（javap 实证；getOrCreate 内部
 * 传 iconst_0=false）——单参 getConstructor 在该版本 NoSuchMethodException → B1 从未
 * 注册（用户机 latest.log:1484 实证）。现按「首参 BindTarget 的最短公有构造」扫描，
 * 多出参数填缺省值（boolean=false），0.7.5~0.7.8 双形态兼容；computeBind 各 EMPTY
 * 出口加限频 reason 打点（此前 catch 全静默，llvmpipe 恒 EMPTY 无法归因——观测缺口）。
 *
 * <p>fix-rc-bindresult-completeness 修正（管线对照 0.7.5-beta commit 8b82d0deec18 亲读，
 * 行号指该 commit）：①空间基准 180-bodyRot → 180-lerp(yRotO,yRot)——探针渲染帧以
 * view yaw 为基准（RealCameraCore.java:89 dispatcher.render 的 yaw 实参），而
 * MixinCamera.getRawPos（MixinCamera.java:52-53+RealCameraCore.java:66-70）把局部坐标
 * 直接加 entityPos（无旋转），基准帧错 netHeadYaw 会让转头时绑定点绕实体原点漂移、
 * offsets 位移的世界方向随之错位（用户实测"offset/旋转未生效"的位置侧根因）。
 * ②配置消费语义定案：offsets（位移+yaw/pitch/roll 旋转）与 BindConfig 逐轴门/
 * bindRotation 门均在官方侧消费——BindResult.computeCamera（BindResult.java:72-84）
 * 对函数/探针结果一视同仁，MixinCamera:53,57 与 forge EventHandler:17 读
 * currentTarget()=我们挂的 target；函数路径唯一结构上不可消费的是 TargetConfig
 * 顶部矢量/前向 UV（仅 VertexData.normal 可读，官方 YSMCompat 也只能重渲染采样，
 * YSMCompat.java:94-96）→ 骨路径以模型轴替代，数值旋转微调走 offsets。
 * ③绑定成功出口数值打点（喂入三字段+target 配置摘要+按官方消费公式预测的最终
 * 位置/yaw/pitch/roll），供与配置期望数值对账（任务卡 Phase3 数值化验收）。
 *
 * <p><1.19.3 无 JOML PoseStack/Axis 面（impl 需要）→ no-op（RealCamera 0.7.x 也未发布该段）；
 * 1.16.5 与 neoforge 线整树排除 client/compat/**（build.unimined.gradle.kts:236 /
 * build.moddev.gradle.kts:272），本文件仅 forge 中段参与编译。
 */
public final class RealCameraApiBinder {

    private RealCameraApiBinder() {
    }

    /** 注册绑定函数（幂等）。realcamera 缺席 / 0.7.8 API 面不符 → 静默禁用。 */
    public static void register() {
        //? if >=1.19.3 {
        register0();
        //? }
    }

    //? if >=1.19.3 {
    /** priority>100：压过 realcamera 自带 YSMCompat(-100) 与 LegacyBindingMode(100)（任务卡 B1） */
    private static final int BIND_PRIORITY = 1000;
    // bedrock 几何面朝模型 -Z（setupRotations 的 180-yaw 翻转把它对齐实体朝向）
    private static final Vector3f MODEL_FORWARD = new Vector3f(0.0f, 0.0f, -1.0f);
    // 骨骼链深度保险（防脏模型循环 parent）
    private static final int MAX_CHAIN_DEPTH = 64;

    private static boolean registered;
    private static Method mRegisterFunction;
    private static Method mConfig;
    private static Method mGetBindTargetList;
    private static Constructor<?> ctorBindResult;
    /** 双参构造 (BindTarget, boolean mirrored) 等形态的尾参缺省值（0.7.5 getOrCreate 传 false） */
    private static Object[] ctorTailDefaults;
    private static Method mSetPosition;
    private static Method mSetForward;
    private static Method mSetUpward;
    private static Object bindResultEmpty;
    /** 绑定成功期间被隐藏的 AllHead 骨骼；退出第一人称/RealCamera 失效时恢复，防第三人称无头 */
    private static IBone hiddenHeadBone;

    private static void register0() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            Class<?> clsApi = Class.forName("com.xtracr.realcamera.api.RealCameraAPI");
            Class<?> clsBindResult = Class.forName("com.xtracr.realcamera.api.BindResult");
            Class<?> clsBindTarget = Class.forName("com.xtracr.realcamera.config.BindTarget");
            bindResultEmpty = clsBindResult.getField("EMPTY").get(null);
            // debug-win-gpu-round2：不锁死单参构造——0.7.5-beta 只有 (BindTarget, boolean mirrored)，
            // 0.7.8 是 (BindTarget)。取首参 BindTarget 的最短公有构造，尾参填缺省值。
            for (Constructor<?> c : clsBindResult.getConstructors()) {
                Class<?>[] ps = c.getParameterTypes();
                if (ps.length == 0 || ps[0] != clsBindTarget) {
                    continue;
                }
                if (ctorBindResult == null || ps.length < ctorBindResult.getParameterTypes().length) {
                    ctorBindResult = c;
                }
            }
            if (ctorBindResult != null) {
                Class<?>[] ps = ctorBindResult.getParameterTypes();
                ctorTailDefaults = new Object[ps.length - 1];
                for (int i = 1; i < ps.length; i++) {
                    Class<?> p = ps[i];
                    if (p == boolean.class) {
                        ctorTailDefaults[i - 1] = Boolean.FALSE;
                    } else if (p == int.class) {
                        ctorTailDefaults[i - 1] = 0;
                    } else if (p == float.class) {
                        ctorTailDefaults[i - 1] = 0.0f;
                    } else if (p == double.class) {
                        ctorTailDefaults[i - 1] = 0.0d;
                    } else if (p == long.class) {
                        ctorTailDefaults[i - 1] = 0L;
                    } else {
                        ctorTailDefaults[i - 1] = null;
                    }
                }
            }
            if (ctorBindResult == null) {
                // 干净失败：不留一个每帧 NPE 的死函数在 FUNCTIONS 里
                throw new NoSuchMethodException(
                        "BindResult has no public constructor with BindTarget as first parameter");
            }
            mSetPosition = clsBindResult.getMethod("setPosition", Vec3.class);
            mSetForward = clsBindResult.getMethod("setForward", Vec3.class);
            mSetUpward = clsBindResult.getMethod("setUpward", Vec3.class);
            mRegisterFunction = clsApi.getMethod("registerFunction", int.class, BiFunction.class);
            mConfig = Class.forName("com.xtracr.realcamera.config.ConfigFile").getMethod("config");
            mGetBindTargetList = mConfig.getReturnType().getMethod("getBindTargetList", String.class);
            BiFunction<Minecraft, Float, Object> fn = RealCameraApiBinder::computeBind;
            mRegisterFunction.invoke(null, BIND_PRIORITY, fn);
            MinecraftForge.EVENT_BUS.register(new HeadRestoreListener());
            YesSteveModel.LOGGER.info(
                    "[compat] RealCamera bind function registered (bone-driven, priority {}, ctor {})",
                    BIND_PRIORITY, ctorBindResult == null ? "NONE" : ctorBindResult);
        } catch (Throwable t) {
            YesSteveModel.LOGGER.warn("[compat] RealCamera bind function not registered: {}", t.toString());
        }
    }

    /**
     * 绑定函数本体（RealCameraAPI.computeBindResult 每帧第一人称调用）。
     * 返回 Object=BindResult；任何失败路径返回 EMPTY 让位 UV 兜底（=不装本修复的现状行为）。
     * 各 EMPTY 出口带 reason 限频打点（debug-win-gpu-round2：此前 catch 全静默=观测缺口，
     * llvmpipe"恒 EMPTY"无法归因；每 reason 前排打 INFO，之后仅计数）。
     */
    private static Object computeBind(Minecraft client, Float partialTick) {
        try {
            if (!(client.getCameraEntity() instanceof Player player) || player.isSpectator()) {
                return empty("noCameraPlayer");
            }
            PlayerCapability cap = PlayerCapability.get(player).orElse(null);
            if (cap == null || !cap.isModelActive()) {
                return empty("modelInactive");
            }
            AnimatedGeoModel model = cap.getCurrentModel();
            if (model == null) {
                return empty("noModel");
            }
            // 用户当前选的 target（GUI activeConfigIndex 语义），无匹配=未绑定→让位
            Object target = userSelectedTarget(cap.getTextureLocation());
            if (target == null) {
                net.minecraft.resources.ResourceLocation tex = cap.getTextureLocation();
                return empty("noMatchingTarget:" + (tex == null ? "null" : tex.toString()));
            }
            List<IBone> chain = viewChain(model);
            if (chain.isEmpty()) {
                return empty("noViewChain");
            }
            // 复刻 GeoReplacedEntityRenderer 实体渲染空间（UV 探针 dispatcher.render(entity,0,0,0)
            // 捕获即该空间，BindResult.position 为实体局部坐标，MixinCamera 经 getRawPos
            // smoothedPos.add(entityPos) 落世界，realcamera-named.jar getRawPos 字节码实证）：
            // setupRotations 180-viewYaw（探针实参=lerp(yRotO,yRot)，RealCameraCore.java:89——
            // fix-rc-bindresult-completeness：bodyRot 帧错 netHeadYaw，转头即漂移+offsets 方向错）→
            // renderEarly scale(hS,wS,hS)（IGeoRenderer.java:46-50）
            float viewRot = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - viewRot));
            poseStack.scale(cap.getHeightScale(), cap.getWidthScale(), cap.getHeightScale());
            // 盔甲层定位同源数学（CustomPlayerArmorLayer:103 prepMatrixForLocator(model.headBones())）
            RenderUtils.prepMatrixForLocator(poseStack, chain);
            org.joml.Matrix4f mat = poseStack.last().pose();
            // 相机点：ViewLocator=作者指定点（locator 无 cube→取 pivot）；head 链=头骨自身 cube
            // 中心（≈UV 面中心同位；颈 pivot 会把相机压到下颌）
            Vector3f centerOffset = boneCubeCenterOffset(model, chain.get(chain.size() - 1));
            Vector3f position = mat.transformPosition(centerOffset != null ? centerOffset : new Vector3f());
            Vector3f forward = mat.transformDirection(new Vector3f(MODEL_FORWARD));
            if (!finiteNonZero(position) || !finiteNonZero(forward)) {
                return empty("nonFiniteAnchor");
            }
            forward.normalize();
            Vector3f upward = mat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
            if (!finiteNonZero(upward)) {
                return empty("nonFiniteUpward");
            }
            upward.normalize();
            // forward/upward 近共线会让 realcamera computeCamera 的正交化退化
            if (Math.abs(forward.dot(upward)) > 0.99f) {
                return empty("collinearAxes");
            }
            Object[] args;
            if (ctorTailDefaults.length == 0) {
                args = new Object[]{target};
            } else {
                args = new Object[1 + ctorTailDefaults.length];
                args[0] = target;
                System.arraycopy(ctorTailDefaults, 0, args, 1, ctorTailDefaults.length);
            }
            Object result = ctorBindResult.newInstance(args);
            mSetPosition.invoke(result, new Vec3(position.x(), position.y(), position.z()));
            mSetForward.invoke(result, new Vec3(forward.x(), forward.y(), forward.z()));
            mSetUpward.invoke(result, new Vec3(upward.x(), upward.y(), upward.z()));
            logBindSuccess(target, position, forward, upward);
            hideHead(model);
            return result;
        } catch (Throwable t) {
            return empty("exception: " + t);
        }
    }

    /** EMPTY 让位 + reason 限频打点：每 reason 前 {@link #EMPTY_LOG_BUDGET} 次打 INFO，之后每 1200 帧汇报一次累计。 */
    private static Object empty(String reason) {
        int n = emptyReasons.merge(reason, 1, Integer::sum);
        if (n <= EMPTY_LOG_BUDGET) {
            YesSteveModel.LOGGER.info("[compat] RealCamera bone bind EMPTY #{} reason={}", n, reason);
        } else if (n % EMPTY_LOG_INTERVAL == 0) {
            YesSteveModel.LOGGER.info("[compat] RealCamera bone bind EMPTY x{} reason={}", n, reason);
        }
        return bindResultEmpty;
    }

    private static final int EMPTY_LOG_BUDGET = 3;
    private static final int EMPTY_LOG_INTERVAL = 1200;
    private static final java.util.HashMap<String, Integer> emptyReasons = new java.util.HashMap<>();

    /** 绑定成功打点限频：首 {@link #BIND_LOG_BUDGET} 帧每帧，之后每 {@link #BIND_LOG_INTERVAL} 帧一次。 */
    private static final int BIND_LOG_BUDGET = 3;
    private static final int BIND_LOG_INTERVAL = 600;
    private static int bindLogCount;

    /**
     * 绑定成功数值打点（fix-rc-bindresult-completeness，任务卡 Phase3-1 数值化验收）：
     * 喂入三字段 + target 配置摘要 + 按官方消费公式预测的最终相机位置/yaw/pitch/roll，
     * 供与用户配置期望对账（红线：禁止看图验收）。ponytail: 「预测最终」是
     * BindResult.computeCamera（8b82d0deec18 BindResult.java:72-84）+
     * MathUtil.getEulerAngleYXZ（MathUtil.java:13-20）的只读日志镜像，不参与相机；
     * 官方改消费语义时需同步本镜像。镜像自身抛错仅降级为少打一行，绝不影响绑定。
     */
    private static void logBindSuccess(Object target, Vector3f position, Vector3f forward, Vector3f upward) {
        bindLogCount++;
        if (bindLogCount > BIND_LOG_BUDGET && bindLogCount % BIND_LOG_INTERVAL != 0) {
            return;
        }
        try {
            String f = "(%.3f, %.3f, %.3f)";
            YesSteveModel.LOGGER.info("[compat] RealCamera bone bind #{} feed pos={} fwd={} up={}",
                    bindLogCount,
                    String.format(f, position.x(), position.y(), position.z()),
                    String.format(f, forward.x(), forward.y(), forward.z()),
                    String.format(f, upward.x(), upward.y(), upward.z()));
            Class<?> t = target.getClass();
            Object bindConfig = t.getMethod("bindConfig").invoke(target);
            Object offsets = t.getMethod("offsets").invoke(target);
            Class<?> b = bindConfig.getClass();
            Class<?> o = offsets.getClass();
            float scale = (Float) o.getMethod("getScale").invoke(offsets);
            float ox = (Float) o.getMethod("getX").invoke(offsets);
            float oy = (Float) o.getMethod("getY").invoke(offsets);
            float oz = (Float) o.getMethod("getZ").invoke(offsets);
            float op = (Float) o.getMethod("getPitch").invoke(offsets);
            float oyw = (Float) o.getMethod("getYaw").invoke(offsets);
            float orl = (Float) o.getMethod("getRoll").invoke(offsets);
            // 官方消费镜像：正交化→列基[left|up|fwd]→position+=R·(z,y,x)·scale→rotateLocal(yaw,pitch,roll)
            org.joml.Matrix3f rot = new org.joml.Matrix3f();
            Vector3f cross = new Vector3f(upward).cross(new Vector3f(forward));
            Vector3f up2 = new Vector3f(forward).cross(cross).normalize();
            Vector3f left = new Vector3f(up2).cross(new Vector3f(forward));
            rot.set(left, up2, new Vector3f(forward));
            Vector3f offset = new Vector3f(oz, oy, ox).mul(scale).mul(rot);
            Vector3f finalPos = new Vector3f(position).add(offset);
            rot.rotateLocal((float) Math.toRadians(oyw), rot.m10, rot.m11, rot.m12);
            rot.rotateLocal((float) Math.toRadians(op), rot.m00, rot.m01, rot.m02);
            rot.rotateLocal((float) Math.toRadians(orl), rot.m20, rot.m21, rot.m22);
            float m21 = Math.max(-1.0f, Math.min(1.0f, rot.m21));
            float pitchDeg = (float) Math.toDegrees(Math.asin(-m21));
            float yawDeg = (float) Math.toDegrees(Math.atan2(rot.m20, rot.m22));
            float rollDeg = (float) Math.toDegrees(Math.atan2(rot.m01, rot.m11));
            YesSteveModel.LOGGER.info(
                    "[compat] RealCamera bind cfg name={} tex={} prio={} bind[X:{} Y:{} Z:{} rot:{}] offsets(scale:{}, x:{}, y:{}, z:{}, pitch:{}, yaw:{}, roll:{}) => final pos={} euler(deg yaw:{} pitch:{} roll:{})",
                    t.getMethod("name").invoke(target), t.getMethod("textureId").invoke(target),
                    t.getMethod("priority").invoke(target),
                    b.getMethod("bindX").invoke(bindConfig), b.getMethod("bindY").invoke(bindConfig),
                    b.getMethod("bindZ").invoke(bindConfig), b.getMethod("bindRotation").invoke(bindConfig),
                    scale, ox, oy, oz, op, oyw, orl,
                    String.format(f, finalPos.x(), finalPos.y(), finalPos.z()),
                    String.format("%.1f", yawDeg), String.format("%.1f", pitchDeg), String.format("%.1f", rollDeg));
        } catch (Throwable t) {
            YesSteveModel.LOGGER.debug("[compat] RealCamera bind log failed: {}", t.toString());
        }
    }

    /**
     * 用户当前选的 target（BindTarget）：getBindTargetList(textureId) 按 realcamera 同款
     * contains 语义过滤 + activeConfigIndex 选中（0.7.5 ModConfig.java:220-226），随 BindResult
     * 进 computeCamera → offsets/bindConfig 手动微调照常生效（任务卡硬约束）。
     */
    @Nullable
    private static Object userSelectedTarget(@Nullable net.minecraft.resources.ResourceLocation texture) {
        if (mConfig == null || mGetBindTargetList == null || texture == null) {
            return null;
        }
        try {
            Object config = mConfig.invoke(null);
            if (config == null) {
                return null;
            }
            List<?> targets = (List<?>) mGetBindTargetList.invoke(config, texture.toString());
            if (targets == null || targets.isEmpty()) {
                return null;
            }
            Object target = targets.get(0);
            if (target == null || (Boolean) target.getClass().getMethod("isEmpty").invoke(target)) {
                return null;
            }
            return target;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 绑定即相机所在：相机位于头骨 cube 中心时头几何会包住相机（cutoutNoCull 双面），
     * 与 FPM 同款语义隐藏 AllHead 子树（setHidden(z,z) 连子骨）；退出第一人称/RealCamera
     * 失效时由 {@link HeadRestoreListener} 恢复。computeCamera 每帧先于 renderLevel 执行，
     * 与 applyHeadTracking 的 FPM 隐藏同管线互不冲突。
     */
    private static void hideHead(AnimatedGeoModel model) {
        IBone allHead = model.allHeadBone();
        if (allHead != null) {
            allHead.setHidden(true);
            hiddenHeadBone = allHead;
        }
    }

    /** 客户端 tick 恢复头骨：仅在 RealCamera 绑定可能活跃期间保持隐藏。 */
    static final class HeadRestoreListener {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            IBone bone = hiddenHeadBone;
            if (bone == null) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            boolean fpRealCamera = RealCameraCompat.isActive()
                    && client.options != null && client.options.getCameraType().isFirstPerson();
            if (!fpRealCamera) {
                bone.setHidden(false);
                hiddenHeadBone = null;
            }
        }
    }

    /** 绑定骨骼链：优先 ViewLocator（祖先链自拼），缺失则退 headBones 链；两者皆无=EMPTY（优雅降级）。 */
    private static List<IBone> viewChain(AnimatedGeoModel model) {
        IBone viewLocator = model.viewLocatorBone();
        if (viewLocator != null) {
            List<IBone> chain = ancestorChain(model, viewLocator.getBoneId());
            if (!chain.isEmpty()) {
                return chain;
            }
        }
        return model.headBones();
    }

    /** 目标骨骼的祖先链（根→…→目标），复用 GeoModel 扁平骨表的 parentIdx。 */
    private static List<IBone> ancestorChain(AnimatedGeoModel model, int boneId) {
        List<GeoBone> bones = model.getGeoModel().topLevelBones();
        GeoBone node = null;
        for (GeoBone bone : bones) {
            if (bone.getBoneId() == boneId) {
                node = bone;
                break;
            }
        }
        if (node == null) {
            return List.of();
        }
        List<IBone> chain = new ArrayList<>(8);
        int depth = 0;
        while (node != null && depth++ < MAX_CHAIN_DEPTH) {
            IBone ibone = model.bones().get(node.getBoneId());
            if (ibone != null) {
                chain.add(ibone);
            }
            node = node.parentIdx < 0 ? null : bones.get(node.parentIdx);
        }
        Collections.reverse(chain);
        return chain;
    }

    /**
     * 链末端骨骼自身 cube 的中心（骨局部单位/16=块，相对 pivot）；无 cube（ViewLocator 等
     * locator 骨）返回 null→调用方回退 pivot 本身。cube 数据构建期填充（YSMClientMapper.java:563），
     * CPU/GPU 路径皆有。
     */
    @Nullable
    private static Vector3f boneCubeCenterOffset(AnimatedGeoModel model, IBone tip) {
        List<GeoModel.BakedBone> bakedBones = model.getGeoModel().bakedBones;
        if (bakedBones == null) {
            return null;
        }
        for (GeoModel.BakedBone bone : bakedBones) {
            if (!bone.name.equals(tip.getName())) {
                continue;
            }
            float sx = 0.0f;
            float sy = 0.0f;
            float sz = 0.0f;
            int count = 0;
            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    float[] p = quad.positions;
                    for (int i = 0; i + 2 < p.length; i += 3) {
                        sx += p[i];
                        sy += p[i + 1];
                        sz += p[i + 2];
                        count++;
                    }
                }
            }
            if (count == 0) {
                return null;
            }
            return new Vector3f(sx / count / 16.0f, sy / count / 16.0f, sz / count / 16.0f);
        }
        return null;
    }

    private static boolean finiteNonZero(Vector3f v) {
        return Float.isFinite(v.x()) && Float.isFinite(v.y()) && Float.isFinite(v.z()) && v.lengthSquared() > 1.0e-8f;
    }
    //? }
}
