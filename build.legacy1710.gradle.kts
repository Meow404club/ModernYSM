// ===== legacy 1.7.10 线构建脚本（legacy-1710-l0-poc，unimined 路线）=====
// 形态先例（逐项对照，禁凭记忆）：tmp/harvest/celeritas-mva/forge1710/build.gradle
//   :3    unimined 插件（本仓复用 1.16.5 线已实证的 1.4.1 稳定版，退路 1.3.16-SNAPSHOT
//         见 settings pluginManagement 注释；Celeritas 用 1.3.16-SNAPSHOT）
//   :73-76 mappings searge() + mcp("stable","12-1.7.10")——1.7.10 无 mojmap，走
//         searge 发布 + MCP stable_12 编译（与 1.12.2 RFB 线不同路）
//   :78-82 minecraftForge loader 10.13.4.1614-1.7.10 + mixinConfig
//   :84-102 runs.config("client") Java21 + RetroFuturaBootstrap Main + add-opens 全家桶
//   :125  unimixins 0.1.19:dev（GTNH nexus；Sponge 桥，1.7.10 代 mixin 唯一实证路线）
//   :113-114 lwjgl3ify（现代化 runtime，用户裁决 2026-09-20）+ forgePatches
//   :155-161 remapJar mixinRemap enableBaseMixin/enableMixinExtra/disableRefmap
// unimined 官方 testing 1.7.10-Forge 样例同构（tmp/harvest/unimined-testing-1710/build.gradle:
// searge+mcp stable 12-1.7.10 / minecraftForge loader / mixinConfig）。
// POC 范围：stub jar 构建绿 + 启动证据；不碰共享源 src/main（渲染翻译层 L1+ 另卡）。
// 运行纪律：gradle 串行 --no-daemon；remapJar 与 configuration cache 不兼容
//（build.unimined.gradle.kts 头注实证），全链 --no-configuration-cache。

plugins {
    id("xyz.wagyourtail.unimined") version "1.4.1"
    `java-library`
}

version = property("mod_version") as String
base.archivesName = "${property("archives_name")}-forge-mc1.7.10"
group = property("maven_group") as String

// 1.7.10 = Java 8 目标字节码；daemon/toolchain 跑 JDK21（unimined 官方 testing 同款）
// L2a 实测：options.release=8 屏蔽 sun.misc.Unsafe（共享链 util/UnsafeUtil+rip/ysm/zstd
// 依赖面，122 线 build.legacy122.gradle.kts:30-33 同走 source/target 1.8 无 release）——
// 与 122 线同机制：source/targetCompatibility 1.8（major 52 同值）
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// client/server run 共享 JVM 参数（先例 forge1710:87-99 逐项；须在 unimined.minecraft
// 块之前声明——runs.config 的 lambda 配置期立即执行，且其 jvmArgs getter 返回不可变列表，
// setJvmArgs 整体赋值而非 addAll）
val clientJvmArgs = listOf(
    "-Dfile.encoding=UTF-8",
    // 取证打点开关（legacy-1710-l1-ingame-visual）：dev run 面开 translator frame
    // quadsDrawn 打点（1.12.2 线 c1b96fd 同款口径）；生产 run 面不带此行零行为差
    "-Dysm.legacy1710.debug=true",
    "-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader",
    "-Djava.security.manager=allow", "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED", "--add-opens",
    "java.base/java.net=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens",
    "java.base/java.io=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens",
    "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.text=ALL-UNNAMED", "--add-opens",
    "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/jdk.internal.reflect=ALL-UNNAMED", "--add-opens",
    "java.base/sun.nio.ch=ALL-UNNAMED", "--add-opens", "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming",
    "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED", "--add-opens", "java.desktop/sun.awt.image=ALL-UNNAMED",
    "--add-opens", "java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED", "--add-opens",
    "jdk.dynalink/jdk.dynalink.beans=ALL-UNNAMED", "--add-opens",
    "java.sql.rowset/javax.sql.rowset.serial=ALL-UNNAMED"
)

