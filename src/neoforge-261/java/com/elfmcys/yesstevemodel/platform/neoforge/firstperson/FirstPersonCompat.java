package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * neoforge 26.1 三线（26.1/26.1.1/26.1.2）FirstPersonModel 真 compat 孪生
 * （fpm-26x-pr659 卡A）。异包孪生（212/2610 树先例）：exclude 按相对路径双杀
 * 同名 RAW 文件，故本类落 platform/neoforge 包，接缝消费方 import 经 stonecutter
 * 行条件交换（PlayerCapability/FirstPersonModHide/ReplacePlayerRenderEvent/
 * ModelPreviewRenderer 四处，AuthModelsCapability.java:3-6 同款缝）。
 *
 * 行为镜像 1.20.1-forge 真实现（client/compat/firstperson/platform/forge/
 * FirstPersonCompat.java，已收官冻结不动）：ModList 探测 modid（forge 端
 * LoadingModList.getModFileById 的 neoforge 对应面，loader-11.0.5/11.0.13
 * net/neoforged/fml/ModList.class isLoaded(String) javap 实证）→
 * FirstPersonAPI.registerPlayerHandler 注册 offset handler →
 * isFirstPersonActive()=FirstPersonAPI.isRenderingPlayer()。
 *
 * 降级语义（主会话 2026-09-16 裁决）：反射特性检测
 * dev.tr7zw.firstperson.FirstPersonModelCore.isRenderingPlayerRaw
 * （PR#659 新增方法，659.diff:34 实证；官方 2.7.2-26.1.2 jar javap 零命中）
 * ——方法存在=用户 FPM 带 #659 宽化旗标 → 走 1.20.1 同款合作路径；
 * 不存在=视同 FPM 缺席（不注册 handler，isFirstPersonActive/shouldHideHead
 * 恒 false，不自研 submit 期隐头）。检测全程异常安全（Throwable 兜底）。
 * isLoaded 语义=FPM 在场（探测行数值口径，ACCEPTANCE ③），合作路径由
 * HAS_PR659 门控；官版 jar 下两条消费面行为与 shim 现状逐点等价：
 * FirstPersonModHide.eval 返回 false（shim 分支不进入亦返回 false）、
 * PlayerCapability.applyHeadTracking setHidden(false)+setCameraDistance 写
 * volatile 无消费者=零观感差。
 *
 * 编译依赖=libs/neoforge-261/firstperson-neoforge-2.7.2-mc26.1.2.jar
 * （compileOnly fileTree，build.forge.gradle.kts:92-94 libs 29 jar 同机制）；
 * api 面 26.x 与 1.20.1 同形（官方 jar javap：registerPlayerHandler(Object)/
 * isRenderingPlayer()/PlayerOffsetHandler.applyOffset(AbstractClientPlayer,
 * float,Vec3,Vec3) net.minecraft.world.phys.Vec3，vanilla-26.1 同名同类实证）。
 * FirstPersonAPI 符号仅被 registerOffsetHandler/isFirstPersonActive 方法体
 * 引用（JVM 惰性解析）：FPM 缺席时这些路径不执行，零 NCDFE（forge 端
 * 同款短路结构，1.20.1 无 FPM 运行实证）。
 */
public class FirstPersonCompat {

    private static final String MOD_ID_OLD = "firstpersonmod";

    private static final String MOD_ID_NEW = "firstperson";

    private static volatile float cameraDistance;

    private static boolean IS_LOADED;

    private static boolean HAS_PR659;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID_NEW) || ModList.get().isLoaded(MOD_ID_OLD);
        HAS_PR659 = IS_LOADED && detectPR659();
        if (HAS_PR659) {
            registerOffsetHandler();
            setCameraDistance(24.0f);
        }
        // 探测行（ACCEPTANCE ③ 数值口径：官版 jar=isLoaded=true、has659=false）
        YesSteveModel.LOGGER.info("FirstPersonCompat init: isLoaded={}, has659={}", IS_LOADED, HAS_PR659);
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

    private static void registerOffsetHandler() {
        PlayerOffsetHandler handler = (abstractClientPlayer, f, vec3, vec32) -> new Vec3(vec32.x(), 1.5f - cameraDistance, vec32.z());

        FirstPersonAPI.registerPlayerHandler(handler);
    }

    public static boolean isFirstPersonActive() {
        return IS_LOADED && HAS_PR659 && FirstPersonAPI.isRenderingPlayer();
    }

    public static boolean shouldHideHead() {
        return isFirstPersonActive();
    }

    public static void setCameraDistance(float distance) {
        cameraDistance = distance / 16.0f;
    }

}
