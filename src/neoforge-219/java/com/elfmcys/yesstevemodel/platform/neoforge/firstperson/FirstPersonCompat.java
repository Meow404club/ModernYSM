package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.WeakHashMap;

/**
 * neoforge 21.9/21.10/21.11 三线 FirstPersonModel 真 compat 孪生
 * （fpm-selfdrive-219-2111 卡）。异包孪生（212/261/2610 树先例）：
 * exclude 按相对路径双杀同名 RAW 文件，故本类落 platform/neoforge 包；
 * 接缝消费方四文件 import 经 stonecutter 行条件交换（PlayerCapability/
 * FirstPersonModHide/ReplacePlayerRenderEvent/ModelPreviewRenderer，261 卡先例）。
 *
 * 【与 26.1 孪生的分歧=采样判据】26.1 走 PR#659 加宽旗标（isRenderingPlayerRaw），
 * 本三线为自力方案（不等上游，PR#659 open 未合实证）：FPM 2.6.0（21.9/21.10，
 * Modrinth 双 game_versions 同 jar）与 2.7.2（21.11）的 WorldRendererMixin 均注
 * extractVisibleEntities（extract 期，tmp/harvest/compat-fpm-src WorldRendererMixin
 * .java:79-143：HEAD 开窗 setRenderingPlayer(true)，:129 extractEntity（FPM 经
 * AvatarRenderer 自 extract 相机实体 state），:140-141 关窗）——旗标窗只盖
 * extract 期，submit 期直查恒 false。对位：
 *   采样=FirstPersonCompatSetupHook 注册的 RegisterRenderStateModifiersEvent
 *        第二 extractor modifier（vanilla extractRenderState 尾回调）：
 *        实体==相机实体（Minecraft.player，第一人称时即 FPM extractEntity 的
 *        同一实体）&& FPM 在场 && FirstPersonAPI.isRenderingPlayer()（extract
 *        窗口内=true）→ putSample(state, true)。vanilla 自 extract 的玩家
 *        state 在开窗前/后采样=天然 false；YSM 不经 FPM mixin、自家 extractor
 *        标自家 state，天然避开第三方捕获污染（研究卡论证，不复刻 PR#659 的
 *        cameraEntityExtract/双旗标拆分）。
 *   消费=本包孪生 ReplacePlayerRenderForgeHook 在 RenderPlayerEvent.Pre 路由前
 *        beginState(state) 置当前快照，setHidden 消费点（共享 PlayerCapability:140-148）
 *        与 molang 门（FirstPersonModHide）经 isFirstPersonActive()/shouldHideHead()
 *        读快照，路由后 endState() 复位。submit 期旗标窗恒 false（spec 明确不复刻
 *        2128 老 render 期直查语义——2.6.0/2.7.2 javap 实证 mixin 目标
 *        extractVisibleEntities，非 21.8- 的 renderEntities）。
 *
 * 【降级/缺席】FPM 缺席：isLoaded=false → sample() 恒 false、isFirstPersonActive()
 * 恒 false（isLoaded 短路在最前），行为与 2111 shim（恒 false）逐位一致——未装 FPM
 * 零变化。26.3 不在本卡范围（FPM 无 26.3 build，shim 恒 false 已是正确降级）。
 *
 * 【offset handler】=2128/261 孪生同款 Vec3(x,1.5−cameraDistance,z)；init 经
 * SetupHook 的 FMLClientSetupEvent（neoforge 线无 1.20.1 ForgeClientSetupHooks），
 * 261 卡 SetupHook 先例（loader 11+ @EventBusSubscriber 无 bus 属性）。
 *
 * 编译依赖=libs/neoforge-219/（compileOnly fileTree 不进产物，261/2128 卡先例）：
 * firstperson-neoforge-2.6.0-mc1.21.9.jar（Modrinth W版 WedulMVH，实拉）+
 * firstperson-neoforge-2.7.2-mc1.21.11.jar（TKoHJWZZ）。FirstPersonAPI 符号仅被
 * sample/registerOffsetHandler 方法体引用（JVM 惰性解析）：FPM 缺席时路径不执行，
 * 零 NCDFE（261/2128 孪生同款短路结构）。
 */
public class FirstPersonCompat {

    private static final String MOD_ID_OLD = "firstpersonmod";

    private static final String MOD_ID_NEW = "firstperson";

    private static volatile float cameraDistance;

    private static boolean IS_LOADED;

    /** 旗标采样槽（extract 期写、submit 期读；key=vanilla extract 的 state 实例）。 */
    private static final WeakHashMap<EntityRenderState, Boolean> FLAG_BY_STATE = new WeakHashMap<>();

    /** 当前 submit 窗口内的快照值（beginState/endState 维护，渲染线程单线程语义）。 */
    private static boolean renderStateFlag;

    /** 采样命中计数（acceptance 数值口径：extract 期自家 extractor 标旗次数）。 */
    private static volatile long sampleHits;

    /** 消费命中计数（acceptance 数值口径：submit 期判据消费到 true 的次数）。 */
    private static volatile long consumeHits;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID_NEW) || ModList.get().isLoaded(MOD_ID_OLD);
        if (IS_LOADED) {
            registerOffsetHandler();
            setCameraDistance(24.0f);
        }
        // 探测行（acceptance 数值口径：带 FPM=isLoaded=true / 不带=false）
        YesSteveModel.LOGGER.info("FirstPersonCompat init: isLoaded={}{}",
                IS_LOADED, IS_LOADED
                        ? " (extract-sampled flag drives head-hide, selfdrive 219/2111)"
                        : " (FPM absent: compat off)");
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    private static void registerOffsetHandler() {
        PlayerOffsetHandler handler = (abstractClientPlayer, f, vec3, vec32) -> new Vec3(vec32.x(), 1.5f - cameraDistance, vec32.z());

        FirstPersonAPI.registerPlayerHandler(handler);
    }

    /**
     * extract 期采样（FirstPersonCompatSetupHook 的第二 extractor modifier 回调）：
     * FPM 在场 && FPM 旗标（extract 窗口内=true）&& 实体是相机实体（本地玩家）。
     */
    public static boolean sample(Object entity) {
        if (!IS_LOADED || !FirstPersonAPI.isRenderingPlayer()) {
            return false;
        }
        if (entity != net.minecraft.client.Minecraft.getInstance().player) {
            return false;
        }
        sampleHits++;
        return true;
    }

    /** 采样写入（由 FirstPersonCompatSetupHook 的 modifier 调用）。 */
    public static void putSample(EntityRenderState state, boolean flag) {
        FLAG_BY_STATE.put(state, flag);
    }

    /** submit 期状态窗开：孪生 ReplacePlayerRenderForgeHook 路由前置入。 */
    public static void beginState(EntityRenderState state) {
        Boolean flag = FLAG_BY_STATE.get(state);
        renderStateFlag = flag != null && flag;
        if (renderStateFlag) {
            consumeHits++;
        }
    }

    /** submit 期状态窗关：复位，非路由相位（预览/手部等）读恒 false。 */
    public static void endState() {
        renderStateFlag = false;
    }

    /** acceptance 数值口径：extract 期自家 extractor 对相机实体标旗命中计数。 */
    public static long sampleHits() {
        return sampleHits;
    }

    /** acceptance 数值口径：submit 期判据消费到 true 的计数。 */
    public static long consumeHits() {
        return consumeHits;
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
