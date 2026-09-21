// ===== legacy 1.12.2 线构建脚本（legacy-1222-l0-poc，RetroFuturaGradle 路线）=====
// 形态先例（逐项对照，禁凭记忆）：tmp/harvest/celeritas-mva/forge122/build.gradle.kts
//   :12   RFB "com.gtnewhorizons.retrofuturagradle" 1.4.8（插件 marker 仅 GTNH nexus，
//         gradlePluginPortal 404 实证 2026-09-20；latest=2.0.4 未验证 Gradle 9.2.1 兼容，POC 锁 1.4.8）
//   :21-26 Java21 toolchain（RFB 自管 decompile/reobf 工具链）
//   :37-43 minecraft{} mcVersion=1.12.2 + mainLwjglVersion=3 + lwjgl3Version + JAVA_17_ARGS
//   :87   lwjgl3ify=org.taumc:lwjgl3ify:d6af8e7（taumc maven，latest=d6af8e7 实证）
//   :114  zone.rong:mixinbooter:10.5（cleanroommc maven）
// 坐标实证（2026-09-20 联网）：forge 1.12.2 maven metadata 线内另有 2860~2864 后补构建，
// 任务卡冻结 tile=14.23.5.2859（社区公认末版稳定线），本脚本照卡不取 2864。
// POC 范围：stub jar 构建绿 + runClient 到主菜单；不碰共享源 src/main（L1 分代条件块另卡）。
// 运行一律 --no-daemon 串行（根 gradle.properties 已 parallel=false；本脚本配置期读
// userdev/patched jar 时 RFB 自身与 configuration-cache 存在持活风险，全仓既有约定
// --no-configuration-cache 跑非 MDG 线）。

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.8"
    `java-library`
}

version = "${property("mod_version")}-${property("deps.minecraft")}-forge"
val modId = rootProject.property("archives_name") as String
base.archivesName = "$modId-forge-mc1.12.2"
group = property("maven_group") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    // 1.12.2 = Java 8 目标字节码；RFB mcVersion 面按 8 处理，javac --release 8 出 major 52
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

minecraft {
    javaCompatibilityVersion = 8
    mainLwjglVersion = 3
    mcVersion.set("1.12.2")
    lwjgl3Version = "3.3.3"
    // 先例 :42 走 embeddium 自家 RFBArgs.JAVA_17_ARGS（非 RFB API，实证 1.4.8 jar 零此符号）；
    // POC 内联等价面（--add-opens + 禁 Java9+ flag 拦截），L1 若扩参再抽公共
    listOf(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "-Dfml.queryExistingLoggers=true", "-Dlegacy.debugClassLoading=true"
    ).forEach { extraRunJvmArguments.add(it) }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.cleanroommc.com") }
        filter { includeGroup("zone.rong") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.taumc.org/releases") }
        filter { includeGroupAndSubgroups("org.taumc") }
    }
}

val lwjgl3ifyVersion = "d6af8e7"

// lwjgl3ify forgePatches：运行面（先例 forge122:118-126 同款，implementation 仅 :dev）
val forgePatchDeps by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation("org.taumc:lwjgl3ify:${lwjgl3ifyVersion}:dev") { isTransitive = false }
    forgePatchDeps("org.taumc:lwjgl3ify:${lwjgl3ifyVersion}:forgePatches") { isTransitive = false }
    // mixinbooter（先例 :114；stub 阶段只进 runtime 面，L1 写 mixin 类时再挂配置）
    implementation("zone.rong:mixinbooter:10.5")
    // 共享源渲染/动画链以 JOML 为工作类型（geckolib3 全线 org.joml），1.12.2 无内置 →
    // Celeritas forge122 先例同款显式依赖（build.gradle.kts:118 org.joml:joml:1.10.5）
    implementation("org.joml:joml:1.10.5")
}

// 共享源分代面首轮编译错误 >100，javac 默认上限截断——1.16.5 线同款放开（build.unimined:51）
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

