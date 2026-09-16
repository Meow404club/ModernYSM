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
 * 跳过整段探针渲染（realcamera-src RealCameraCore.java:109-125），省每帧 4+ 次全模型重渲染。
 *
 * <p>关键语义（任务卡硬约束）：BindResult 挂在<b>用户当前选的 target</b> 上——
 * ConfigFile.config().getBindTargetList(textureId)（ModConfig.java:277-286）返回的正是
 * GUI activeConfigIndex 选中的匹配 target（contains 匹配语义 ModConfig.java:280），
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
            // setupRotations 180-bodyRot（LivingEntityRenderer 同构）→
            // renderEarly scale(hS,wS,hS)（IGeoRenderer.java:46-50）
            float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRot));
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

    /**
     * 用户当前选的 target（BindTarget）：getBindTargetList(textureId) 按 realcamera 同款
     * contains 语义过滤 + activeConfigIndex 选中（ModConfig.java:277-286），随 BindResult
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