// ===== twin 源集（1.12.2 线 build.legacy122.gradle.kts:296 同机制整体移植）=====
// 共享 YesSteveModel/NativeLibLoader/ChatLogger/AnimationDebugOverlay/OuterFileTexture/
// ShadersTextureType 绑定现代 MC 面，1710 无对应——1.7.10 原生孪生（同包同名）放
// versions/1.7.10-forge/src/twin/java：twin classes 挂 main 编译/运行类路径与 jar
// 打包，包结构对消费点透明。twin 文件零条件轴（不走 stonecutter 剥离面）。
// 声明在 unimined.minecraft 块之前（源集先于消费其的配置块声明）。
sourceSets {
    create("twin") {
        java {
            srcDir(file("src/twin/java"))
        }
    }
}

unimined.minecraft {
    version(property("deps.minecraft") as String)

    mappings {
        searge()
        mcp("stable", "12-1.7.10")
    }

    minecraftForge {
        loader(property("deps.forge") as String)
        // stub 阶段挂配置不写 mixin 类（空配置 json，L1 落 RenderPlayer 切面时填）
        mixinConfig("openysm.mixins.json")
    }

    // 先例 forge1710:84-102 逐项照抄：Java21 下 launchwrapper 需 lwjgl3ify/RFB 接管
    runs.config("client") {
        javaVersion = JavaVersion.VERSION_21
        mainClass = "com.gtnewhorizons.retrofuturabootstrap.Main"
        setJvmArgs(clientJvmArgs)
    }
    // runServer 接管判负（2026-09-21 实测）：runs.config("server") 下改 mainClass 抛
    // UnsupportedOperationException（unimined server run config 不开放该面）；Celeritas
    // 先例 :104-106 直接 enabled=false。取舍：L0 启动判据走 runClient（Xvfb），
    // runServer 留 L1。
    runs.config("server") {
        enabled = false
    }
}

tasks.named<xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask>("remapJar") {
    mixinRemap {
        enableBaseMixin()
        enableMixinExtra()
        disableRefmap() // like fabric-loom 1.6
    }
}

repositories {
    mavenCentral()
    // unimixins / lwjgl3ify（GTNH nexus）
    maven("https://nexus.gtnewhorizons.com/repository/public/") { name = "GTNH" }
    // lwjgl3ify taumc 镜像（legacy-1222 线实证通道）
    maven("https://maven.taumc.org/releases") { name = "TauMC" }
}