// stub 阶段 jar 即产物（RFB reobfJar 对纯 SRG 命名编译输出做 ForgeSRG→notch 映射；
// Celeritas 生产先例关闭它走自研 remap——本卡无 shadow/mixin，保留默认 reobfJar 即可）
tasks.named<Jar>("jar") {
    manifest {
        attributes["Lwjgl3ify-Aware"] = "true"
        attributes["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        attributes["MixinConfigs"] = "$modId.mixins.json"
        attributes["FMLCorePluginContainsFMLMod"] = "true"
        attributes["ForceLoadAsMod"] = "true"
    }
}

// ===== 源集挂载（legacy-1222-l1-render 改写）=====
// L0 曾用 afterEvaluate setSrcDirs 换根把共享生成树挤出（当时共享源 1.12.2 全量编译
// 100 错）。L1 实测：共享源 1.12.2 分代面首轮 546 文件 7409 错（compat 包 tacz/tlm/
// slashblade 在 1.12.2 无对应平台 + mojmap 包名全线不在 RFB MCP 面）——全量分代不合
// L1 量级，改 include 白名单只挂渲染复用链（geckolib3 geo/core/util + bake 链），
// 其余共享面待 L2 分批挂。外部 compat 触点由 versions/1.12.2-forge no-op shim 提供
//（build.unimined:102 先例同款）。
val legacy122Include = listOf(
    "com/elfmcys/yesstevemodel/geckolib3/**",
    "com/elfmcys/yesstevemodel/molang/**",
    "com/elfmcys/yesstevemodel/client/ClientModelInfo.java",
    "com/elfmcys/yesstevemodel/audio/AudioTrackData.java",
    "com/elfmcys/yesstevemodel/audio/AudioCodec.java",
    "com/elfmcys/yesstevemodel/client/model/**",
    "com/elfmcys/yesstevemodel/client/texture/**",
    "com/elfmcys/yesstevemodel/client/gui/custom/**",
    
    "rip/ysm/imagestream/**",
    "org/gagravarr/**",
    
    "com/elfmcys/yesstevemodel/resource/**",
    "com/elfmcys/yesstevemodel/model/**",
    "com/elfmcys/yesstevemodel/util/**",
    "com/elfmcys/yesstevemodel/config/**",
    "rip/ysm/algorithms/**",
    "rip/ysm/security/**",
    "rip/ysm/zstd/**",
    "rip/ysm/util/**",
    "rip/ysm/legacy122/**",
    "rip/ysm/OpenYSMStub.java",
    "rip/ysm/LegacyConfig.java",
    "net/sourceforge/pinyin4j/**",
)
// 共享源 1.12.2 无 mojmap 可言（Mojang 映射 1.14.4 起才有，RFB 只出 MCP/SRG 面），
// 白名单内 111 个 import net.minecraft/com.mojang 的文件在 L1 不编译（名单由
// import 扫描生成，L2 分批做分代 shim 后逐个摘除）——渲染/动画链的 MC 耦合面
// 由 versions/1.12.2-forge/src/main/java 的 1.12.2 原生类承接。
val legacy122ExcludeMcDeps = listOf(
    "com/elfmcys/yesstevemodel/YesSteveModel.java",
    "com/elfmcys/yesstevemodel/client/model/ModelAssembly.java",
    "com/elfmcys/yesstevemodel/client/model/ModelAssemblyFactory.java",
    "com/elfmcys/yesstevemodel/client/model/PlayerModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/ProjectileModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/VehicleModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ArmorSlotProcessor.java",
    "com/elfmcys/yesstevemodel/config/ModSoundEvents.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/AnimatableEntity.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/EntityFrameStateTracker.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/QueryBinding.java",
    "com/elfmcys/yesstevemodel/client/animation/molang/YSMBinding.java",
    "com/elfmcys/yesstevemodel/client/animation/molang/CtrlBinding.java",
    "com/elfmcys/yesstevemodel/client/animation/molang/TLMBinding.java",
    "com/elfmcys/yesstevemodel/client/animation/molang/ArgsVariable.java",
    "com/elfmcys/yesstevemodel/config/ExtraPlayerRenderConfig.java",
    "com/elfmcys/yesstevemodel/config/GeneralConfig.java",
    "com/elfmcys/yesstevemodel/config/LoadingStateConfig.java",
    "com/elfmcys/yesstevemodel/config/ServerConfig.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/BiomeHasAllTags.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/BiomeHasAnyTag.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/EquipmentItemAnyTag.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/EquippedItemAllTags.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/IsItemNameAny.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/MaxDurability.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/Position.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/PositionDelta.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/RelativeBlockHasAllTags.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/RelativeBlockHasAnyTag.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/RemainingDurability.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/RotationToCamera.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Random.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ControllerVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ForeignVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ScopedVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/TempVariableRegistry.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonKeyFrameUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/RandomInteger.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/DieRoll.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/DieRollInteger.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/blocks/AbstractBlockFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/blocks/BlockFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/AbstractArrowEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/AbstractClientPlayerFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/AbstractProjectileFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/ArrowEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/EntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/LivingEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/LocalPlayerEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/MobEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/entity/TamableEntityFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/item/ItemFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/item/ItemStackFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/block/BlockBehaviorVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/block/BlockStateVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/block/BlockVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/AbstractArrowEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/ArrowEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/ClientPlayerEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/EntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/FishingHookEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/LivingEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/LocalPlayerEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/MobEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/PlayerEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/ProjectileEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/TamableEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/entity/ThrowableProjectileEntityVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/item/ItemStackVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/item/ItemVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/processor/AnimationProcessor.java",
    // ===== 批C 永久归档（legacy122-l3-4a-exclude-batch-a）：以下 5 项不摘除 =====
    // GeoEntityRenderer 族走现代 GPU 渲染管线（RenderSystem/PoseStack/BufferBuilder
    // 现代 API 面），1.12.2 固定管线无对应物——由 versions/1.12.2-forge 原生渲染
    // 翻译层（LegacyModelRenderer/LegacyRenderHook）降级替代，不回接编译面。
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoLayerRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoReplacedEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/IGeoRenderer.java",
    // NativeModelRenderer 依赖 com.mojang.blaze3d.platform.NativeImage/TextureUtil
    // native 面（1.12.2 无 blaze3d），不做 shim——1.12.2 原生 ModelRenderer 承接。
    "com/elfmcys/yesstevemodel/geckolib3/geo/NativeModelRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/exception/GeckoLibException.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/AnimationUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MathInterpolation.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MolangUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/RenderUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/VectorUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonAnimationUtils.java",
    "com/elfmcys/yesstevemodel/model/ClientOnlyHostBridge.java",
    "com/elfmcys/yesstevemodel/model/ModelLoadResult.java",
    "com/elfmcys/yesstevemodel/model/format/ServerModelData.java",
    "com/elfmcys/yesstevemodel/model/format/UUIDComponentData.java",
    "com/elfmcys/yesstevemodel/util/AnimatableCacheUtil.java",
    "com/elfmcys/yesstevemodel/util/CameraUtil.java",
    "com/elfmcys/yesstevemodel/util/ComponentUtil.java",
    "com/elfmcys/yesstevemodel/util/EquipmentUtil.java",
    "com/elfmcys/yesstevemodel/util/InputUtil.java",
    "com/elfmcys/yesstevemodel/util/ItemTagsConstants.java",
    "com/elfmcys/yesstevemodel/util/ParticleEffectUtil.java",
    "com/elfmcys/yesstevemodel/util/ThreadLocalItemTagSets.java",
    "com/elfmcys/yesstevemodel/util/YSMMessageFormatter.java",
    "com/elfmcys/yesstevemodel/util/YSMNativeHelper.java",
    "com/elfmcys/yesstevemodel/util/YsmEntity.java",
    "com/elfmcys/yesstevemodel/util/YsmFrame.java",
    "com/elfmcys/yesstevemodel/util/YsmTag.java",
    "com/elfmcys/yesstevemodel/util/YsmText.java",
    "com/elfmcys/yesstevemodel/util/log/ChatLogger.java",
    "com/elfmcys/yesstevemodel/util/log/ILogger.java",
    "rip/ysm/util/RenderCompat.java",
    "rip/ysm/util/Rl.java",
    "rip/ysm/util/UseAction.java",
    "com/elfmcys/yesstevemodel/client/model/AnimationDataProvider.java",
    "com/elfmcys/yesstevemodel/client/model/ModelResourceBundle.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ControllerFactory.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ControllerSlotBinder.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ModelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/NamedModelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ParallelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ProcessorPipeline.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/DebugOut.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/ContextFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/ContextVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/LambdaVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonAnimationControllerUtils.java",
    "com/elfmcys/yesstevemodel/util/ResourceCleanupHelper.java",
    "com/elfmcys/yesstevemodel/client/texture/OuterFileTexture.java",
    "com/elfmcys/yesstevemodel/util/log/ChatLogger.java",
    "com/elfmcys/yesstevemodel/client/renderer/AnimationDebugOverlay.java",
    "com/elfmcys/yesstevemodel/client/texture/ITextureMap.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MatrixBridge.java",
    "com/elfmcys/yesstevemodel/model/ServerModelManager.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/context/AnimationContext.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/context/IContext.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerInstance.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerRuntime.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/CompositeAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/FirstPersonArmAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/PlayerAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/ProjectileAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/VehicleAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/PredicateBasedController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/InstructionKeyFrameExecutor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/predicate/AnimationEvent.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/SoundKeyFrameExecutor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/AnimationPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/BoneAnimationQueue.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/ConstantPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/KeyFramePoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/TransitionPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/manager/AnimationData.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/IAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerContext.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/BoneTransformProvider.java",
)
// ===== twin 源集（legacy-1222-l1-render 分代孪生）=====
// 共享 YesSteveModel/ChatLogger/AnimationDebugOverlay/OuterFileTexture/ShadersTextureType
// 绑定现代 MC 面（Component/AbstractTexture/net.minecraft.resources），1.12.2 无对应。
// 孪生类（1.12.2 原生面，同包同名）放 versions/1.12.2-forge/src/twin/java 独立源集：
// main 源集按相对路径排除共享同名文件时不会误杀 twin（exclude 只作用于 main 自己的
// srcDirs），twin classes 挂 main 编译/运行类路径——包结构对消费点透明。
sourceSets {
    create("twin") {
        java {
            srcDir(file("src/twin/java"))
        }
    }
}
tasks.named<JavaCompile>("compileTwinJava") {
    sourceCompatibility = "8"
    targetCompatibility = "8"
}
// twin 需要 RFB patched MC 编译面（AbstractTexture/Minecraft/log4j 与 main 同源）：
// main.compileClasspath 摘除 twin.output（FileCollection 减法）断
// compileTwinJava↔twinClasses↔main 环，其余面（RFB patched MC+依赖）全继承
afterEvaluate {
    tasks.named<JavaCompile>("compileTwinJava") {
        classpath = sourceSets.main.get().compileClasspath - sourceSets.getByName("twin").output
    }
}
afterEvaluate {
    sourceSets.main {
        java {
            srcDir(sourceSets.getByName("twin").output)
        }
    }
}
// main 编译/运行不挂 twin.output（会经 compileClasspath 传染 twinClasses 依赖，
// 与 twin 继承 main 编译面成环）——twin classes 由 jar 打包 + 各消费任务显式并
tasks.named<JavaCompile>("compileJava") {
    // 显式任务依赖（output FileCollection 理论自带 dependsOn，RFB 环境实测不生效）
    dependsOn(tasks.named("compileTwinJava"))
    classpath += sourceSets.getByName("twin").output
}
// main 源路径含 build/generated/stonecutter/main/java（313bdf4 起）——生成树由
// stonecutterGenerate 产出，但 RFB 线该目录为纯路径 srcDir，FileCollection 不挂
// stonecutterGenerate 任务依赖，clean 后/新 worktree 首跑 compileJava 会在空生成树上
// 编译 → 58 个 "geckolib3 包不存在"（dev 顶 b83a111 门禁红实证 2026-09-21）。
// build.forge.gradle.kts:286 createMinecraftArtifacts dependsOn stonecutterGenerate 先例
// 同款，补显式任务依赖（幂等，已生成时 up-to-date 秒过）。
tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("stonecutterGenerate"))
}
// twin 源集同样走 stonecutter 剥离（//? 条件块在 twin/classes 面同样不剥离必炸）
tasks.named<JavaCompile>("compileTwinJava") {
    dependsOn(tasks.named("stonecutterGenerateTwin"))
}
tasks.named<Jar>("jar") {
    from(sourceSets.getByName("twin").output)
}
// twin 需要 RFB patched MC 编译面（AbstractTexture/Minecraft/log4j 与 main 同源）：
// twinCompileClasspath 继承 implementation/compileOnly 外部配置（不含任何 sourceSet
// output，无环）；joml/fastutil 等已随 implementation 面
afterEvaluate {
    configurations.getByName("twinCompileClasspath").extendsFrom(
        configurations.getByName("implementation"),
        configurations.getByName("compileOnly"),
    )
}

