// neoforge 线 OculusCompat（iris-neoforge-pack-detect）：真检测替换恒 false no-op。
// 背景：Iris 官方支持 NeoForge 1.21.1+（modid=iris，1.8.12+1.21.1-neoforge 实证存在；
// oculus=社区移植线 modid），恒 false 使带光影包时 NativeModelRenderer.renderMesh
// 走 GpuRenderPath 裸 GL 直绘绕过 Iris 管线（研究卡 R4 定性双输）。
// 检测面：FML ModList（net.neoforged.fml.ModList.get().isLoaded，loader 4/6/11 代
// javap 实证同形，与 src/neoforge 平台树 ModContainer 先例同 jar）双 modid 命中其一。
// API 面：反射 net.irisshaders.iris.api.v0.IrisApi（接口 static getInstance() +
// isShaderPackInUse/isRenderingShadowPass，两份 harvest 源码实证；不引编译期依赖），
// Class.forName 守卫 + MethodHandle 缓存，失败安全降 false 并 log 一次。
// init 时点：forge 线由 ForgeClientSetupHooks 调 init（neoforge 侧该树被排除）→
// 本 shim 首次调用惰性初始化（首个调用点=NativeModelRenderer.renderMesh:45 的
// updatePBRState，渲染期 FML 已就绪），语义对照 forge 真实现
// src/main/java/com/elfmcys/yesstevemodel/client/compat/oculus/platform/forge/OculusCompat.java。
package rip.ysm.compat.oculus;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class OculusCompat {

    private static final Logger LOGGER = LogUtils.getLogger();
    // iris=官方 neoforge 线；oculus=社区移植（1.16.5~1.20.1 时代，防手搬 jar 双跑）
    private static final String[] MOD_IDS = {"iris", "oculus"};

    private static final Object LOCK = new Object();

    // volatile 发布位：置 true 是同步块最后一步，读到 true 即可见 loaded/句柄（piggyback）
    private static volatile boolean initialized;
    private static boolean loaded;
    private static MethodHandle shaderPackInUse;
    private static MethodHandle renderingShadowPass;

    private OculusCompat() {
    }

    private static void init() {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            try {
                boolean present = false;
                for (String modId : MOD_IDS) {
                    if (ModList.get().isLoaded(modId)) {
                        present = true;
                        break;
                    }
                }
                if (present) {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object instance = api.getMethod("getInstance").invoke(null);
                    shaderPackInUse = MethodHandles.lookup()
                            .unreflect(api.getMethod("isShaderPackInUse")).bindTo(instance);
                    renderingShadowPass = MethodHandles.lookup()
                            .unreflect(api.getMethod("isRenderingShadowPass")).bindTo(instance);
                    // 预热一次：反射链断裂（IrisApi 实现缺失/签名漂移）在此暴露并降 false
                    boolean packInUse = (boolean) shaderPackInUse.invokeExact();
                    loaded = true;
                    LOGGER.info("[YSM] Iris/Oculus shader compat active (IrisApi v0 reflection), packInUse={}", packInUse);
                }
            } catch (Throwable t) {
                loaded = false;
                LOGGER.warn("[YSM] Iris/Oculus detected but IrisApi reflection failed, shader compat disabled", t);
            }
            initialized = true;
        }
    }

    public static boolean isLoaded() {
        init();
        return loaded;
    }

    public static boolean isPBRActive() {
        init();
        return loaded && isRenderingShadowPassRaw();
    }

    public static void updatePBRState() {
        init();
    }

    public static boolean isShaderPackInUse() {
        init();
        return loaded && invoke(shaderPackInUse);
    }

    public static boolean isRenderingShadowPass() {
        init();
        return loaded && isRenderingShadowPassRaw();
    }

    private static boolean isRenderingShadowPassRaw() {
        return invoke(renderingShadowPass);
    }

    private static boolean invoke(MethodHandle handle) {
        try {
            return (boolean) handle.invokeExact();
        } catch (Throwable t) {
            // 调用期失败安全降 false（勿让着色器检测炸渲染帧）
            return false;
        }
    }
}
