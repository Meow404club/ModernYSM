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
    "com/elfmcys/yesstevemodel/client/model/**",
    "com/elfmcys/yesstevemodel/resource/**",
    "com/elfmcys/yesstevemodel/model/**",
    "com/elfmcys/yesstevemodel/util/**",
    "com/elfmcys/yesstevemodel/config/**",
    "rip/ysm/algorithms/**",
    "rip/ysm/util/**",
    "net/sourceforge/pinyin4j/**",
)
// 共享源 1.12.2 无 mojmap 可言（Mojang 映射 1.14.4 起才有，RFB 只出 MCP/SRG 面），
// 白名单内 111 个 import net.minecraft/com.mojang 的文件在 L1 不编译（名单由
// import 扫描生成，L2 分批做分代 shim 后逐个摘除）——渲染/动画链的 MC 耦合面
// 由 versions/1.12.2-forge/src/main/java 的 1.12.2 原生类承接。
val legacy122ExcludeMcDeps = listOf(
    "com/elfmcys/yesstevemodel/client/model/ModelAssembly.java",
    "com/elfmcys/yesstevemodel/client/model/ModelAssemblyFactory.java",
    "com/elfmcys/yesstevemodel/client/model/PlayerModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/ProjectileModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/VehicleModelBundle.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ArmorSlotProcessor.java",
    "com/elfmcys/yesstevemodel/config/ModSoundEvents.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/AnimatableEntity.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/EntityFrameStateTracker.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerInstance.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/FirstPersonArmAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/PlayerAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/SoundKeyFrameExecutor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/MolangParser.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/ContextBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/QueryBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Clamp.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Cos.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/DieRoll.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/DieRollInteger.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/BoneAnimation.java",
    "com/elfmcys/yesstevemodel/molang/MolangEngine.java",
    "com/elfmcys/yesstevemodel/molang/MolangEngineImpl.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/BoneKeyFrame.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/BoneKeyFrameProcessor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/EasingType.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/TransitionKeyFrame.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/storage/VariableStorage.java",
    "com/elfmcys/yesstevemodel/geckolib3/file/AnimationControllerFile.java",
    "com/elfmcys/yesstevemodel/geckolib3/file/AnimationFile.java",
    "com/elfmcys/yesstevemodel/molang/parser/MolangParser.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/AssignableVariableExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/BinaryExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/BinaryOperationExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/ExecutionScopeExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/Expression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/FloatExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/StatementExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/StructAccessExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/TernaryConditionalExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/UnaryExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/VariableExpression.java",
    "com/elfmcys/yesstevemodel/molang/runtime/Int2FloatOpenHashMapStruct.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/builder/Animation.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/builder/AnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/BoneTransformProvider.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/InstructionKeyFrameExecutor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/AnimationPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/BoneAnimationQueue.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/ConstantPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/KeyFramePoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/TransitionPoint.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/CatmullRomKeyFrame.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/LinearKeyFrame.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/RawBoneKeyFrame.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/keyframe/bone/Vector3v.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/manager/AnimationData.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ControllerVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ForeignVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/ScopedVariableBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/variable/TempVariableRegistry.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/ACos.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/ASin.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/ATan2.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Abs.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Atan.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Ceil.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Exp.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Lerp.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/LerpRotate.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Ln.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Max.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Min.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/MinAngle.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Mod.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Pow.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Random.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/RandomInteger.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Round.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Sqrt.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/query/DebugOut.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/funciton/ContextFunction.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/storage/TempVariableStorage.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/value/FloatValue.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/value/RotationValue.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/ContextVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/variable/LambdaVariable.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/util/TransitionVector3f.java",
    "com/elfmcys/yesstevemodel/model/format/ServerModelInfo.java",
    "com/elfmcys/yesstevemodel/molang/parser/MolangParserImpl.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/CallExpression.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/ExpressionVisitor.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/IdentifierExpression.java",
    "com/elfmcys/yesstevemodel/molang/runtime/AssignableVariable.java",
    "com/elfmcys/yesstevemodel/molang/runtime/ExpressionEvaluator.java",
    "com/elfmcys/yesstevemodel/molang/runtime/ExpressionEvaluatorImpl.java",
    "com/elfmcys/yesstevemodel/molang/runtime/Variable.java",
    "com/elfmcys/yesstevemodel/molang/runtime/binding/StandardBindings.java",
    "com/elfmcys/yesstevemodel/molang/runtime/binding/ValueConversions.java",
    "com/elfmcys/yesstevemodel/client/model/AnimationDataProvider.java",
    "com/elfmcys/yesstevemodel/client/model/MainModelData.java",
    "com/elfmcys/yesstevemodel/client/model/ModelResourceBundle.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ControllerFactory.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ControllerSlotBinder.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ModelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/NamedModelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ParallelProcessor.java",
    "com/elfmcys/yesstevemodel/client/model/processor/ProcessorPipeline.java",
    "com/elfmcys/yesstevemodel/config/ExtraPlayerRenderConfig.java",
    "com/elfmcys/yesstevemodel/config/GeneralConfig.java",
    "com/elfmcys/yesstevemodel/config/LoadingStateConfig.java",
    "com/elfmcys/yesstevemodel/config/ServerConfig.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/builder/AnimationState.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerContext.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/AnimationControllerRuntime.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/CompositeAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/IAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/PredicateBasedController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/ProjectileAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/controller/controllers/VehicleAnimationController.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/event/predicate/AnimationEvent.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/binding/PrimaryBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/MathBinding.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Floor.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/HermitBlend.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Sin.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/builtin/math/Trunc.java",
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
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/context/AnimationContext.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/context/IContext.java",
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
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/value/IValue.java",
    "com/elfmcys/yesstevemodel/geckolib3/core/molang/value/MolangValue.java",
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
    "com/elfmcys/yesstevemodel/geckolib3/core/util/MathUtil.java",
    "com/elfmcys/yesstevemodel/geckolib3/file/ModelExtraResourcesFile.java",
    "com/elfmcys/yesstevemodel/geckolib3/file/ProjectileModelFiles.java",
    "com/elfmcys/yesstevemodel/geckolib3/file/VehicleModelFiles.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoLayerRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoReplacedEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/IGeoRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/NativeModelRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/animated/AnimatedGeoBone.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/animated/AnimatedGeoModel.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/exception/GeckoLibException.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/render/built/GeoModel.java",
    "com/elfmcys/yesstevemodel/geckolib3/resource/GeckoLibCache.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/AnimationUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MathInterpolation.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MatrixBridge.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/MolangUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/RenderUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/VectorUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonAnimationControllerUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonAnimationUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonKeyFrameUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonMolangUtils.java",
    "com/elfmcys/yesstevemodel/geckolib3/util/json/JsonTextureUtils.java",
    "com/elfmcys/yesstevemodel/model/ClientOnlyHostBridge.java",
    "com/elfmcys/yesstevemodel/model/ModelLoadResult.java",
    "com/elfmcys/yesstevemodel/model/ServerModelManager.java",
    "com/elfmcys/yesstevemodel/model/format/ServerModelData.java",
    "com/elfmcys/yesstevemodel/model/format/UUIDComponentData.java",
    "com/elfmcys/yesstevemodel/molang/parser/ast/StringExpression.java",
    "com/elfmcys/yesstevemodel/molang/runtime/ExecutionContext.java",
    "com/elfmcys/yesstevemodel/molang/runtime/Function.java",
    "com/elfmcys/yesstevemodel/resource/YSMBinaryDeserializer.java",
    "com/elfmcys/yesstevemodel/resource/YSMBinarySerializer.java",
    "com/elfmcys/yesstevemodel/resource/YSMClientMapper.java",
    "com/elfmcys/yesstevemodel/resource/YSMFolderDeserializer.java",
    "com/elfmcys/yesstevemodel/resource/models/ModelPackData.java",
    "com/elfmcys/yesstevemodel/resource/models/ModelProperties.java",
    "com/elfmcys/yesstevemodel/util/AnimatableCacheUtil.java",
    "com/elfmcys/yesstevemodel/util/CameraUtil.java",
    "com/elfmcys/yesstevemodel/util/ComponentUtil.java",
    "com/elfmcys/yesstevemodel/util/EquipmentUtil.java",
    "com/elfmcys/yesstevemodel/util/FileTypeUtil.java",
    "com/elfmcys/yesstevemodel/util/InputUtil.java",
    "com/elfmcys/yesstevemodel/util/ItemTagsConstants.java",
    "com/elfmcys/yesstevemodel/util/ParticleEffectUtil.java",
    "com/elfmcys/yesstevemodel/util/ResourceCleanupHelper.java",
    "com/elfmcys/yesstevemodel/util/ThreadLocalItemTagSets.java",
    "com/elfmcys/yesstevemodel/util/YSMMessageFormatter.java",
    "com/elfmcys/yesstevemodel/util/YSMNativeHelper.java",
    "com/elfmcys/yesstevemodel/util/YsmEntity.java",
    "com/elfmcys/yesstevemodel/util/YsmFrame.java",
    "com/elfmcys/yesstevemodel/util/YsmTag.java",
    "com/elfmcys/yesstevemodel/util/YsmText.java",
    "com/elfmcys/yesstevemodel/util/log/ChatLogger.java",
    "com/elfmcys/yesstevemodel/util/log/ILogger.java",
    "rip/ysm/algorithms/YsmZstd.java",
    "rip/ysm/util/RenderCompat.java",
    "rip/ysm/util/Rl.java",
    "rip/ysm/util/UseAction.java",
)
afterEvaluate {
    sourceSets.main {
        java {
            setSrcDirs(listOf(
                rootProject.file("src/main/java"),
                file("src/main/java"),
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