afterEvaluate {
    sourceSets.main {
        java {
            // 共享面必须走 stonecutter 展开树（//? 条件剥离在生成步）；raw src/main/java
            // 的条件块两侧代码都在，直接编译必炸（pass26 实证 GeoModel 双 initSIMD）
            setSrcDirs(listOf(
                file("build/generated/stonecutter/main/java"),
                file("src/main/java"),
                // avif/webp/jpeg 解码 vendor（build.forge.gradle.kts:252 先例同款，8 文件
                // 纯净零 MC import 实证）——.ysm 解析链 YSMFolderDeserializer/YSMClientMapper 依赖
                rootProject.file("versions/1.16.5-forge/src/imagestream"),
            ))
            include(legacy122Include)
            // 白名单内仍面向 >=1.17 的重文件（L2 分批接入，先按报错面点名排除）
            exclude("com/elfmcys/yesstevemodel/geckolib3/extended/**")
            exclude(legacy122ExcludeMcDeps)
        }
        resources.setSrcDirs(listOf(
            file("src/main/resources"),
            rootProject.file("src/main/resources"),
        ))
    }
}
// 双资源目录（版本独有面列前=优先）：pack.mcmeta/mcmod.info 等重叠条目以版本面为准
tasks.processResources.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// 运行面：lwjgl3ify 现代化 runtime（用户裁决 2026-09-20）。RFB 默认 runClient 在 Java21 下
// launchwrapper 直挂 main（ClassCastException AppClassLoader→URLClassLoader 实测
// 2026-09-20）——先例 runRFBClient（forge122:151-172）同款自定义 RunMinecraftTask：
// forgePatches 显式排 classpath 前列（内含打补丁的 launchwrapper，绕开 JEP 违规 cast），
// bouncer 走 RetroFuturaBootstrap。
tasks.register<com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask>(
    "runRFBClient", com.gtnewhorizons.retrofuturagradle.util.Distribution.CLIENT
).configure {
    val mcTasks = project.extensions.getByType<com.gtnewhorizons.retrofuturagradle.minecraft.MinecraftTasks>()
    classpath(forgePatchDeps)
    classpath(mcpTasks.taskPackageMcLauncher)
    classpath(mcpTasks.taskPackagePatchedMc)
    classpath(sourceSets.main.get().compileClasspath)
    classpath(tasks.named("jar"))
    setup(project)
    dependsOn(mcTasks.taskDownloadVanillaAssets, mcpTasks.taskPackagePatchedMc, "jar")
    mainClass.set("GradleStart")
    username.set(minecraft.username)
    userUUID.set(minecraft.userUUID)
    // RetroFuturaBootstrap Main.main:140 强制要求系统类加载器替换（实测报错原文点名）
    extraJvmArgs.add("-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader")
    // L1 验收动画打点原文行（LegacyModelTranslator/LegacyAnimationDriver printf）
    extraJvmArgs.add("-Dysm.legacy122.debug=true")
    systemProperty("gradlestart.bouncerClient", "com.gtnewhorizons.retrofuturabootstrap.Main")
    systemProperty("fml.coreMods.load", "zone.rong.mixinbooter.MixinBooterPlugin")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

// RFB 默认 runClient/runServer 不可用（launchwrapper URLClassLoader cast，实测）；
// server 同理需 forgePatches 前置——POC 验收走 runRFBClient。
tasks.named("runClient") { enabled = false }

tasks.processResources.configure {
    filesMatching("mcmod.info") {
        expand(mapOf("version" to version))
    }
}