val forgePatchDeps by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // ===== 共享解析链三依赖（research-legacy1710-l2-l3 block1 new_deps）=====
    // fastutil：GeoModel 字段 IntList/ObjectArrayList + YSMClientMapper:43——1.7.10
    // vanilla 不带 fastutil（1.12.2 自带，build.legacy122.gradle.kts:347 注释实证其靠
    // MC 传递），须显式声明。8.5.13=研究裁定基线（Java 8 字节码面）。
    implementation("it.unimi.dsi:fastutil:8.5.13")
    // asm-tree：GeoModel.java:25-29 ClassReader/ClassNode/FieldNode/MethodNode 面；
    // asm-tree 传递携带 asm 核心（ClassReader 所在）。launchwrapper 自带 asm-all 同包
    // org.objectweb.asm——运行期以 launchwrapper 先加载为准（研究 risk 已留档）。
    implementation("org.ow2.asm:asm-tree:9.7")
    // jetbrains annotations：GeoModel @NotNull（CLASS retention，不进运行面）
    compileOnly("org.jetbrains:annotations:24.0.0")
    // lwjgl3ify 现代化 runtime（用户裁决 2026-09-20）：forgePatches=运行面打补丁，
    // dev=编译/开发面。坐标 2.1.18（先例 forge1710:113-114 GTNH jitpack 同版）。
    // lwjgl3ify（GTNH jitpack 坐标，先例 forge1710:113-114 逐字：com.github.GTNewHorizons:
    // lwjgl3ify:2.1.18 + :forgePatches；org.taumc:lwjgl3ify:2.1.18 在 taumc/GTNH maven
    // 均 404 实测 2026-09-21——taumc maven 只发 commit-hash 版（1.12.2 线 d6af8e7 同款））
    implementation("com.github.GTNewHorizons:lwjgl3ify:2.1.18")
    implementation("com.github.GTNewHorizons:lwjgl3ify:2.1.18:forgePatches")
    // LWJGL3 运行面（lwjgl3ify relaunch 后 MC 请求 org.lwjgl.*，缺则
    // ClassNotFoundException: org/lwjgl/system/Platform 实测 2026-09-21；
    // 先例 forge1710:115-123 同款 3.3.3 全套+natives）
    val lwjglVersion = "3.3.3"
    implementation("org.lwjgl:lwjgl:${lwjglVersion}")
    implementation("org.lwjgl:lwjgl-opengl:${lwjglVersion}")
    implementation("org.lwjgl:lwjgl-glfw:${lwjglVersion}")
    implementation("org.lwjgl:lwjgl-stb:${lwjglVersion}")
    runtimeOnly("org.lwjgl:lwjgl:${lwjglVersion}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${lwjglVersion}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${lwjglVersion}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-stb:${lwjglVersion}:natives-linux")
    // 1.7.10 代 mixin：unimixins（Sponge 桥；先例 :125 0.1.19:dev）
    implementation("io.github.legacymoddingmc:unimixins:0.1.19:dev")
    // 共享源渲染/动画链 JOML 工作类型（1.7.10 无内置；1.12.2 线同款 1.10.5）
    implementation("org.joml:joml:1.10.5")
}

