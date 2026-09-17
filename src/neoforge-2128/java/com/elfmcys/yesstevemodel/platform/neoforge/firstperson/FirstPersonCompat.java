package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * neoforge 21.2~21.8 七线（21.2/21.3/21.4/21.5/21.6/21.7/21.8）FirstPersonModel
 * 真 compat 孪生（fpm-card-b-2128 卡B，=26.1 卡A 打法降配）。异包孪生
 * （212/261 树先例：exclude 按相对路径双杀同名 RAW 文件，孪生必须异包），
 * 接缝消费方四文件 import 经 stonecutter 行条件交换（PlayerCapability/
 * FirstPersonModHide/ReplacePlayerRenderEvent/ModelPreviewRenderer，261 卡先例）。
 *
 * 【与 26.1 孪生的差异：无 extract 快照链/无 SetupHook】三族 FPM neoforge jar
 * javap 实证（Modrinth 实拉，逐线 vendor 见 libs/neoforge-21xx）：2.4.8（21.2 线）、
 * 2.5.0（21.6/21.7 线）、2.7.2（21.3/21.4/21.5/21.8 线）的 WorldRendererMixin
 * 同款开窗=setRenderingPlayer(true)→手动 renderEntity（走完整 vanilla
 * PlayerRenderer.render 链）→setRenderingPlayer(false)，旗标盖住整个同步
 * renderEntities 期=1.20.1 老语义天然成立（21.9+ FPM 2.6.0 才改 extract 窗口）。
 * 消费面（含动画求值，21.2~21.8 我方求值在 render 期）直读
 * {@code FirstPersonAPI.isRenderingPlayer()} 即可，无快照/无 RenderPlayerEvent
 * 消费窗——故 2111/261 的 ReplacePlayerRenderForgeHook 替换与本线无关，既有
 * 1213/1215/1218/212 树 hook 原样保留。
 *
 * 【惰性 init】neoforge 线无 ForgeClientSetupHooks（1.20.1-forge 的 init 挂点），
 * 卡B 不引入 SetupHook → init 延迟到首个消费面调用（渲染线程单线程语义，
 * 双检锁只 init 一次）。时差代价：FPM 在 renderEntities 内先 updatePositionOffset
 * 消费 handler 列表后 renderEntity（2.7.2 WorldRendererMixin 字节码序），首个
 * FPM 渲染帧 handler 可能未注册（不偏移一帧），次帧起正常——
 * ponytail: 一帧时差，引入 FMLClientSetupEvent SetupHook 才能消除，21.2~21.5
 * loader 的 @EventBusSubscriber 还有 bus 成员分代（21.6 才删），不值。
 *
 * 【offset/排除语义=1.20.1 金标准】offset=Vec3(x, 1.5−cameraDistance, z)
 * （cameraDistance 由共享 PlayerCapability.applyHeadTracking 写 ViewLocator
 * pivot×scale，兜底 24/16）；RealCamera/Iris 阴影排除=消费面既有条件
 * （ReplacePlayerRenderEvent 的 RealCameraCompat.isActive() 分支+FPM 旗标
 * 在阴影 pass 天然 false），无需本类自建检测（26.1 的 extract 采样才需要）。
 *
 * 编译依赖=libs/neoforge-21xx 官方 jar（compileOnly fileTree 不进产物，
 * 26.1 卡/1.20.1 libs 先例）。FirstPersonAPI 符号仅被 registerOffsetHandler/
 * isFirstPersonActive 方法体引用（JVM 惰性解析）：FPM 缺席时路径不执行，
 * 零 NCDFE（261 孪生同款短路结构）。
 */
public class FirstPersonCompat {

    private static final String MOD_ID_OLD = "firstpersonmod";

    private static final String MOD_ID_NEW = "firstperson";

    private static volatile float cameraDistance;

    private static volatile boolean inited;

    private static boolean IS_LOADED;

    /** 惰性 init（首个消费面调用触发一次；渲染线程语义+双检锁兜底）。 */
    private static void ensureInit() {
        if (inited) {
            return;
        }
        synchronized (FirstPersonCompat.class) {
            if (inited) {
                return;
            }
            IS_LOADED = ModList.get().isLoaded(MOD_ID_NEW) || ModList.get().isLoaded(MOD_ID_OLD);
            if (IS_LOADED) {
                registerOffsetHandler();
                setCameraDistance(24.0f);
            }
            // 探测行（冒烟验收数值口径：带 FPM=isLoaded=true / 不带=false）
            YesSteveModel.LOGGER.info("FirstPersonCompat init(lazy): isLoaded={}{}",
                    IS_LOADED, IS_LOADED ? " (old-semantics direct flag: FPM window covers render)"
                            : " (FPM absent: compat off)");
            inited = true;
        }
    }

    private static void registerOffsetHandler() {
        PlayerOffsetHandler handler = (abstractClientPlayer, f, vec3, vec32) -> new Vec3(vec32.x(), 1.5f - cameraDistance, vec32.z());

        FirstPersonAPI.registerPlayerHandler(handler);
    }

    public static boolean isLoaded() {
        ensureInit();
        return IS_LOADED;
    }

    /** 21.2~21.8 老语义：FPM 旗标盖住 render 期，直读即消费时点真值。 */
    public static boolean isFirstPersonActive() {
        ensureInit();
        return IS_LOADED && FirstPersonAPI.isRenderingPlayer();
    }

    public static boolean shouldHideHead() {
        return isFirstPersonActive();
    }

    public static void setCameraDistance(float distance) {
        cameraDistance = distance / 16.0f;
    }

}
