package com.elfmcys.yesstevemodel.client.compat.realcamera.platform.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.api.entity.EntityDataBridge;

import java.awt.Polygon;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
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
 * 行号指该 commit）：①空间基准修正——MixinCamera.getRawPos（MixinCamera.java:52-53+
 * RealCameraCore.java:66-70）把 BindResult.position 直接加 entityPos（无旋转）→ 契约空间
 * =【世界轴向、实体脚原点】偏移；探针（RealCameraCore.computeCamera:89 dispatcher.render
 * → CustomPlayerRenderer.render:88 → renderEntityWithTexture）捕获同一渲染链顶点即该空间。
 * <b>diag-rc-anchor-space-mismatch 勘误</b>：M1 轮把基准定为 180-lerp(yRotO,yRot)（view yaw），
 * 依据是 dispatcher.render 的 yaw 实参——但渲染器根本不消费该实参（renderEntityWithTexture
 * 仅转给名牌渲染，GeoReplacedEntityRenderer.java:283；模型根帧 setupRotations 用
 * modelData.lerpBodyRot=rotLerp(pT,yBodyRotO,yBodyRot)，GeoReplacedEntityRenderer.java:234 +
 * AnimatableEntity.java:262，vanilla LivingEntityRenderer:187 mulPose(180-yBodyRot)）。
 * 且头旋转 netHeadYaw=lerpHeadRot-lerpBodyRot 被烤进头骨（LivingAnimatable.applyHeadTracking
 * :71-72 setRotationX/Y）→ feed 根帧若用 view yaw 则头转被双计：pos_view=R(-netHead)·pos_correct
 * ——正前方（netHead=0）恰好无错、转头/移动（tickHeadTurn 每 tick 只追 30%，LivingEntity:2701-2706）
 * /滞空（身体朝移动方向）时持续偏差=用户"正前方低头正常、转头/跳跃转视角不对"的根因。
 * 本轮根帧改 180-lerpBodyRot（体转）并与渲染链矩阵序逐项对齐
 * （R·T(0,0.01,0)·S·[骨链]：GeoReplacedEntityRenderer.java:234→:259→IGeoRenderer.renderEarly）。
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
 *
 * <p><b>diag-rc-preview-anchor-mismatch 方案 A（UV→网格表面点）</b>：此前 B1 锚点=骨原点
 * （headBones 末端 cube 中心或 ViewLocator pivot），而 RC 绑定 GUI（ModelViewScreen）与
 * 探针（RealCameraCore.computeBindResult）的锚点=<b>target UV 在命中面内的重心插值表面点</b>
 * ——两套语义永不相等（Trissy 实测:posUV=Head 颅 cube north 面中心，与 cube 中心差
 * Δz=3.5px，随姿态/动画摆动），用户 offsets 微调永远对不上。另 offsets 消费帧
 * （BindResult.computeCamera:79 的 R·(z,y,x)）由 feed forward/upward 装配——骨轴帧与
 * GUI 预览的面法线帧不同，GUI 里调好的 offset 方向在世界内错位。
 *
 * <p>本轮把函数主路径改为<b>逐字对齐探针语义</b>（8b82d0deec18 行号）：
 * ①UV 命中判定=UV 在 quad 四顶点 UV 多边形内（java.awt.Polygon + 1e6 量化，
 * BuiltIterableBuffer.findPrimitives:71-102 同款；烘焙 quad.uvs 归一化且渲染顶点 UV
 * 即 quad.uvs 原样（NativeModelRenderer:201），与探针捕获值同空间）；②表面点=面前 3 顶点
 * 重心插值（VertexData.position:24-30 公式逐字）；③forward/upward=命中面法线（探针
 * :172-173 不取反、mirrored=false 同款）；④骨变换=该 quad 骨祖先链逐骨
 * RenderUtils.prepMatrixForBone（与渲染 calculateBoneMatrix:251-280 数学同源：平移合并
 * 等价+rotateZYX+scale+-pivot/16），顶点/16 后变换——根帧经 diag-rc-anchor-space-mismatch
 * 勘误为 180-lerpBodyRot+T(0,0.01,0)+scale（渲染链矩阵序，见上段勘误）。任一 UV 无命中/
 * 退化失败 → 原骨轴路径兜底（行为不回退）。
 *
 * <p>性能：quad 命中按 (模型, target) 弱引用缓存（UV 布局=geo 决定，构建期一次 O(全 quad)），
 * 每帧 3 次缓存查 + ≤3 条骨链矩阵 + 9 顶点矩阵乘，微秒级；不渲染、不进 GL。
 *
 * <p><b>fix-rc-transform-matrix（本卡）</b>：M4 情境态收口——bindRootFrame 补 SLEEPING
 * 平移+床向翻转根旋转（GeoReplacedEntityRenderer.java:230-233 + vanilla 1.20.1
 * LivingEntityRenderer.java:193-198 逐字）与载具 expressionOffset 旋转（:235-257 逐字），
 * 无睡眠/无载具默认路径数值零差（新分支全部被条件短路）；M4-③ yaw 帧源经证明已是同帧同源
 * （见 rendererLerpBodyRot javadoc 证明链，无代码改动）；M1 蹲位 uv 数值打点
 * （限频 3+600）已随用户终测裁决 2026-09-17 整体删除。
 *
 * <p><b>fix-rc-bind-target（本卡）根因定案</b>（用户反证「锚到头发/同 quad 双骨归属/蹲下
 * 偏高」三轮证伪，全部有离线数值证据）：①稳态锚面正确——用户真机 posUV(54.5,69.5)px
 * 全模型<b>唯一</b>候选=Head cube0 north 脸面（Trissy main.json: uv=[51,66]+size[7,7]，
 * 面中心 (0,35.825,-3.5)px 与真机 staticSurface 逐位一致），无 UV 重叠、无命中序歧义；
 * ②真机日志 #1~#3 的 bone=Hair=模型加载切换瞬态（该 UV 在 Trissy 无第二候选，Hair cube19
 * west 的 UV 区 [44,50] 不含 54.5 → 命中的是切换前另一模型实例的 quad，QUAD_HIT_CACHE
 * 按模型实例弱引用隔离，3 帧自愈）；③「同一 quad 双骨归属」不成立——打点期实证
 * uv 命中的 bone 与 quad 同链同源（QuadHit 构造期绑定）；④蹲下
 * 只降 0.18 块=Trissy 双蹲动画结构差异的<b>忠实</b>变换：移动蹲 sneak（Root posY
 * +0.5~+1.31px 上浮+UpperBody 俯身旋转，脸点绕 pivot 净降 ~4px×height_scale0.7≈0.18 块
 * ——手工链算 Δy=-4.1px×0.7=-2.88px 与真机 #1800 实测 -0.1801 块吻合）vs 原地蹲 sneaking
 * （Root -10.3+AllBody +1 → Δy≈-0.41 块，M3 实测 -0.4237）；动画选择 AnimationRegister:41-42
 * （sneak=onGround&&CROUCHING&&移动 是 sneaking 子集且先注册）与官方 ctrl.sneak/ctrl.sneaking
 * 语义逐字一致。⑤睡眠=原版相机=RealCamera 官方门控（DisableHelper.MAIN_FEATURE
 * 构造器硬编码 isSleeping()||isSpectator()，RealCameraCore.initialize:41 置 active=false
 * → MixinCamera 首行短路），无配置开关，官方同款行为入账边界不修。本卡曾加的观测
 * 诚实性打点（uv 行 quadId/staticNormal/quadUvCenter/targetUv/uvCandidates/modelHash、
 * bind 行 finalUp 终态镜像）已随用户终测裁决 2026-09-17 删除；根因结论（上①~⑤）
 * 与校验③（up=west 左向+roll=90° 经官方 rotateLocal 链→终态 top=(0,1,0)，产出侧
 * 无 roll 预应用、无双重 roll）保留。
 *
 * <p>结构边界（保留原声明）：TargetConfig 顶部矢量 UV（forward/upward）官方函数路径本就
 * 只能由面数据重建——本路径命中面法线即该语义的网格侧实现，非违例。
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
    /** diag-rc-preview-anchor-mismatch：BindTarget.targetConfig() + 六个 UV getter（float） */
    private static Method mTargetConfig;
    private static Method[] mUvGetters = new Method[6];
    private static final int UV_POS_U = 0, UV_POS_V = 1, UV_FWD_U = 2, UV_FWD_V = 3, UV_UP_U = 4, UV_UP_V = 5;
    /** 绑定成功期间被隐藏的 AllHead 骨骼；退出第一人称/RealCamera 失效时恢复，防第三人称无头 */
    private static IBone hiddenHeadBone;
    /** diag-rc-preview-anchor-mismatch：UV→quad 命中缓存，key=(模型弱引用, target 名)；UV 布局由 geo 决定 */
    private static final Map<AnimatedGeoModel, Map<String, QuadHit[][]>> QUAD_HIT_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    /** UV→quad 命中量化精度（与 realcamera BuiltIterableBuffer.findPrimitives 的 resolution 一致） */
    private static final int UV_RESOLUTION = 1000000;

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
            // diag-rc-preview-anchor-mismatch：TargetConfig UV 面（缺席/漂移 → UV 路径自动禁用，走骨轴兜底）
            try {
                mTargetConfig = clsBindTarget.getMethod("targetConfig");
                Class<?> clsTargetConfig = mTargetConfig.getReturnType();
                mUvGetters[UV_POS_U] = clsTargetConfig.getMethod("posU");
                mUvGetters[UV_POS_V] = clsTargetConfig.getMethod("posV");
                mUvGetters[UV_FWD_U] = clsTargetConfig.getMethod("forwardU");
                mUvGetters[UV_FWD_V] = clsTargetConfig.getMethod("forwardV");
                mUvGetters[UV_UP_U] = clsTargetConfig.getMethod("upwardU");
                mUvGetters[UV_UP_V] = clsTargetConfig.getMethod("upwardV");
            } catch (Throwable t) {
                mTargetConfig = null;
                YesSteveModel.LOGGER.info("[compat] RealCamera TargetConfig UV face unavailable, bone-axis fallback only: {}", t.toString());
            }
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
            float viewRot = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            float[] basis = rendererLerpBodyRot(player, partialTick);
            float bodyRot = basis[0];
            float netHeadYawDeg = basis[1];
            // diag-rc-preview-anchor-mismatch 方案 A：UV→表面点主路径（与 GUI/探针同语义锚点）
            if (mTargetConfig != null) {
                Object uvResult = tryUvBind(player, model, target, viewRot, bodyRot, netHeadYawDeg, cap, partialTick);
                if (uvResult != null) {
                    return uvResult;
                }
            }
            // 骨轴兜底（原路径：UV 无命中/退化/TargetConfig 反射缺席时行为不回退）
            List<IBone> chain = viewChain(model);
            if (chain.isEmpty()) {
                return empty("noViewChain");
            }
            // 根帧=可见模型/UV 探针渲染空间（diag-rc-anchor-space-mismatch 空间簿记）：
            // GeoReplacedEntityRenderer.renderEntityWithTexture:234 setupRotations 用
            // modelData.lerpBodyRot（体转，entityYaw 实参仅名牌消费），矩阵序 :234(R)→
            // :259 translate(0,0.01,0)→IGeoRenderer.renderEarly scale(hS,wS,hS)；契约=
            // MixinCamera.getRawPos(RealCameraCore.java:66-70) 直加 entityPos → 世界轴向脚原点空间
            PoseStack poseStack = bindRootFrame(player, bodyRot, cap);
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
            // rc-closure-mirror-gaps：骨轴路径的 armed 探针差分（UV 路径 collinear 被拒时
            // 一致性数值格仍可用；探针 geo 兜底对任意预测点有效，见 runProbeDiff javadoc）
            if (probeDiffArmed() && mTargetConfig != null) {
                try {
                    Object cfgB = mTargetConfig.invoke(target);
                    if (cfgB != null) {
                        float[] uvB = new float[6];
                        for (int i = 0; i < 6; i++) {
                            uvB[i] = (Float) mUvGetters[i].invoke(cfgB);
                        }
                        runProbeDiff(player, model, cap, null, uvB, position, forward, upward, bodyRot,
                                partialTick, null, chain, target);
                    }
                } catch (Throwable t) {
                    YesSteveModel.LOGGER.info("[compat][rc-probe-diff] bone-path diff setup failed: {}", t.toString());
                }
            }
            if (spaceDiagAllowed(netHeadYawDeg)) {
                // 对照值：view-yaw 基准（M1 旧行为）独立重算——同一骨链换根帧，非恒等式推导
                PoseStack rootAlt = bindRootFrame(player, viewRot, cap);
                RenderUtils.prepMatrixForLocator(rootAlt, chain);
                org.joml.Matrix4f matAlt = rootAlt.last().pose();
                Vector3f altOffset = boneCubeCenterOffset(model, chain.get(chain.size() - 1));
                Vector3f altPos = matAlt.transformPosition(altOffset != null ? altOffset : new Vector3f());
                Vector3f altFwd = matAlt.transformDirection(new Vector3f(MODEL_FORWARD));
                altFwd.normalize();
                Vector3f altUp = matAlt.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
                altUp.normalize();
                logSpaceDiag(viewRot, bodyRot, netHeadYawDeg, position, forward, upward, altPos, altFwd, altUp, target);
            }
            hideHead(model);
            return result;
        } catch (Throwable t) {
            return empty("exception: " + t);
        }
    }

    /**
     * diag-rc-preview-anchor-mismatch 方案 A：UV→网格表面点绑定（GUI/探针同语义锚点）。
     * 返回 null = 本路径不适用（让调用方走骨轴兜底），绝不抛出。
     *
     * <p>语义对齐（8b82d0deec18 行号见类 javadoc）：posUV 命中面 3 顶点重心插值=position
     * （VertexData.position:24-30 公式）；forward/upward=forwardUV/upwardUV 命中面法线
     * （探针 RealCameraCore:172-173 不取反）；命中判定=UV 在 quad UV 四边形内
     * （java.awt.Polygon + 1e6 量化=BuiltIterableBuffer.findPrimitives:71-102 同款）。
     * forwardUV/upwardUV 无命中时分别退 pos 面法线 / pos 骨链 Y 轴（探针遇缺面是整体
     * not-available 让位 UV 兜底；此处保底单点配置可用，位置语义不受影响）。
     */
    @Nullable
    private static Object tryUvBind(Player player, AnimatedGeoModel model, Object target, float viewRot, float bodyRot,
                                    float netHeadYawDeg, PlayerCapability cap, float partialTick) {
        try {
            Object cfg = mTargetConfig.invoke(target);
            if (cfg == null) {
                uvFallbackLog("targetConfigNull");
                return null;
            }
            float[] uv = new float[6];
            for (int i = 0; i < 6; i++) {
                uv[i] = (Float) mUvGetters[i].invoke(cfg);
            }
            // 三槽候选组：[槽][候选序]，[?][0]=首中=生效锚面（fix-rc-bind-target）
            QuadHit[][] hitSlots = quadHits(model, target, uv);
            QuadHit[] posCandidates = hitSlots[0];
            if (posCandidates.length == 0) {
                // posUV 无命中：UV 布局对不上（模型/贴图布局不符）→ 骨轴兜底
                uvFallbackLog("noPosCandidates u=" + uv[UV_POS_U] + " v=" + uv[UV_POS_V]);
                return null;
            }
            QuadHit posHit = posCandidates[0];
            // 名字→IBone 一次遍历（三槽链共享）
            Map<String, IBone> bonesByName = new java.util.HashMap<>();
            for (IBone bone : model.bones().values()) {
                bonesByName.put(bone.getName(), bone);
            }
            // 根帧与可见模型渲染链一致：180-lerpBodyRot + T(0,0.01,0) + scale（bindRootFrame，
            // 空间簿记见类 javadoc 勘误段：探针/entityYaw 实参不决定根帧，体转才是渲染根；
            // 本卡起 SLEEPING/载具情境态分支同帧镜像，见 bindRootFrame javadoc）
            PoseStack root = bindRootFrame(player, bodyRot, cap);
            Map<String, Matrix4f> frameMats = new java.util.HashMap<>();

            Vector3f position = surfacePoint(root, frameMats, model, bonesByName, posHit, uv[UV_POS_U], uv[UV_POS_V]);
            if (!finiteNonZero(position)) {
                uvFallbackLog("nonFinitePos");
                return null;
            }
            // forward：forwardUV 命中面法线；无命中退 pos 面法线（=面朝向，bindRotation 时视角贴面）
            Vector3f forward = quadDirection(root, frameMats, model, bonesByName,
                    hitSlots[1].length > 0 ? hitSlots[1][0] : posHit);
            if (!finiteNonZero(forward)) {
                uvFallbackLog("nonFiniteFwd");
                return null;
            }
            forward.normalize();
            // upward：upwardUV 命中面法线；无命中退 pos 骨链 Y 轴（模型竖直轴）
            Vector3f upward = hitSlots[2].length > 0
                    ? quadDirection(root, frameMats, model, bonesByName, hitSlots[2][0])
                    : boneUpAxis(root, frameMats, model, bonesByName, posHit.boneName);
            if (!finiteNonZero(upward)) {
                uvFallbackLog("nonFiniteUp");
                return null;
            }
            upward.normalize();
            // forward/upward 近共线会让 realcamera computeCamera 的正交化退化
            if (Math.abs(forward.dot(upward)) > 0.99f) {
                uvFallbackLog("collinearAxes");
                return null;
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
            // fix-rc-probe-diff：harness 门控探针差分（生产 armed 文件不存在=零行为，见 runProbeDiff javadoc）
            if (probeDiffArmed()) {
                runProbeDiff(player, model, cap, posHit, uv, position, forward, upward, bodyRot, partialTick,
                        bonesByName, null, target);
            }
            if (spaceDiagAllowed(netHeadYawDeg)) {
                // 对照值：view-yaw 基准（M1 旧行为）独立重算——同一骨链换根帧，非恒等式推导；
                // 三字段与 fed 计算完全镜像（surfacePoint/quadDirection/boneUpAxis 同参同序）
                PoseStack rootAlt = bindRootFrame(player, viewRot, cap);
                Map<String, Matrix4f> altMats = new java.util.HashMap<>();
                Vector3f altPos = surfacePoint(rootAlt, altMats, model, bonesByName, posHit, uv[UV_POS_U], uv[UV_POS_V]);
                Vector3f altFwd = quadDirection(rootAlt, altMats, model, bonesByName,
                        hitSlots[1].length > 0 ? hitSlots[1][0] : posHit);
                Vector3f altUp = hitSlots[2].length > 0
                        ? quadDirection(rootAlt, altMats, model, bonesByName, hitSlots[2][0])
                        : boneUpAxis(rootAlt, altMats, model, bonesByName, posHit.boneName);
                if (altPos != null && altFwd != null && altUp != null) {
                    altFwd.normalize();
                    altUp.normalize();
                    logSpaceDiag(viewRot, bodyRot, netHeadYawDeg, position, forward, upward, altPos, altFwd, altUp, target);
                }
            }
            hideHead(model);
            return result;
        } catch (Throwable t) {
            // UV 路径任何异常 → 骨轴兜底（不计 EMPTY 统计，避免双计数）
            uvFallbackLog("exception: " + t);
            YesSteveModel.LOGGER.debug("[compat] RealCamera UV bind fell back to bone axis: {}", t.toString());
            return null;
        }
    }

    /** UV 命中槽位记录：quad 所在骨名 + quad（positions/uvs/normal 均为骨局部/归一化 UV，与渲染同源）。 */
    private static final class QuadHit {
        final String boneName;
        final GeoModel.BakedQuad quad;

        QuadHit(String boneName, GeoModel.BakedQuad quad) {
            this.boneName = boneName;
            this.quad = quad;
        }
    }

    /** 空 UV 候选槽（替代旧 null 槽语义：无命中面）。 */
    private static final QuadHit[] NO_HIT = new QuadHit[0];

    /** UV 路径回退原因打点（仅 harness armed 时 INFO，生产 debug 级保持静默）。 */
    private static void uvFallbackLog(String reason) {
        if (probeDiffArmed()) {
            YesSteveModel.LOGGER.info("[compat][rc-probe-diff] uv-fallback reason={}", reason);
        }
    }

    /**
     * 三槽（pos/fwd/up）UV→quad 候选命中，按 (模型, target) 缓存（UV 布局=geo 构建期决定，模型重载
     * 换实例后由 WeakHashMap 失效）。外层 [槽]，内层数组=该 UV 的<b>全部</b>命中 quad（按烘焙遍历序，
     * [0]=首个命中=生效锚面，与探针 findPrimitives 同序；整组保留语义不回退）。
     */
    private static QuadHit[][] quadHits(AnimatedGeoModel model, Object target, float[] uv) {
        Map<String, QuadHit[][]> perModel = QUAD_HIT_CACHE.computeIfAbsent(model, m -> new ConcurrentHashMap<>());
        String key = String.valueOf(System.identityHashCode(target)) + '@' + uv[UV_POS_U] + ',' + uv[UV_POS_V]
                + ',' + uv[UV_FWD_U] + ',' + uv[UV_FWD_V] + ',' + uv[UV_UP_U] + ',' + uv[UV_UP_V];
        QuadHit[][] cached = perModel.get(key);
        if (cached != null) {
            return cached;
        }
        QuadHit[][] hits = new QuadHit[][]{
                uv[UV_POS_U] == 0.0f && uv[UV_POS_V] == 0.0f ? NO_HIT
                        : findQuadCandidates(model, uv[UV_POS_U], uv[UV_POS_V]),
                uv[UV_FWD_U] == 0.0f && uv[UV_FWD_V] == 0.0f ? NO_HIT
                        : findQuadCandidates(model, uv[UV_FWD_U], uv[UV_FWD_V]),
                uv[UV_UP_U] == 0.0f && uv[UV_UP_V] == 0.0f ? NO_HIT
                        : findQuadCandidates(model, uv[UV_UP_U], uv[UV_UP_V])
        };
        perModel.put(key, hits);
        return hits;
    }

    /**
     * UV 候选查找：遍历烘焙 quad，UV 在四顶点 UV 多边形内即命中（java.awt.Polygon + 1e6 截断
     * 量化，与 realcamera BuiltIterableBuffer.findPrimitives:71-102 同类同精度）。返回<b>全部</b>
     * 命中（烘焙序）：正常模型 UV 区不重叠=单候选；重叠 UV 时首个命中与探针一致。无命中返回空数组。
     */
    private static QuadHit[] findQuadCandidates(AnimatedGeoModel model, float u, float v) {
        List<GeoModel.BakedBone> baked = model.getGeoModel().bakedBones;
        if (baked == null) {
            return NO_HIT;
        }
        int px = (int) (UV_RESOLUTION * u);
        int pv = (int) (UV_RESOLUTION * v);
        ArrayList<QuadHit> found = new ArrayList<>(1);
        for (int bi = 0; bi < baked.size(); bi++) {
            GeoModel.BakedBone bone = baked.get(bi);
            for (int ci = 0; ci < bone.cubes.size(); ci++) {
                GeoModel.BakedCube cube = bone.cubes.get(ci);
                List<GeoModel.BakedQuad> quads = cube.quads;
                for (int qi = 0; qi < quads.size(); qi++) {
                    GeoModel.BakedQuad quad = quads.get(qi);
                    int[] us = new int[4];
                    int[] vs = new int[4];
                    for (int i = 0; i < 4; i++) {
                        us[i] = (int) (UV_RESOLUTION * quad.uvs[i * 2]);
                        vs[i] = (int) (UV_RESOLUTION * quad.uvs[i * 2 + 1]);
                    }
                    if (new Polygon(us, vs, 4).contains(px, pv)) {
                        found.add(new QuadHit(bone.name, quad));
                    }
                }
            }
        }
        return found.toArray(new QuadHit[0]);
    }

    /**
     * 表面点：命中面 3 顶点 UV 重心插值（VertexData.position 公式逐字，alpha/beta 同式），
     * 顶点 px/16 后经该 quad 骨链当前帧矩阵变换。仿射下先插值后变换 ≡ 探针的先变换后插值。
     */
    @Nullable
    private static Vector3f surfacePoint(PoseStack root, Map<String, Matrix4f> frameMats, AnimatedGeoModel model,
                                         Map<String, IBone> bonesByName, QuadHit hit, float u, float v) {
        Matrix4f mat = frameMatrix(root, frameMats, model, bonesByName, hit.boneName);
        if (mat == null) {
            return null;
        }
        float[] p = hit.quad.positions;
        float[] uvArr = hit.quad.uvs;
        float u0 = uvArr[0], v0 = uvArr[1], u1 = uvArr[2], v1 = uvArr[3], u2 = uvArr[4], v2 = uvArr[5];
        float denomA = (u0 - u1) * (v1 - v2) - (v0 - v1) * (u1 - u2);
        float denomB = (u1 - u2) * (v2 - v0) - (v1 - v2) * (u2 - u0);
        if (Math.abs(denomA) < 1.0e-9f || Math.abs(denomB) < 1.0e-9f) {
            return null; // 退化 UV 面（三点共线）：探针同样不可插值
        }
        float alpha = ((u - u1) * (v1 - v2) - (v - v1) * (u1 - u2)) / denomA;
        float beta = ((u - u2) * (v2 - v0) - (v - v2) * (u2 - u0)) / denomB;
        if (!Float.isFinite(alpha) || !Float.isFinite(beta)) {
            return null;
        }
        float w = 1.0f - alpha - beta;
        // positions 烘焙期已 /16 成块级（YSMFolderDeserializer.bakeFaceToRaw x/16f + cube 烘焙矩阵），
        // 与渲染顶点同单位（NativeModelRenderer:190 直接乘矩阵，无再缩放）——这里不 /16
        float x = alpha * p[0] + beta * p[3] + w * p[6];
        float y = alpha * p[1] + beta * p[4] + w * p[7];
        float z = alpha * p[2] + beta * p[5] + w * p[8];
        return mat.transformPosition(new Vector3f(x, y, z));
    }

    /** 命中面法线（骨局部）经链帧方向变换（探针 setForward/normal 同源语义）。 */
    @Nullable
    private static Vector3f quadDirection(PoseStack root, Map<String, Matrix4f> frameMats, AnimatedGeoModel model,
                                          Map<String, IBone> bonesByName, QuadHit hit) {
        Matrix4f mat = frameMatrix(root, frameMats, model, bonesByName, hit.boneName);
        if (mat == null) {
            return null;
        }
        float[] n = hit.quad.normal;
        return mat.transformDirection(new Vector3f(n[0], n[1], n[2]));
    }

    /** pos 骨链 Y 轴（upwardUV 无命中时的保底 up：模型竖直轴，同旧骨轴路径的世界朝上语义）。 */
    @Nullable
    private static Vector3f boneUpAxis(PoseStack root, Map<String, Matrix4f> frameMats, AnimatedGeoModel model,
                                       Map<String, IBone> bonesByName, String boneName) {
        Matrix4f mat = frameMatrix(root, frameMats, model, bonesByName, boneName);
        if (mat == null) {
            return null;
        }
        return mat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
    }

    /**
     * quad 骨的当前帧矩阵：根帧 push 后沿 BakedBone.parentIdx 祖先链（YSMClientMapper 烘焙期
     * 填充，与 GeoBone/IBone 同序）逐骨 RenderUtils.prepMatrixForBone（与渲染
     * NativeModelRenderer.calculateBoneMatrix:251-280 数学同源），栈顶即该骨局部（模型 px 坐标）
     * 空间。每槽骨各算一次，帧内缓存（三槽常同骨=一次）。
     */
    @Nullable
    private static Matrix4f frameMatrix(PoseStack root, Map<String, Matrix4f> frameMats, AnimatedGeoModel model,
                                        Map<String, IBone> bonesByName, String boneName) {
        Matrix4f cached = frameMats.get(boneName);
        if (cached != null) {
            return cached;
        }
        List<String> names = bakedAncestorNames(model, boneName);
        if (names == null) {
            return null;
        }
        root.pushPose();
        try {
            // 根→骨逐骨 prepMatrixForBone（父变换先应用；prepMatrixForBone 与渲染
            // calculateBoneMatrix 数学同源，见类 javadoc）
            for (String name : names) {
                IBone bone = bonesByName.get(name);
                if (bone == null) {
                    return null;
                }
                RenderUtils.prepMatrixForBone(root, bone);
            }
            Matrix4f mat = new Matrix4f(root.last().pose());
            frameMats.put(boneName, mat);
            return mat;
        } finally {
            root.popPose();
        }
    }

    /** 锚骨祖先链名表（根→锚骨，复用 BakedBone.parentIdx，与 frameMatrix 同源）；烘焙表缺失/骨名未命中返回 null。 */
    @Nullable
    private static List<String> bakedAncestorNames(AnimatedGeoModel model, String boneName) {
        List<GeoModel.BakedBone> baked = model.getGeoModel().bakedBones;
        if (baked == null) {
            return null;
        }
        int idx = -1;
        for (int i = 0; i < baked.size(); i++) {
            if (baked.get(i).name.equals(boneName)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return null;
        }
        ArrayList<String> names = new ArrayList<>(8);
        int depth = 0;
        while (idx >= 0 && depth++ < MAX_CHAIN_DEPTH) {
            GeoModel.BakedBone b = baked.get(idx);
            names.add(b.name);
            idx = b.parentIdx;
        }
        Collections.reverse(names);
        return names;
    }

    /** EMPTY 让位 + reason 限频打点：每 reason 前 {@link #EMPTY_LOG_BUDGET} 次打 INFO，之后每 1200 帧汇报一次累计。 */
    private static Object empty(String reason) {        int n = emptyReasons.merge(reason, 1, Integer::sum);
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

    /** target offsets 数值 {scale,x,y,z,pitch,yaw,roll}（反射缺席返回 null）。 */
    @Nullable
    private static float[] targetOffsets(Object target) {
        try {
            Object offsets = target.getClass().getMethod("offsets").invoke(target);
            Class<?> o = offsets.getClass();
            return new float[]{
                    (Float) o.getMethod("getScale").invoke(offsets),
                    (Float) o.getMethod("getX").invoke(offsets),
                    (Float) o.getMethod("getY").invoke(offsets),
                    (Float) o.getMethod("getZ").invoke(offsets),
                    (Float) o.getMethod("getPitch").invoke(offsets),
                    (Float) o.getMethod("getYaw").invoke(offsets),
                    (Float) o.getMethod("getRoll").invoke(offsets)};
        } catch (Throwable t) {
            return null;
        }
    }

    /** 官方消费镜像（旋转基）：正交化→列基[left|up|fwd]——BindResult.computeCamera:75-77 只读复刻。 */
    private static org.joml.Matrix3f mirrorRotation(Vector3f forward, Vector3f upward) {
        org.joml.Matrix3f rot = new org.joml.Matrix3f();
        Vector3f cross = new Vector3f(upward).cross(new Vector3f(forward));
        Vector3f up2 = new Vector3f(forward).cross(cross).normalize();
        Vector3f left = new Vector3f(up2).cross(new Vector3f(forward));
        rot.set(left, up2, new Vector3f(forward));
        return rot;
    }

    /** 官方消费镜像（位置段）：position += R·(z,y,x)·scale——BindResult.computeCamera:79-80 只读复刻，旋转段 offsets(yaw/pitch/roll) 不影响 position 不在此。 */
    private static Vector3f mirrorFinalPos(Vector3f position, Vector3f forward, Vector3f upward, float[] offs) {
        org.joml.Matrix3f rot = mirrorRotation(forward, upward);
        Vector3f offset = new Vector3f(offs[3], offs[2], offs[1]).mul(offs[0]).mul(rot);
        return new Vector3f(position).add(offset);
    }

    /**
     * 绑定成功打点预算槽：每成功帧 +1（{@link #logBindSuccess} 消费），首
     * {@link #BIND_LOG_BUDGET} 帧每帧，之后每 {@link #BIND_LOG_INTERVAL} 帧一次。
     */
    private static final int BIND_LOG_BUDGET = 3;
    private static final int BIND_LOG_INTERVAL = 600;
    private static int bindLogCount;

    private static boolean logAllowed() {
        bindLogCount++;
        return bindLogCount <= BIND_LOG_BUDGET || bindLogCount % BIND_LOG_INTERVAL == 0;
    }

    /**
     * 绑定成功数值打点（fix-rc-bindresult-completeness，任务卡 Phase3-1 数值化验收）：
     * 喂入三字段 + target 配置摘要 + 按官方消费公式预测的最终相机位置/yaw/pitch/roll，
     * 供与用户配置期望对账（红线：禁止看图验收）。ponytail: 「预测最终」是
     * BindResult.computeCamera（8b82d0deec18 BindResult.java:72-84）+
     * MathUtil.getEulerAngleYXZ（MathUtil.java:13-20）的只读日志镜像，不参与相机；
     * 官方改消费语义时需同步本镜像。镜像自身抛错仅降级为少打一行，绝不影响绑定。
     */
    private static void logBindSuccess(Object target, Vector3f position, Vector3f forward, Vector3f upward) {
        if (!logAllowed()) {
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
            float[] offs = targetOffsets(target);
            if (offs == null) {
                return;
            }
            Class<?> b = bindConfig.getClass();
            float scale = offs[0];
            float ox = offs[1];
            float oy = offs[2];
            float oz = offs[3];
            float op = offs[4];
            float oyw = offs[5];
            float orl = offs[6];
            // 官方消费镜像：正交化→列基[left|up|fwd]→position+=R·(z,y,x)·scale→rotateLocal(yaw,pitch,roll)
            org.joml.Matrix3f rot = mirrorRotation(forward, upward);
            Vector3f finalPos = mirrorFinalPos(position, forward, upward, offs);
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

    /**
     * 渲染根帧 yaw 基准（diag-rc-anchor-space-mismatch）：与可见模型渲染根同源——
     * GeoReplacedEntityRenderer.renderEntityWithTexture:234 的 setupRotations 实参=
     * modelData.lerpBodyRot（AnimatableEntity.processAnimationImpl:262
     * =Mth.rotLerp(pT,yBodyRotO,yBodyRot)），dispatcher.render 的 entityYaw（view yaw）
     * 实参渲染器不消费（仅名牌渲染，GeoReplacedEntityRenderer.java:283）。骑乘活体载具
     * 分支逐镜像 AnimatableEntity:267-279。返回 {lerpBodyRot, rawNetHeadDeg}
     * （后者=lerpHeadRot-lerpBodyRot 原始值未取负未钳制，供空间对账打点判读；
     * 进骨的是其取负钳制态 AnimatableEntity:286，与基准无关）。
     *
     * <p><b>fix-rc-transform-matrix M4-③ 同帧同源证明（1.20.1）</b>：审计曾疑本自算基准
     * 与渲染消费的 eval modelData 有 partialTick 微差——实证不存在，理由链：
     * ①formula 逐字同源（本方法 ≡ processAnimationImpl:262-287，字段同组、运算同序）；
     * ②partialTick 同源——RealCamera 的 deltaTick=MixinGameRenderer 注入
     * GameRenderer.renderLevel 形参（MixinGameRenderer.java:49-57），vanilla 1.20.1
     * GameRenderer.renderLevel 把该形参<b>原样透传</b>给 levelRenderer.renderLevel
     * （GameRenderer.java:1254→:1303），WorldRendererMixin（>=1.19.3&&<1.20.5 分支）再以
     * 同一形参调 EntityRenderCache.tick → submitAsyncUpdate(partialTick)
     * （EntityRenderCache.java:24-58，GeoEntity.java:216-228）——即 eval 与本函数收到
     * <b>同一 float</b>；③EntityRenderCache.tick 是 renderLevel 内<b>每帧</b>钩子而非
     * per-tick（审计"eval tick 时值"前提不成立），async event 在同帧 entity render 被
     * join（GeoEntity.java:243-244）；④yBodyRot/yBodyRotO 仅在 client tick 变更，而
     * client tick 与 renderLevel 不同相位（Minecraft.runTick 先 tick 后 render），帧内恒定。
     * 故本自算基准 ≡ 渲染 eval modelData.lerpBodyRot（逐位），改读 eval 输出无通道且无差值
     * ——保留公式镜像并以此证明落账，未引入死代码。
     */
    private static float[] rendererLerpBodyRot(Player player, float partialTick) {
        float lerpBodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float lerpHeadRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = lerpHeadRot - lerpBodyRot;
        if (player.isPassenger() && player.getVehicle() instanceof LivingEntity vehicle
                && EntityDataBridge.shouldRiderSit(player.getVehicle())) {
            lerpBodyRot = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(lerpHeadRot - lerpBodyRot), -85.0f, 85.0f);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;
            if (clampedHeadYaw * clampedHeadYaw > 2500.0f) {
                lerpBodyRot += clampedHeadYaw * 0.2f;
            }
            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }
        return new float[]{lerpBodyRot, netHeadYaw};
    }

    /**
     * 绑定根帧=可见模型/UV 探针渲染空间（diag-rc-anchor-space-mismatch 空间簿记 + 本卡
     * fix-rc-transform-matrix M4 情境态收口，矩阵序逐项镜像渲染链）：
     * <ol>
     * <li>{@code GeoReplacedEntityRenderer.java:230-233} SLEEPING 平移（根旋转之前、世界轴向）：
     * {@code translate(-bed.stepX*eyeHeight, 0, -bed.stepZ*eyeHeight)}，eyeHeight=
     * {@code getEyeHeight(Pose.STANDING)-0.1f}；</li>
     * <li>{@code :234} setupRotations 的根旋转（vanilla 1.20.1 LivingEntityRenderer.java:178-199
     * 逐项镜像）：非 SLEEPING（或死亡/旋转攻击覆盖 SLEEPING 分支时）=
     * {@code mulPose(180-lerpBodyRot)}；SLEEPING 稳态（deathTime<=0 且非旋转攻击）=
     * 床向翻转分支 {@code R_y(sleepDirectionToRotation(bed)|lerpBodyRot)·R_z(90)·R_y(270)}
     * （{@code getFlipDegrees}=90，LivingEntityRenderer.java:213-215；死亡/旋转攻击期间的
     * SLEEPING 属濒死态、RC 锚点无意义，落到 180-body 不另镜像死亡旋转）；</li>
     * <li>{@code :235-257} 载具 expressionOffset 旋转（根旋转之后、translate(0,0.01,0) 之前）：
     * {@code mulPose(rotateZYX(offset.z,0,offset.x).invert())}；</li>
     * <li>{@code :259} translate(0,0.01,0) → IGeoRenderer.renderEarly scale（上游变量名互换
     * 原样：x/z=getHeightScale、y=getWidthScale）→ 骨链。</li>
     * </ol>
     * identity PoseStack 下该空间=世界轴向、实体脚原点——即 MixinCamera.getRawPos
     * (RealCameraCore.java:66-70) 直加 entityPos 所要求的 BindResult 契约空间（UV 探针
     * RealCameraCore.computeCamera:89 空 PoseStack dispatcher.render 捕获同空间，
     * CustomPlayerRenderer.render:88 同渲染链）。无睡眠/无载具时与旧实现逐位一致（默认路径
     * 数值零差：新增分支全部被 pose/vehicle 条件短路）。
     */
    private static PoseStack bindRootFrame(Player player, float lerpBodyRotDeg, PlayerCapability cap) {
        PoseStack root = new PoseStack();
        if (player.getPose() == Pose.SLEEPING && player.getBedOrientation() != null) {
            // GeoReplacedEntityRenderer.java:230-233 逐字（eyeHeight 语义同款）
            float eyeHeight = player.getEyeHeight(Pose.STANDING) - 0.1f;
            Direction bedOrientation = player.getBedOrientation();
            root.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
        }
        // rc-closure-mirror-gaps：setupRotations 的 yaw 链镜像（GeoReplacedEntityRenderer.java:411-452
        // → vanilla 1.20.1 LivingEntityRenderer.java:173-203，分支序=渲染链真相）：
        // climbable yaw 整体替换（GeoReplacedEntityRenderer.java:420-435：onClimbable&&
        // getLastClimbablePos&&FACING 三条件 → getOpposite().get2DDataValue()*90，非叠加）
        // → isShaking 抖动叠加（vanilla :174-176：this.isShaking 默认实现=entity.isFullyFrozen()
        // :169-171，我方渲染器无覆盖；cos(tickCount*3.25)*PI*0.4F 弧度值直进 rotationDegrees）
        // → 根旋转（:178-180 非睡 180-yaw / :193-198 床向翻转，睡优先）→ Dinnerbone 平移+ZP180
        // （:199-202 else-if 链尾：濒死/旋转攻击/睡眠优先——前两者旗标被我方 :412-419+446-451
        // 中和，渲染链实际只余睡眠优先；睡濒死边沿差=fix-rc-transform-matrix 已声明取舍）。
        float yaw = lerpBodyRotDeg;
        if (player.onClimbable()) {
            Optional<BlockPos> ladderPos = player.getLastClimbablePos();
            if (ladderPos.isPresent()) {
                //? if <1.20
                /*Optional<Direction> ladderFacing = player.getLevel().getBlockState(ladderPos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);*/
                //? if >=1.20
                Optional<Direction> ladderFacing = player.level().getBlockState(ladderPos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (ladderFacing.isPresent()) {
                    yaw = ladderFacing.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }
        if (player.isFullyFrozen()) {
            yaw += (float) (Math.cos(player.tickCount * 3.25) * Math.PI * 0.4f);
        }
        if (player.getPose() == Pose.SLEEPING && player.deathTime <= 0 && !player.isAutoSpinAttack()) {
            // vanilla 1.20.1 LivingEntityRenderer.setupRotations SLEEPING 分支逐字
            // （:193-198：180-body 分支在 SLEEPING 时被 :178-180 跳过，改走床向翻转；
            // 床向缺失时的 $$3 兜底含 climbable/isShaking 修正后的 yaw，同源 :195）
            Direction bedOrientation = player.getBedOrientation();
            float sleepRot = bedOrientation != null ? sleepDirectionToRotation(bedOrientation) : yaw;
            root.mulPose(Axis.YP.rotationDegrees(sleepRot));
            root.mulPose(Axis.ZP.rotationDegrees(90.0f));
            root.mulPose(Axis.YP.rotationDegrees(270.0f));
        } else {
            root.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
            if (LivingEntityRenderer.isEntityUpsideDown(player)) {
                // vanilla :199-202 逐字：translate(0, bbHeight+0.1, 0) + ZP180
                //（静态公共方法直调=同源判定：profile 名 Dinnerbone/Grumm+玩家 CAPE 层开）
                root.translate(0.0f, player.getBbHeight() + 0.1f, 0.0f);
                root.mulPose(Axis.ZP.rotationDegrees(180.0f));
            }
        }
        if (player.getVehicle() != null) {
            // GeoReplacedEntityRenderer.java:235-257 逐字（1.19.3 分支：rotateZYX(z,0,x).invert()）
            VehicleCapability vehicleCap = VehicleCapability.get(player.getVehicle()).orElse(null);
            if (vehicleCap != null) {
                Vector3f expressionOffset = vehicleCap.getExpressionOffset();
                if (expressionOffset != null) {
                    root.mulPose(new Quaternionf().rotateZYX(expressionOffset.z(), 0.0f, expressionOffset.x()).invert());
                }
            }
        }
        root.translate(0.0f, 0.01f, 0.0f);
        root.scale(cap.getHeightScale(), cap.getWidthScale(), cap.getHeightScale());
        return root;
    }

    /** vanilla 1.20.1 LivingEntityRenderer.sleepDirectionToRotation 逐字镜像（:154-168）。 */
    private static float sleepDirectionToRotation(Direction direction) {
        switch (direction) {
            case SOUTH:
                return 90.0f;
            case WEST:
                return 0.0f;
            case NORTH:
                return 270.0f;
            case EAST:
                return 180.0f;
            default:
                return 0.0f;
        }
    }

    /** 空间对账打点门控：仅 |netHead|>5°（转头瞬态/移动/滞空态），前 {@link #SPACE_DIAG_BUDGET} 次每帧，之后每 {@link #SPACE_DIAG_INTERVAL} 帧一次。 */
    private static final int SPACE_DIAG_BUDGET = 40;
    private static final int SPACE_DIAG_INTERVAL = 1200;
    private static int spaceDiagCount;

    private static boolean spaceDiagAllowed(float netHeadYawDeg) {
        if (Math.abs(netHeadYawDeg) < 5.0f) {
            return false;
        }
        spaceDiagCount++;
        return spaceDiagCount <= SPACE_DIAG_BUDGET || spaceDiagCount % SPACE_DIAG_INTERVAL == 0;
    }

    /**
     * 空间对账数值打点：fed（当前代码基准）与对照基准（view-yaw=M1 旧行为）同骨链独立重算
     * （调用方已算好传入），各按官方消费镜像（offsets R·(z,y,x)·scale，BindResult.java:79）
     * 得 final，dFinal=alt−fed。判读（任务卡 c/d 判据）：修复提交 fed=体转基准=契约空间，
     * dFinal≈−(R(−netHead)−I)·fedFinal（解析对账，随 netHead 周期性、netHead→0 时→0）；
     * 本提交回滚（revert feed 基准）后 fed/alt 列互换。
     */
    private static void logSpaceDiag(float viewRot, float bodyRot, float netHead,
            Vector3f fedPos, Vector3f fedFwd, Vector3f fedUp,
            Vector3f altPos, Vector3f altFwd, Vector3f altUp, Object target) {
        try {
            float[] offs = targetOffsets(target);
            String f = "(%.4f, %.4f, %.4f)";
            Vector3f fedFinal = offs == null ? null : mirrorFinalPos(fedPos, fedFwd, fedUp, offs);
            Vector3f altFinal = offs == null ? null : mirrorFinalPos(altPos, altFwd, altUp, offs);
            String dFinal = fedFinal == null || altFinal == null ? "n/a"
                    : String.format(f, altFinal.x() - fedFinal.x(), altFinal.y() - fedFinal.y(), altFinal.z() - fedFinal.z())
                            + String.format(" |d|=%.4f", new Vector3f(altFinal).sub(fedFinal).length());
            YesSteveModel.LOGGER.info(
                    "[compat] RealCamera space-diag #{} viewYaw={} bodyYaw={} netHead={} fedPos={} altPos={} fedFinal={} altFinal={} dFinal={}",
                    spaceDiagCount,
                    String.format("%.2f", viewRot), String.format("%.2f", bodyRot), String.format("%.2f", netHead),
                    String.format(f, fedPos.x(), fedPos.y(), fedPos.z()),
                    String.format(f, altPos.x(), altPos.y(), altPos.z()),
                    fedFinal == null ? "n/a" : String.format(f, fedFinal.x(), fedFinal.y(), fedFinal.z()),
                    altFinal == null ? "n/a" : String.format(f, altFinal.x(), altFinal.y(), altFinal.z()),
                    dFinal);
        } catch (Throwable t) {
            YesSteveModel.LOGGER.debug("[compat] RealCamera space-diag log failed: {}", t.toString());
        }
    }

    // ==================== fix-rc-probe-diff：探针差分 harness（生产零行为） ====================
    //
    // 任务：B1 预测表面点（烘焙 quad 重心插值+骨链矩阵）vs RC 探针实际捕获顶点（真值）逐姿势差分。
    // 真值=复用 realcamera-8b82d0d YSMCompat.computeBindResult 的捕获逻辑（任务卡"直调其类"授权；
    // 首轮直调实证：运行时 realcamera jar 的该函数对本 mod 恒 EMPTY——textureId 来自
    // RenderType.toString() 的 texture[Optional[...]] 正则（BuiltIterableBuffer.buildFrom），本 mod
    // 半透明纹理走 CustomEntityTranslucentRenderType（name=entity_translucent_ysm）其 toString 无
    // 纹理段 → getBindTargetList(textureId) 零匹配。这就是"探针在世内两路都失败"（诊断账 C4）
    // 的根因；官方 YSM 用常规类型故官方集成不受影响。故本 harness 改为同逻辑自管捕获：直用
    // realcamera 的 MultiVertexCatcher/BuiltIterableBuffer/VertexData 工具（javap 对运行时
    // realcamera-dev.jar 实证：renderer.MultiVertexCatcher、renderer.BuiltIterableBuffer、
    // renderer.state.VertexData$UV/position/normal），绕开 BindTarget/textureId 匹配，UV 直查。
    //
    // 门控：probeDiffArmed()=gameDir/harness.armed 文件存在（与 GuiTourDriver 同款懒查缓存）。
    // 生产无该文件 → 静态布尔短路，零反射、零渲染、零行为。
    // 额外成本（仅 harness）：每帧最多 4 次 dispatcher.render（官方 YSM+RC 集成的常态工作模式）。
    //
    // 差分分解（定位"锚点与渲染表面距离随姿势变化"的来源）：
    //   dStale = b1Before − b1After：B1 读到的 matrixData 相位（camera setup 读上一 eval）
    //     vs 探针同步求值后的 matrixData——纯"求值时机/分支"贡献（z=false 异步 tick 门控
    //     vs z=true 同步全量，GeoEntity.shouldSkipAnimation/isFirstPerson 分叉）。
    //   dStruct = probe − b1After：同帧同 matrixData 下，烘焙 quad+骨链数学 vs 实际渲染顶点
    //     流——纯"预测链结构"贡献（候选：UV 采样点插值/cube 级通道/渲染期骨处理/探针逆旋闭合差）。
    // 同时镜像链值（posY/rotX 逐骨）与 RC 消费侧 lastResult，供姿势间对账。

    /** harness armed 门懒查缓存（生产=FALSE 恒短路）。 */
    private static Boolean diffArmedCache;

    private static boolean probeDiffArmed() {
        Boolean b = diffArmedCache;
        if (b == null) {
            try {
                b = java.nio.file.Files.exists(
                        net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("harness.armed"));
            } catch (Throwable t) {
                b = Boolean.FALSE;
            }
            diffArmedCache = b;
        }
        return b.booleanValue();
    }

    /** 探针捕获反射面（运行时 realcamera jar，javap 实证），失败置 probeToolsUnavailable 一次。 */
    private static boolean probeToolsUnavailable;
    private static Method mCatcherDefaultImpl;
    private static Method mCatcherEndCatching;
    private static Method mFindPrimitives;
    private static Class<?> clsVdUv;
    private static Constructor<?> ctorVertexUv;
    private static Method mVdPosition;
    private static Method mVdNormal;
    /** BuiltIterableBuffer record 组件 textureId()——buffer 门（挡掉阴影/手持等非模型 buffer）。 */
    private static Method mBufferTextureId;
    /** 几何最近匹配面（UV miss 兜底）：vertexBuffer()/primitives()/VertexData 坐标 UV 访问。 */
    private static Method mBufferVertexBuffer;
    private static Method mPrimitives;
    private static java.lang.reflect.Field mVbVertexCount;
    private static Method mVdX;
    private static Method mVdY;
    private static Method mVdZ;
    private static Method mVdU;
    private static Method mVdV;
    /** BindResult.getPosition()（实际声明在父类 CameraTransform，getMethod 沿继承链可查）。 */
    private static Method mBindResultGetPosition;

    /**
     * 探针差分本体（仅 armed 时被 {@code tryUvBind} 调用）。任何异常只打点、绝不影响绑定返回值。
     * 序：①链值快照→②临时解除 AllHead 隐藏（隐藏骨不发顶点，探针需捕获脸部）→③自管 4-pass
     * 捕获（同 YSMCompat 常量 pitch/yaw 四方向、空 PoseStack 预乘逆旋、UV 直查捕获顶点、
     * 逆矩阵旋回实体本地空间）→④恢复隐藏态→⑤同 quad 同公式重算 B1（探针同步求值已刷新
     * matrixData）→⑥读 RC 消费侧 lastResult→⑦逐姿势差分打点。
     */
    private static void runProbeDiff(Player player, AnimatedGeoModel model, PlayerCapability cap, QuadHit posHit,
                                     float[] uv, Vector3f b1Before, Vector3f b1Fwd, Vector3f b1Up,
                                     float bodyRot, float partialTick, Map<String, IBone> bonesByName,
                                     List<IBone> boneAxisChain, Object target) {
        try {
            if (!initProbeTools()) {
                return;
            }
            // buffer 门：官方探针按 target.textureId 对 BuiltIterableBuffer.textureId 做 contains
            // 匹配（ModConfig.getBindTargetList）；无此门时实体阴影 quad（UV 覆盖 [0,1]² 全域）
            // 会抢在模型面片前命中。绕过 BindTarget 匹配后保留同一道门。
            String texGate = targetTextureId(target);
            if (bonesByName == null) {
                // 骨轴路径不预建表：此处按需构建（uv 路径由 tryUvBind 传入）
                bonesByName = new java.util.HashMap<>();
                for (IBone bone : model.bones().values()) {
                    bonesByName.put(bone.getName(), bone);
                }
            }
            String boneName = posHit == null ? "none" : posHit.boneName;
            String chainBefore = boneChainPoseDump(model, bonesByName, boneName);
            // ② AllHead 临时解隐（进入本方法时它通常仍是上帧 hideHead 的隐藏态）
            IBone allHead = model.allHeadBone();
            boolean headWasHidden = false;
            boolean headWasChildHidden = false;
            if (allHead != null) {
                headWasHidden = allHead.isHidden();
                headWasChildHidden = allHead.childBonesAreHiddenToo();
                allHead.setHidden(false, false);
            }
            // ③ 自管探针捕获（真值=实际渲染顶点）。置 firstPerson 标志镜像 renderLevel HEAD
            // 时序：探针同步求值走 isFirstPerson=true 分支——z=false 时 applyHeadTracking 的
            // FPM 重隐藏分支（PlayerCapability:135 !event.isFirstPerson() 守卫）不触发，
            // 解除隐藏的 AllHead 保持可见，脸部顶点得以捕获。
            com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer.setFirstPersonMode(true);
            Object[] captured;
            try {
                captured = probeCapture(player, uv, partialTick, texGate, b1Before);
            } finally {
                com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer.setFirstPersonMode(false);
            }
            // ④ 恢复隐藏态（外层 tryUvBind 尾部 hideHead 会按绑定语义再隐藏）
            if (allHead != null) {
                allHead.setHidden(headWasHidden, headWasChildHidden);
            }
            boolean headRehiddenDuringPasses = allHead != null && allHead.isHidden();
            // ⑤ 探针同步求值后的 B1 重算（同 quad 同公式/同骨链同 pivot，仅 matrixData 相位不同）
            PoseStack rootAfter = bindRootFrame(player, bodyRot, cap);
            Vector3f b1After;
            if (posHit != null) {
                b1After = surfacePoint(rootAfter, new java.util.HashMap<>(), model, bonesByName,
                        posHit, uv[UV_POS_U], uv[UV_POS_V]);
            } else {
                // 骨轴路径（rc-closure-mirror-gaps）：与 computeBind 主算同 helper 同序
                RenderUtils.prepMatrixForLocator(rootAfter, boneAxisChain);
                org.joml.Matrix4f matAfter = rootAfter.last().pose();
                Vector3f centerOffset = boneCubeCenterOffset(model, boneAxisChain.get(boneAxisChain.size() - 1));
                b1After = matAfter.transformPosition(centerOffset != null ? centerOffset : new Vector3f());
            }
            String chainAfter = boneChainPoseDump(model, bonesByName, boneName);
            // ⑥ RC 消费侧实况（本帧 computeCamera 尚未回写 → lastResult=上一帧喂入的平滑值）
            Vector3f rcLast = readRcLastResult();
            // ⑦ 差分打点
            Vector3f probePos = (Vector3f) captured[0];
            Vector3f probeFwd = (Vector3f) captured[1];
            Vector3f probeUp = (Vector3f) captured[2];
            boolean probeAvail = (Boolean) captured[3];
            float geoDist = (Float) captured[4];
            int vbVerts = (Integer) captured[6];
            String f = "(%.4f, %.4f, %.4f)";
            String dStruct = !probeAvail || b1After == null ? "n/a" : diffField(probePos, b1After);
            String dStale = b1After == null ? "n/a" : diffField(b1Before, b1After);
            String dFwd = !probeAvail ? "n/a" : String.format("%.2f deg", angleDeg(b1Fwd, probeFwd));
            String dUp = !probeAvail ? "n/a" : String.format("%.2f deg", angleDeg(b1Up, probeUp));
            YesSteveModel.LOGGER.info(
                    "[compat][rc-probe-diff] #{} pose={} shift={} pos={} b1Before={} b1After={} probe={} probeAvail={}"
                            + " dStruct=probe-b1After{} dStale=b1Before-b1After{} dFwdAngle={} dUpAngle={}"
                            + " st=climb:{}/frozen:{}/flip:{} fwdZ={} upY={}"
                            + " geoDist(px)={} vbVerts={} headRehidden={} rcLastFramePos={} chainBefore=[{}] chainAfter=[{}]",
                    bindLogCount, player.getPose(), player.isShiftKeyDown(),
                    String.format("(%.3f, %.3f, %.3f)", player.getX(), player.getY(), player.getZ()),
                    String.format(f, b1Before.x(), b1Before.y(), b1Before.z()),
                    b1After == null ? "n/a" : String.format(f, b1After.x(), b1After.y(), b1After.z()),
                    probePos == null ? "n/a" : String.format(f, probePos.x(), probePos.y(), probePos.z()),
                    probeAvail, dStruct, dStale, dFwd, dUp,
                    player.onClimbable(), player.isFullyFrozen(),
                    net.minecraft.client.renderer.entity.LivingEntityRenderer.isEntityUpsideDown(player),
                    String.format("%.3f", b1Fwd.z()),
                    String.format("%.3f", b1Up == null ? Float.NaN : b1Up.y()),
                    Float.isInfinite(geoDist) ? "n/a" : String.format("%.2f", geoDist * 16.0f),
                    vbVerts, headRehiddenDuringPasses,
                    rcLast == null ? "n/a" : String.format(f, rcLast.x(), rcLast.y(), rcLast.z()),
                    chainBefore, chainAfter);
        } catch (Throwable t) {
            YesSteveModel.LOGGER.info("[compat][rc-probe-diff] failed: {}", t.toString());
        }
    }

    /** 探针工具反射懒初始化；false=运行时 realcamera 面不符（只报一次）。 */
    private static boolean initProbeTools() {
        if (mCatcherDefaultImpl != null) {
            return true;
        }
        if (probeToolsUnavailable) {
            return false;
        }
        try {
            Class<?> clsCatcher = Class.forName("com.xtracr.realcamera.renderer.MultiVertexCatcher");
            mCatcherDefaultImpl = clsCatcher.getMethod("defaultImpl");
            mCatcherEndCatching = clsCatcher.getMethod("endCatching", java.util.function.Consumer.class);
            clsVdUv = Class.forName("com.xtracr.realcamera.renderer.state.VertexData$UV");
            Class<?> clsBuffer = Class.forName("com.xtracr.realcamera.renderer.BuiltIterableBuffer");
            mFindPrimitives = clsBuffer.getMethod("findPrimitives", clsVdUv.arrayType());
            ctorVertexUv = clsVdUv.getConstructor(float.class, float.class);
            Class<?> clsVd = Class.forName("com.xtracr.realcamera.renderer.state.VertexData");
            mVdPosition = clsVd.getMethod("position", clsVd.arrayType(), float.class, float.class);
            mVdNormal = clsVd.getMethod("normal", clsVd.arrayType());
            mVdX = clsVd.getMethod("x");
            mVdY = clsVd.getMethod("y");
            mVdZ = clsVd.getMethod("z");
            mVdU = clsVd.getMethod("u");
            mVdV = clsVd.getMethod("v");
            mBufferTextureId = clsBuffer.getMethod("textureId");
            mBufferVertexBuffer = clsBuffer.getMethod("vertexBuffer");
            Class<?> clsVb = Class.forName("com.xtracr.realcamera.renderer.IterableVertexBuffer");
            mPrimitives = clsVb.getMethod("primitives");
            mVbVertexCount = clsVb.getField("vertexCount");
            mBindResultGetPosition = Class.forName("com.xtracr.realcamera.api.BindResult").getMethod("getPosition");
            return true;
        } catch (Throwable t) {
            probeToolsUnavailable = true;
            YesSteveModel.LOGGER.info("[compat][rc-probe-diff] probe tools unavailable: {}", t.toString());
            return false;
        }
    }

    /**
     * 自管 4-pass 捕获（YSMCompat.TransformedVertexRecorder 同构）：空 PoseStack 预乘<b>正向</b>
     * 旋转（pitch=1.9106332f/yaw=2.0943951f 四方向，覆盖背面剔除各朝向；官方序=pose·mul(R)、
     * 捕获点·mulPosition(R⁻¹) 旋回）→ dispatcher.render 进 MultiVertexCatcher → endCatching
     * 逐 buffer findPrimitives UV 直查 → 命中面 3 顶点重心插值（VertexData.position）；法线
     * VertexData.normal·mul(R⁻¹)。UV miss 时几何兜底：全 primitive 质心逆旋回后与预测点
     * {@code b1Before} 比距，最近者即真值面（无需 UV 对上，直接量化表面偏差）。
     * 返回 {position, forward, upward, available, geoDistance, geoCentroid}。
     */
    private static Object[] probeCapture(Player player, float[] uv, float partialTick, String texGate,
                                         Vector3f predicted) throws Exception {
        final float pitch = 1.9106332f, yaw = 2.0943951f;
        Minecraft client = Minecraft.getInstance();
        Entity entity = client.getCameraEntity() != null ? client.getCameraEntity() : player;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        Object uvPos = ctorVertexUv.newInstance(uv[UV_POS_U], uv[UV_POS_V]);
        Object uvFwd = ctorVertexUv.newInstance(uv[UV_FWD_U], uv[UV_FWD_V]);
        Object uvUp = ctorVertexUv.newInstance(uv[UV_UP_U], uv[UV_UP_V]);
        Object uvArr = java.lang.reflect.Array.newInstance(clsVdUv, 3);
        java.lang.reflect.Array.set(uvArr, 0, uvPos);
        java.lang.reflect.Array.set(uvArr, 1, uvFwd);
        java.lang.reflect.Array.set(uvArr, 2, uvUp);
        Vector3f[] slotValue = new Vector3f[3];
        boolean[] slotFound = new boolean[3];
        float[] geoBest = {Float.MAX_VALUE};
        Vector3f[] geoAnchor = new Vector3f[1];
        Vector3f[] geoNormal = new Vector3f[1];
        int[] vbVerts = new int[1];
        for (int pass = 0; pass < 4 && !(slotFound[0] && slotFound[1] && slotFound[2]); pass++) {
            float p = pass == 0 ? 0.0f : pitch;
            float y = pass == 2 ? yaw : pass == 3 ? 2 * yaw : 0.0f;
            org.joml.Matrix4f rot4 = new org.joml.Matrix4f().rotationYXZ(y, p, 0);
            org.joml.Matrix3f rot3 = new org.joml.Matrix3f().rotationYXZ(y, p, 0);
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().mul(rot4);
            poseStack.last().normal().mul(rot3);
            Object catcher = mCatcherDefaultImpl.invoke(null);
            dispatcher.render(entity, 0, 0, 0,
                    Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick, poseStack,
                    (MultiBufferSource) catcher, dispatcher.getPackedLightCoords(entity, partialTick));
            final org.joml.Matrix4f inv4 = rot4.invert(new org.joml.Matrix4f());
            final org.joml.Matrix3f inv3 = rot3.invert(new org.joml.Matrix3f());
            final Vector3f predictedF = predicted == null ? new Vector3f() : new Vector3f(predicted);
            mCatcherEndCatching.invoke(catcher, (java.util.function.Consumer<Object>) buffer -> {
                try {
                    if (texGate != null) {
                        String bufTex = String.valueOf(mBufferTextureId.invoke(buffer));
                        // 官方门=bufTex.contains(target.textureId)；本 mod 半透明纹理走
                        // CustomEntityTranslucentRenderType，其 toString 无纹理段（=C4 根因），
                        // 补自定义类型名白名单；阴影 buffer 显式排除（UV [0,1]² 全域必命中）。
                        boolean modelBuffer = bufTex.contains(texGate) || bufTex.contains("entity_translucent_ysm");
                        if (!modelBuffer || bufTex.contains("shadow")) {
                            logBufferDiag(bufTex, "gate-skipped");
                            return;
                        }
                    }
                    Object[] slots = (Object[]) mFindPrimitives.invoke(buffer, uvArr);
                    if (slots[0] == null) {
                        logBufferDiag(String.valueOf(mBufferTextureId.invoke(buffer)), "uv-miss");
                    }
                    for (int slot = 0; slot < 3; slot++) {
                        if (slots[slot] == null) {
                            continue;
                        }
                        if (slot == 0) {
                            net.minecraft.world.phys.Vec3 v = (net.minecraft.world.phys.Vec3) mVdPosition
                                    .invoke(null, slots[slot], uv[UV_POS_U], uv[UV_POS_V]);
                            slotValue[0] = new Vector3f((float) v.x, (float) v.y, (float) v.z).mulPosition(inv4);
                            geoBest[0] = 0.0f;
                        } else {
                            net.minecraft.world.phys.Vec3 n = (net.minecraft.world.phys.Vec3) mVdNormal
                                    .invoke(null, slots[slot]);
                            Vector3f dir = new Vector3f((float) n.x, (float) n.y, (float) n.z).mul(inv3);
                            slotValue[slot] = dir;
                        }
                        slotFound[slot] = true;
                    }
                    // 几何兜底（UV miss 时）：全 primitive 质心逆旋 vs 预测点最近者=真值面
                    if (slots[0] == null && !slotFound[0]) {
                        Object vb = mBufferVertexBuffer.invoke(buffer);
                        vbVerts[0] = Math.max(vbVerts[0], (Integer) mVbVertexCount.get(vb));
                        float best = Float.MAX_VALUE;
                        Vector3f bestAnchor = null;
                        Vector3f bestNormal = null;
                        java.util.Iterator<?> it = ((java.lang.Iterable<?>) mPrimitives.invoke(vb)).iterator();
                        while (it.hasNext()) {
                            Object primObj = it.next();
                            Object[] prim = (Object[]) primObj;
                            int n = java.lang.reflect.Array.getLength(prim);
                            if (n < 3) {
                                continue;
                            }
                            float cx = 0, cy = 0, cz = 0;
                            for (int i = 0; i < n; i++) {
                                cx += (Float) mVdX.invoke(prim[i]);
                                cy += (Float) mVdY.invoke(prim[i]);
                                cz += (Float) mVdZ.invoke(prim[i]);
                            }
                            Vector3f c = new Vector3f(cx / n, cy / n, cz / n).mulPosition(inv4);
                            float d = c.distanceSquared(predictedF);
                            if (d < best) {
                                best = d;
                                bestAnchor = c;
                                // Object[] 实参必须再包一层：否则被 Method.invoke 的 varargs 展开
                                net.minecraft.world.phys.Vec3 nn = (net.minecraft.world.phys.Vec3) mVdNormal
                                        .invoke(null, new Object[]{prim});
                                bestNormal = new Vector3f((float) nn.x, (float) nn.y, (float) nn.z).mul(inv3);
                            }
                        }
                        if (best < geoBest[0] && bestAnchor != null) {
                            geoBest[0] = best;
                            geoAnchor[0] = bestAnchor;
                            geoNormal[0] = bestNormal;
                        }
                    }
                } catch (Throwable t) {
                    YesSteveModel.LOGGER.info("[compat][rc-probe-diff] buffer consume failed: {}", t.toString());
                }
            });
        }
        // UV 命中失败但几何命中（质心距预测 ≤0.25 块=4px，同一面片级）：以几何锚点为真值
        if (!slotFound[0] && geoAnchor[0] != null && geoBest[0] <= 0.25f * 0.25f) {
            slotValue[0] = geoAnchor[0];
            slotValue[1] = geoNormal[0];
            slotValue[2] = null;
            slotFound[0] = true;
        }
        return new Object[]{slotValue[0], slotValue[1], slotValue[2], slotFound[0],
                (float) Math.sqrt(geoBest[0]), geoAnchor[0], vbVerts[0]};
    }

    /** buffer 诊断日志（每 textureId+原因只打一次，harness 观测用；生产 armed 门内不触达）。 */
    private static final java.util.Set<String> BUFFER_DIAG_SEEN = java.util.Collections.newSetFromMap(new java.util.HashMap<>());

    private static void logBufferDiag(String textureId, String reason) {
        if (BUFFER_DIAG_SEEN.add(reason + '|' + textureId)) {
            YesSteveModel.LOGGER.info("[compat][rc-probe-diff] buffer {} textureId={}", reason, textureId);
        }
    }

    /** 向量夹角（deg），任一零向量返回 NaN。 */
    private static float angleDeg(Vector3f a, Vector3f b) {
        if (a == null || b == null || a.lengthSquared() < 1.0e-12f || b.lengthSquared() < 1.0e-12f) {
            return Float.NaN;
        }
        float dot = Math.max(-1.0f, Math.min(1.0f, new Vector3f(a).normalize().dot(new Vector3f(b).normalize())));
        return (float) Math.toDegrees(Math.acos(dot));
    }

    /** target.textureId() 反射读取（探针 buffer 门用）；缺席返回 null=不过滤。 */
    @Nullable
    private static String targetTextureId(Object target) {
        try {
            return (String) target.getClass().getMethod("textureId").invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 差分字段：Δ向量（块）+ 模长（块与 px 双单位）。 */
    private static String diffField(Vector3f a, Vector3f b) {        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return String.format("=(%.4f, %.4f, %.4f) |%.4f|blk %.2fpx", dx, dy, dz, len, len * 16.0f);
    }

    /** 锚骨祖先链逐骨动画值快照（posY px / rotX deg），探针求值前后对账用。 */
    private static String boneChainPoseDump(AnimatedGeoModel model, Map<String, IBone> bonesByName, String boneName) {
        List<String> names = bakedAncestorNames(model, boneName);
        if (names == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder(128);
        for (String name : names) {
            IBone bone = bonesByName.get(name);
            if (bone == null) {
                continue;
            }
            sb.append(name)
                    .append('(').append(String.format("%.3f", bone.getPositionY()))
                    .append('/').append(String.format("%.2f", Math.toDegrees(bone.getRotationX())))
                    .append(')');
        }
        return sb.toString();
    }

    /**
     * RC 消费侧实况读取：RealCameraCore.lastResult（private static BindResult）的 position
     * =上一帧喂入经 computeCamera 后的值（SmoothUtil 平滑态）。反射失败返回 null（打点降级）。
     */
    @Nullable
    private static Vector3f readRcLastResult() {
        try {
            Class<?> clsCore = Class.forName("com.xtracr.realcamera.RealCameraCore");
            java.lang.reflect.Field fLast = clsCore.getDeclaredField("lastResult");
            fLast.setAccessible(true);
            Object lastResult = fLast.get(null);
            if (lastResult == null) {
                return null;
            }
            Method getPos = Class.forName("com.xtracr.realcamera.api.BindResult").getMethod("getPosition");
            net.minecraft.world.phys.Vec3 p = (net.minecraft.world.phys.Vec3) getPos.invoke(lastResult);
            return new Vector3f((float) p.x, (float) p.y, (float) p.z);
        } catch (Throwable t) {
            return null;
        }
    }
    //? }
}