// ===== 源集挂载（M-U3 挂 yui 树；L2a legacy1710-l2a-model-load 扩到全解析链）=====
// L0 曾用 setSrcDirs 只挂版本 stub；M-U3 起 include 白名单；本卡把 1.12.2 线
// legacy122Include（build.legacy122.gradle.kts:103-129）+legacy122ExcludeMcDeps
//（:134-289）机制整体同挂：geckolib3 资源解析链（YSMFolderDeserializer 全文零
// net.minecraft import 求证，YSMClientMapper 闭包全在白名单+排除集）+resource/model/
// util/config/rip.ysm 工具树。legacy122/legacy1710 两包名互换，其余逐条同构。
// 生成树路径 build/generated/stonecutter/main/java（stonecutter 0.7 布局）。
val legacy1710Include = listOf(
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
    "rip/ysm/yui/**",
    "rip/ysm/legacy1710/**",
    // 共享 molang math 六文件的 <1.14 分支硬编码 import rip.ysm.legacy122.Mth——
    // 包名是共享链契约点，1710 在版本树该包名下提供同名 shim（Mth.java 头注详述）
    "rip/ysm/legacy122/Mth.java",
    "rip/ysm/OpenYSMStub.java",
    "rip/ysm/LegacyConfig.java",
    "net/sourceforge/pinyin4j/**",
)
// 白名单内仍绑定 MC 依赖面的重文件（122 线 L1 由 import 扫描生成同名排除集；
// 1710 轴 stonecutter 剥离组合与 122 不逐位相同——按编译报错面逐个迭代增补，
// 迭代记录见交卡报告）。批C 永久归档段与 122 同语义：GeoEntityRenderer 族=
// 现代 GPU 管线（1710 固定管线无对应物，翻译层 LegacyModelTranslator 降级替代），
// NativeModelRenderer=blaze3d NativeImage 面，均不回接编译面。
val legacy1710ExcludeMcDeps = listOf(
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
    // ===== 批C 永久归档（与 build.legacy122.gradle.kts:206 同语义）=====
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoLayerRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/GeoReplacedEntityRenderer.java",
    "com/elfmcys/yesstevemodel/geckolib3/geo/IGeoRenderer.java",
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
// ===== twin 源集（声明见 unimined.minecraft 块上方）=====
// main 源路径含 build/generated/stonecutter/main/java——生成树由 stonecutterGenerate
// 产出，但该目录对 unimined 线是纯路径 srcDir（build.legacy122.gradle.kts:335 同款），
// 补显式任务依赖（幂等）。
tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("stonecutterGenerate"))
    // twin classes 同目录合并后先于 main 编译（main 消费 twin 类：ClientModelInfo→
    // OuterFileTexture）。目录用 .get() 取裸 File（classesDirectory Property 自带
    // compileJava 产出边，直接入 classpath 会成 self-cycle）
    dependsOn(tasks.named("compileTwinJava"))
    classpath += files(sourceSets.main.get().java.classesDirectory.get().asFile)
    // 共享解析链首轮编译错误 >100 会被 javac 默认上限截断——122 线 :80 同款放开
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}
// twin classes 输出并入 main classes 目录（单一 "(main)" 条目）：run1 构造正常但
// 运行期 OuterFileTexture CNFE（twin classes 不在 launch -cp）；run2/3/5 实证任何
// 额外 twin 类路径条目（runtimeClasspath += / combineWith）都会令 FML mod 构造
// 失败（getMod()==null "appears not to have constructed correctly"）。同目录合并=
// launch -cp 形态与 run1 完全一致且 twin classes 对 LaunchClassLoader 可见，jar
// 打包经 main.output 自动携带。
tasks.named<JavaCompile>("compileTwinJava") {
    dependsOn(tasks.named("stonecutterGenerate")) // 幂等，与 122 线 twin 剥离纪律同源
    destinationDirectory.set(sourceSets.main.get().java.classesDirectory)
}
afterEvaluate {
    sourceSets.main {
        java {
            // 共享面必须走 stonecutter 展开树（//? 条件剥离在生成步；raw 条件块两侧
            // 代码都在直接编译必炸）。avif/webp/jpeg 解码 vendor 挂 1.16.5 线 imagestream
            //（build.legacy122.gradle.kts:365 先例同款，纯净零 MC import 实证）——
            // .ysm 解析链 YSMFolderDeserializer/YSMClientMapper 依赖
            setSrcDirs(listOf(
                file("build/generated/stonecutter/main/java"),
                file("src/main/java"),
                rootProject.file("versions/1.16.5-forge/src/imagestream"),
            ))
            include(legacy1710Include)
            exclude("com/elfmcys/yesstevemodel/geckolib3/extended/**")
            exclude(legacy1710ExcludeMcDeps)
        }
        resources.setSrcDirs(listOf(
            file("src/main/resources"),
            // 内置模型包（builtin/default+wine_fox 等）在共享资源树——装载链
            // BUILTIN_PREFIX classpath 直读的数据面（122 线 :372 同款双资源目录）
            rootProject.file("src/main/resources"),
        ))
    }
}
// 双资源目录（版本独有面列前=优先）：mcmod.info 等重叠条目以版本面为准
tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
// twin 编译面：twinCompileClasspath 补 MC 面（minecraft=合并打补丁 jar，
// minecraftLibraries=log4j/gson 等 vanilla 库，run4 实证缺此二配置 twin 编译炸
// net.minecraft/log4j 不存在）+ implementation/compileOnly 外部依赖。
afterEvaluate {
    configurations.getByName("twinCompileClasspath").extendsFrom(
        configurations.getByName("minecraft"),
        configurations.getByName("minecraftLibraries"),
        configurations.getByName("implementation"),
        configurations.getByName("compileOnly"),
    )
}
// twin 运行面=同目录合并机制（见 compileTwinJava 块说明），无类路径条目变更。

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Lwjgl3ify-Aware" to "true",
            "ForceLoadAsMod" to "true",
            "FMLCorePluginContainsFMLMod" to "true",
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("mcmod.info") {
        expand("version" to version)
    }
}
