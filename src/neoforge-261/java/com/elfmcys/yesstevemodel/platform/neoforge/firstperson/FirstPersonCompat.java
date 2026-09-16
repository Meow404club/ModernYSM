package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import rip.ysm.compat.oculus.OculusCompat;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * neoforge 26.1 三线（26.1/26.1.1/26.1.2）FirstPersonModel 真 compat 孪生
 * （fpm-26x-pr659 卡A + 26.1.2 逆向追加单）。异包孪生（212/2610 树先例）：
 * exclude 按相对路径双杀同名 RAW 文件，故本类落 platform/neoforge 包，
 * 接缝消费方 import 经 stonecutter 行条件交换（PlayerCapability/
 * FirstPersonModHide/ReplacePlayerRenderEvent/ModelPreviewRenderer 四处，
 * AuthModelsCapability.java:3-6 同款缝）。
 *
 * 【旗标采样链（追加单必改项，全卡成败点）】官方 26.1.2 契约（反编译
 * tmp/harvest/ysm2612-reverse/src 实证）：extract 期把 FPM 旗标快照进随
 * state 走的相位 record（O0O0O0OOooOO00oOo0OooOOO.java 工厂 r3 位=
 * FirstPersonCompat.oo000Oo000o0Ooooooo00oOo()=installed&&
 * FirstPersonAPI.isRenderingPlayer()，O0o0O0O00aaa0aaa000a0ooo.java 7 布尔
 * record），动画 lazy eval/submit 期消费快照（AllHead 隐藏 O0OoOoO0oo0O00O0
 * ooo0o0OO.java:105-110 守卫读快照；molang first_person_mod_hide
 * OO00o0OOoO0oo0000oOoo0oO.java:11 直读同旗标）。原因：26.1 我方动画求值在
 * submit 期 lazy 触发，直查 isRenderingPlayer() 在 stock 与加宽版 FPM 下均
 * 不在旗标窗口内=恒 false。我方对位：
 *   采样=FirstPersonCompatSetupHook 注册的 RegisterRenderStateModifiersEvent
 *        modifier（extract 尾回调，neoforge-26.1
 *        RegisterRenderStateModifiersEvent.java:58 registerEntityModifier 追加
 *        语义，RenderStateExtensions.registerEntity:74 集合 add 多 modifier
 *        共存）调 {@link #sample()} 存入 FLAG_BY_STATE（随 state 走的槽）；
 *   消费=261 孪生 ReplacePlayerRenderForgeHook 在 RenderPlayerEvent.Pre 路由
 *        前 beginState(state) 置当前快照，setHidden 消费点（共享
 *        PlayerCapability:137）与 molang 门经 isFirstPersonActive()/shouldHideHead()
 *        读快照，路由后 endState() 复位。
 *
 * 【降级/检测语义（主会话 2026-09-16 裁决 + 2026-09-17 追加微调）】
 * 反射检测 dev.tr7zw.firstperson.FirstPersonModelCore.isRenderingPlayerRaw
 * （PR#659 新增方法，659.diff:34 实证；官方 2.7.2-26.1.2 jar javap 零命中）：
 * raw 存在=加宽版 FPM（isRenderingPlayer=base||cameraEntityExtract，extract
 * 窗口采样=true）→隐头生效；raw 不存在=stock FPM 旗标在 eval 期必然 down
 * →隐头不触发=与官方 stock FPM 行为一致（官方行为非缺陷，日志注明）。
 * 检测全程异常安全（Throwable 兜底）。isLoaded=FPM 在场（探测行数值口径）。
 *
 * 【三项官方排除（追加单必补）】
 * 1. RealCamera：modid realcamera 在场且 com.xtracr.realcamera.RealCameraCore
 *    .isActive()（官方 oOOo0o0oO0OoOoo00oOooo0o.java:6-17 + 硬引用助手
 *    Ooo0O00OoO0oO000oOooO0OO.java 语义；我方无 realcamera 编译 jar→反射
 *    Method 缓存，异常安全）→ 采样恒 false（不隐头；RC 视角全模）。
 * 2. Iris 阴影 pass：OculusCompat.isRenderingShadowPass()→采样恒 false
 *    （保持全模投影）。跨卡依赖：neoforge 线该 face 现为 2111 shim 恒 false
 *    （另一张卡接线后自动生效），按追加单要求只接调用不自建检测。
 * 3. GUI/预览相位：不设专门旗标——预览/InventoryScreen 的 state 走各自
 *    extract 路径，采样时不在 FPM extract 窗口内=恒 false（官方快照过滤器
 *    oOa0aOaaO0aOOO00a0aOaa00:36-38 同效），预览路径带头。
 *
 * 【offset handler】语义对齐官方 o00OaaOOaOOO0aaO0OaOO0O0.java:29-33（=
 * 1.20.1 同款 Vec3(x,1.5−cameraDistance,z)）：注册时机=FPM 在场即注册
 * （官方 o00OooOOoOOO0ooO0OoOO0O0.java:17-23），cameraDistance 由共享
 * PlayerCapability:139-143 写 ViewLocator 骨高度×scale、兜底 24/16。
 *
 * 编译依赖=libs/neoforge-261/firstperson-neoforge-2.7.2-mc26.1.2.jar
 * （compileOnly fileTree，build.forge.gradle.kts:92-94 libs 29 jar 同机制）。
 * FirstPersonAPI 符号仅被 sample/registerOffsetHandler 方法体引用（JVM 惰性
 * 解析）：FPM 缺席时这些路径不执行，零 NCDFE（forge 端同款短路结构）。
 */
public class FirstPersonCompat {

    private static final String MOD_ID_OLD = "firstpersonmod";

    private static final String MOD_ID_NEW = "firstperson";

    private static final String RC_MOD_ID = "realcamera";

    private static volatile float cameraDistance;

    private static boolean IS_LOADED;

    private static boolean HAS_PR659;

    /** RealCamera 反射面（init 期解析一次，采样期只 invoke）。 */
    private static boolean RC_LOADED;
    private static Method RC_IS_ACTIVE;

    /** 旗标采样槽（extract 期写、submit 期读；key=vanilla extract 的 state 实例）。 */
    private static final WeakHashMap<EntityRenderState, Boolean> FLAG_BY_STATE = new WeakHashMap<>();

    /** 当前 submit 窗口内的快照值（beginState/endState 维护，渲染线程单线程语义）。 */
    private static boolean renderStateFlag;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID_NEW) || ModList.get().isLoaded(MOD_ID_OLD);
        HAS_PR659 = IS_LOADED && detectPR659();
        if (IS_LOADED) {
            registerOffsetHandler();
            setCameraDistance(24.0f);
        }
        RC_LOADED = ModList.get().isLoaded(RC_MOD_ID);
        if (RC_LOADED) {
            try {
                RC_IS_ACTIVE = Class.forName("com.xtracr.realcamera.RealCameraCore").getMethod("isActive");
            } catch (Throwable t) {
                RC_IS_ACTIVE = null;
            }
        }
        // 探测行（ACCEPTANCE ③ 数值口径：官版 jar=isLoaded=true、has659=false）。
        // has659=false 语义（追加单微调）：stock FPM 旗标在 submit 期求值恒 down
        // →隐头不触发=官方 stock FPM 行为，非缺陷。
        YesSteveModel.LOGGER.info("FirstPersonCompat init: isLoaded={}, has659={}{}", IS_LOADED, HAS_PR659,
                !IS_LOADED ? " (FPM absent: compat off)"
                        : HAS_PR659 ? " (widened FPM: extract-sampled flag drives head-hide)"
                        : " (stock FPM: flag down at submit-time evaluation, head-hide off = official stock behavior)");
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    private static boolean detectPR659() {
        try {
            Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore").getMethod("isRenderingPlayerRaw");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * extract 期采样（RegisterRenderStateModifiersEvent modifier 回调）：
     * FPM 在场 && FPM 旗标（加宽版在 extract 窗口内=true）&& 非 RealCamera
     * 接管 && 非 Iris 阴影 pass。
     */
    public static boolean sample() {
        if (!IS_LOADED || !FirstPersonAPI.isRenderingPlayer()) {
            return false;
        }
        return !realCameraActive() && !OculusCompat.isRenderingShadowPass();
    }

    /** 采样写入（由 FirstPersonCompatSetupHook 的 modifier 调用）。 */
    public static void putSample(EntityRenderState state, boolean flag) {
        FLAG_BY_STATE.put(state, flag);
    }

    /** submit 期状态窗开：261 孪生 ReplacePlayerRenderForgeHook 路由前置入。 */
    public static void beginState(EntityRenderState state) {
        Boolean flag = FLAG_BY_STATE.get(state);
        renderStateFlag = flag != null && flag;
    }

    /** submit 期状态窗关：复位，非路由相位（预览/手部等）读恒 false。 */
    public static void endState() {
        renderStateFlag = false;
    }

    private static boolean realCameraActive() {
        if (!RC_LOADED || RC_IS_ACTIVE == null) {
            return false;
        }
        try {
            return (Boolean) RC_IS_ACTIVE.invoke(null);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void registerOffsetHandler() {
        PlayerOffsetHandler handler = (abstractClientPlayer, f, vec3, vec32) -> new Vec3(vec32.x(), 1.5f - cameraDistance, vec32.z());

        FirstPersonAPI.registerPlayerHandler(handler);
    }

    public static boolean isFirstPersonActive() {
        return IS_LOADED && renderStateFlag;
    }

    public static boolean shouldHideHead() {
        return isFirstPersonActive();
    }

    public static void setCameraDistance(float distance) {
        cameraDistance = distance / 16.0f;
    }

}
