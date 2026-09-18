plugins {
    id("net.neoforged.moddev")
}

version = "${property("mod_version")}-${property("deps.minecraft")}-neoforge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

// ===== M3 批二 a 起 neoforge 线共用本脚本（批二 a：1.20.4/1.20.6/1.21.1；
// 批二 b：21.3/21.4/21.5/21.8/21.10/21.11/26.1.2/26.2 后八线）=====
// 分段依据（本机 vanilla 索引 + MDG 2.0.141 源码 tmp/harvest/m3-asm-mdg-src + NeoForge 官方文档
// tmp/refs/neoforge-docs-full，检索证据见行内）：
//  - <1.20.5（仅 1.20.4）：META-INF/mods.toml 装载（docs version-1.20.4 modfiles.md
//    "The mods.toml file, located at src/main/resources/META-INF/mods.toml"）；
//    >=1.20.5 起改名 META-INF/neoforge.mods.toml（docs version-1.20.6 modfiles.md:61）
//  - Java：1.20.4=17、1.20.6/1.21.1=21（docs.neoforged.net Java 表；MDG 按 MC 版本自动
//    请求对应 toolchain——legacyforge 1.17.1 请求 16 同机制，批一实证）
//  - 运行时命名：NeoForge 1.20.2+ 全链 mojmap（无 SRG reobf）→ 无 reobfJar、无 refmap：
//    不挂 org.spongepowered:mixin:processor 注解处理器、mixins.json 不注入 refmap 键
//    （批一 forge 线的 refmap 注入是 SRG 运行时专用，neoforge 线字面 mojmap 名即真名）
val pre1205 = stonecutter.eval(stonecutter.current.version, "<1.20.5")
val mcVersion = property("deps.minecraft") as String
// neoforge 装载区间用线大版本（20.4.251→20.4 / 21.1.250→21.1）
val neoMajor = (property("deps.neoforge") as String).substringBeforeLast('.')
// 26.x 段（版本号去 1.x 前缀）：Java 25（piston-meta javaVersion.majorVersion=25 实证，
// 2026-09-13 实拉；javac<25 读不了 classfile major 69 的 MC/NeoForge jar）。
// stonecutter 数值段比较："26.1.2"/"26.2" 对 ">=26" 为 true、对 ">=1.21.x" 亦为 true、
// 对 "<1.21" 为 false——条件轴不受影响
val v26 = stonecutter.eval(stonecutter.current.version, ">=26")
// 26.1 三线（26.1/26.1.1/26.1.2，不含 26.2——26.2 submit-dag 换代独立一卡，FPM 维持 shim）：
// FirstPersonModel 真 compat 专属段（fpm-26x-pr659 卡A），挂 src/neoforge-261 孪生小树
val fpm261 = stonecutter.eval(stonecutter.current.version, ">=26.1") &&
        stonecutter.eval(stonecutter.current.version, "<26.2")
// 21.2~21.8 七线（fpm-card-b-2128 卡B，卡A 同段降配）：FirstPersonModel 真 compat
// 专属段，挂 src/neoforge-2128 孪生小树。三族 FPM jar（2.4.8/2.5.0/2.7.2）javap
// 实证 WorldRendererMixin 同步开窗盖 render 期（21.9+ 才改 extract 窗口），
// 无需 extract 快照/SetupHook
val fpm2128 = stonecutter.eval(stonecutter.current.version, ">=21.2") &&
        stonecutter.eval(stonecutter.current.version, "<21.9")
// 卡B vendor 目录（compileOnly fileTree，261 卡先例）：逐线官方 jar，
// 21.6/21.7 共用 FPM 2.5.0 单 jar（Modrinth o7XTDjvI 双 game_versions）
val fpm2128Libs = when (stonecutter.current.version) {
    "21.2" -> "libs/neoforge-212"
    "21.3" -> "libs/neoforge-213"
    "21.4" -> "libs/neoforge-214"
    "21.5" -> "libs/neoforge-215"
    "21.6", "21.7" -> "libs/neoforge-216217"
    else -> "libs/neoforge-218"
}

// 21.3+ 线 log4j 对齐：下方 eachDependency 已把 log4j-core 钉死 2.19.0（全线，NFRT 去抖），
// 而 1.21.4/1.21.5 vanilla 自带 log4j-api 2.22.x 与 core 2.19.0 错配 → 启动即
// NoSuchMethodError（ServiceLoaderUtil.loadServices 3 参签名缺失，21.4/21.5 runClient 实证）。
// 21.8 的依赖图冲突（库传递 core 2.19.0 抢占 × api 2.24.1）同根。constraints 全家对齐 2.19.0
// （21.8 runServer/runClient 绿实证的自洽组合）。1.20.4/1.20.6/1.21.1 实证 api 自然版 ×
// core 2.19.0 可用，不在此列不动。
println("[ysm] log4j alignment applied for " + stonecutter.current.version)
if (stonecutter.eval(stonecutter.current.version, ">=21.3")) {
    configurations.configureEach {
        resolutionStrategy {
            // 全家对齐 2.19.0：NFRT legacy classpath 固化 core 2.19.0（NFRT 内部解析，Gradle force
            // 无法影响），Gradle 侧必须向其看齐——api 2.24/2.25 × core 2.19 混版启动即崩（实证）
            force("org.apache.logging.log4j:log4j-core:2.19.0")
            force("org.apache.logging.log4j:log4j-api:2.19.0")
            force("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0")
            force("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
        }
    }
}

// NFRT 类路径含版本区间依赖（log4j-core 2.11.+），每次解析都 HEAD maven-metadata——
// maven.neoforged.net 偶发 502 即断构建（实测两次）→ 钉死具体版本（loader 2.0.17 自带 2.19.0）去抖
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j" && requested.name == "log4j-core") {
            useVersion("2.19.0")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
}

// ImageStream 生产内嵌（机制同 build.forge.gradle.kts imageStreamEmbed；三线 Java 17/21
// 运行时均可载 JitPack 产物 major 61，无 1.17.1 的 pre118 源码 vendor 例外）。
// dev 运行时经 additionalRuntimeClasspath 注入（见下方依赖块），生产 jar 由 jar 任务并入。
val imageStreamEmbed = configurations.create("imageStreamEmbed") {
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）：编译 + dev 运行时 + 生产内嵌
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    add("imageStreamEmbed", "com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // MixinExtras：@WrapOperation 等注解编译面；运行时 NeoForge 20.2+ 自带模块
    //（forge 线的 mixinextras-forge implementation 与 1171 的 additionalRuntimeClasspath
    // 注入均不适用——neoforge 线无独立 forge 变体 jar，也不存在 split-package 问题）
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    // 26.1 三线 FirstPersonModel api 编译面（fpm-26x-pr659 卡A）：官方
    // 2.7.2-26.1.2 neoforge jar vendor（Modrinth H5XMjpHi/fm5enNE9 实拉），仅
    // compileOnly 不进运行时——机制同 build.forge.gradle.kts:92-94 的
    // compileOnly(fileTree(libs))（1.20.1-forge 线 29 jar 先例），独立子目录避免
    // 三线误吞 libs/ 根的 forge 代 jar
    if (fpm261) {
        compileOnly(fileTree(rootProject.file("libs/neoforge-261")))
    }
    // 21.2~21.8 七线 FirstPersonModel api 编译面（fpm-card-b-2128 卡B）：机制同
    // 上 fpm261 段——官方 jar 逐线 vendor（Modrinth H5XMjpHi 实拉，版本见
    // fpm2128Libs 注），仅 compileOnly 不进运行时
    if (fpm2128) {
        compileOnly(fileTree(rootProject.file(fpm2128Libs)))
    }
}

neoForge {
    version = property("deps.neoforge") as String
    validateAccessTransformers = true

    runs {
        // client/server 分目录：与 forge 线 run/client、run/server 约定一致
        //（MDG 默认 run/ 会双进程互写 logs/latest.log，build.forge.gradle.kts 同注释）
        register("client") {
            gameDirectory = file("run/client")
            client()
        }
        register("server") {
            gameDirectory = file("run/server")
            server()
        }
    }

    mods {
        register(property("archives_name") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

// additionalRuntimeClasspath configuration 由 MDG runs 装配期（上方 neoForge 块求值时）
// 创建，故依赖声明必须置于其后（同 build.forge.gradle.kts legacyForge 块的次序约束）
dependencies {
    // MDG 按 NeoForge userdev 能力数据（legacyClasspath capability）二选一
    //（MDG 源码 ModDevRunWorkflow.java:101 legacy 分支 vs :120 新分支 + :145
    // forbidAdditionalRuntimeDependencies，批二 b 21.10 线配置期报错实证分界）：
    //  - legacy 形态（1.20.4~21.8 线）：implementation 依赖进模块层、游戏类路径不可见，
    //    ImageStream 走 MDG 官方注入口 additionalRuntimeClasspath
    //  - 新形态（21.10/21.11/26.x 线）：配置仍创建但禁止注入，报错文案明示改用标准
    //    configuration（"Add the dependency to a standard configuration such as
    //    implementation or runtimeOnly"）→ 走 runtimeOnly
    // 两分支都创建同名 configuration，判别用 canBeConsumed：legacy 分支显式
    // setCanBeConsumed(false)（ModDevRunWorkflow.java:97），新分支裸 create 默认 true
    val additionalCp = configurations.findByName("additionalRuntimeClasspath")
    if (additionalCp != null && !additionalCp.isCanBeConsumed) {
        "additionalRuntimeClasspath"("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    } else {
        runtimeOnly("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    }
}
// 配置期显式解析为 File（configuration cache 安全，勿把 Configuration 持进任务）
val imageStreamEmbedJars: Set<File> = imageStreamEmbed.files

// ===== neoforge 线专属源集（绕开 stonecutter 的 RAW srcDir，同 forge 线 shim srcDir 先例）=====
//  - src/neoforge/java：platform/neoforge 平台实现跨代同形部分（主类/能力 provider/桥，纯
//    neoforge 代码零条件块）——与 platform/forge 树镜像互斥，forge 六线 sourceSet 不含此目录零接触
//  - src/neoforge-1204/java、src/neoforge-1205/java：1.20.5 网络重铸/事件注解换代的孪生分代树
//    （同 FQCN 双版本，按线二选一挂载，防同源集双份类定义冲突）：
//      1204=1.20.4 代（RegisterPayloadHandlerEvent/IPayloadRegistrar/PlayPayloadContext/
//           Mod.EventBusSubscriber/NeoForgeMod.BLOCK_REACH），
//      1205=1.20.6/1.21.1 共用代（RegisterPayloadHandlersEvent/PayloadRegistrar.playBidirectional
//           +StreamCodec/IPayloadContext/fml.common.EventBusSubscriber/vanilla Attributes.*）
//  - src/neoforge-resources：META-INF/neoforge.mods.toml 模板（20.5+ 口径，1.20.4 线在
//    processResources 里 rename 回 mods.toml）——不能放共享 src/main/resources：
//    build.unimined.gradle.kts（1.16.5 在产线）的 processResources 无 neoforge.mods.toml
//    排除项，放共享目录会给 1.16.5 产物加新条目，破坏批一"产物语义零变化"口径
//  - compat shim 复用 1.16.5 线版本中立 shim（同 build.forge.gradle.kts pre120 块）
sourceSets.main {
    java {
        // 基础树按代分挂（见下方链尾注记）：21.5 线挂 1215（= neoforge ∪ neoforge-1205 的
        // 1.21.5 代副本）、21.6~21.9 线挂 1218（= 1215 ∪ 1213 的 1.21.8 代副本；21.6/21.7
        // 并入本分支见下方批二 c-2 注）、21.10 线挂 2110、21.11+/26.x 线挂 2111，
        // 防同 FQCN 双份类定义
        if (stonecutter.eval(stonecutter.current.version, "<21.5")) {
            srcDir(rootProject.file("src/neoforge/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<21.6")) {
            srcDir(rootProject.file("src/neoforge-1215/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<21.9")) {
            // 21.6/21.7 并入 1218 分支（批二 c-2 实证 21.6.20-beta/21.7.25-beta-sources）：
            // vanilla/neoforge API 面与 21.8 同形——EventBusSubscriber 无 bus、
            // RenderLevelStageEvent 子事件类形、getShaderTexture 返 GpuTextureView、
            // getShaderFog 返 GpuBufferSlice、createTexture 七参、PacketSendListener 类化、
            // ServerPlayer.level() 直返 ServerLevel（serverLevel 删）。唯网络分发面在
            // 21.6 断裂（无 client.network.ClientPacketDistributor，PacketDistributor
            // .sendToServer 21.7 才删）→ YSMChannelImpl 拆出独立网络树 per-线挂载
            //（同 FQCN 文件无法按线 exclude——exclude 相对路径双杀同名 RAW 文件），
            // 下方 net 树挂载注。
            srcDir(rootProject.file("src/neoforge-1218/java"))
            if (stonecutter.eval(stonecutter.current.version, "<21.7")) {
                // net16 = 1215 版 YSMChannelImpl（PacketDistributor.sendToServer 形，21.6 有）
                srcDir(rootProject.file("src/neoforge-net16/java"))
            } else {
                // net18 = 1218 版 YSMChannelImpl（client.network.ClientPacketDistributor 形，
                // 21.7 起；21.7/21.8）
                srcDir(rootProject.file("src/neoforge-net18/java"))
            }
        } else if (stonecutter.eval(stonecutter.current.version, "<21.11")) {
            // 2110 树 = 1218 的 1.21.10 代副本。分裂动因（neoforge-21.10.64-sources 实证）：
            // RenderLevelStageEvent 删 AfterBlockEntities 子事件类（RenderFirstPlayerForgeHook）、
            // PlayerRenderer → AvatarRenderer 改名（PlayerRenderStateEntityCache）。
            // 21.9 并入本分支（批二 c-2 实证 21.9.16-beta-sources）：上述 21.10 断裂点 21.9
            // 已生效（AfterBlockEntities 零命中/AvatarRenderer 在/Level.isClientSide private 化
            // +isClientSide() 访问器），且 GUI/FML 面（GuiGraphics.renderOutline 删、
            // GameProfile name()/id()、FMLEnvironment.dist/production 删、IModFile.findResource
            // 删、GuiElementRenderState.buildVertices 单参化）与 21.10 同形（共享源条件
            // 21.10→21.9 移位）；21.9 renderLevel 仍 21.8 形（WorldRendererMixin >=21.8 && <21.10
            // 分支编译实证）
            srcDir(rootProject.file("src/neoforge-2110/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<26.2")) {
            // 2111 树 = 2110 的 1.21.11 代副本：net.minecraft.resources.ResourceLocation →
            // Identifier 全树改名（同包同 API，neoforge-21.11.45-sources 实证）+ Arrow 族
            // 移 projectile.arrow 子包（ArrowPotionAccessor 目标）
            srcDir(rootProject.file("src/neoforge-2111/java"))
            if (v26) {
                // 26.x 分歧（neoforge-26.1 RenderLevelStageEvent 实证）：删 AfterEntities 子事件
                // → RenderFirstPlayerForgeHook 走 2610 孪生小树取 AfterOpaqueFeatures。
                // 异包 event26x 原因（212 树先例）：exclude 按相对路径双杀同名 RAW 文件，
                // 孪生必须异包；@EventBusSubscriber 注解自注册，包名无关。
                exclude("com/elfmcys/yesstevemodel/platform/neoforge/event/RenderFirstPlayerForgeHook.java")
                srcDir(rootProject.file("src/neoforge-2610/java"))
            }
        } else {
            // 262 树 = 2111 的 26.2 代副本（m3-262-submit-dag-port）：26.2 MultiBufferSource/
            // RenderBuffers 全删（/tmp/vanilla-262 全树 0 引用实证）→ RenderPlayerEvent/
            // RenderArmEvent/RenderHandEvent 携带的 SubmitNodeCollector 直传；全树分代挂载同
            // FQCN（2110→2111 先例），2610 event26x 异包孪生被本树吸收（RenderFirstPlayerForgeHook
            // 内联 AfterOpaqueFeatures 版），v26 孪生挂载仅剩 26.1 线
            // 26.3 分叉（m3-263-increment）：RenderArmEvent 删 getAvatar（携 PlayerRenderState，
            // 事件仅 FirstPersonHandsAndItemsRenderer 首-person 自臂触发）→ 本树不可再复用
            if (stonecutter.eval(stonecutter.current.version, "<26.3")) {
                srcDir(rootProject.file("src/neoforge-262/java"))
            } else {
                srcDir(rootProject.file("src/neoforge-263/java"))
            }
        }
        if (pre1205) {
            srcDir(rootProject.file("src/neoforge-1204/java"))
            srcDir(rootProject.file("src/neoforge-pre1213/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<1.21")) {
            // 1205 树 = 1.20.6 代共用部分；1206 树 = 1.20.6 独占分歧
            //（ShieldBlockEvent 在 1.21.1 更名 LivingShieldBlockEvent）
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1206/java"))
            srcDir(rootProject.file("src/neoforge-pre1213/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<1.21.2")) {
            // 1.21/1.21.1（批二 c-2 注：1.21 缩写轴新线与本分支同形——renderstate 包
            // 21.3 才有，1213 树不可用）：1205 树 + 1211 树（1.21.1 分歧：
            // LivingShieldBlockEvent、ItemAbilities 更名）+ pre1213 树；
            // shim 挂 1211 副本（见下方 shim 块）
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1211/java"))
            srcDir(rootProject.file("src/neoforge-pre1213/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<21.5")) {
            // 1.21.2~1.21.4：1205 树跨代同形部分（MobEffect/FirstPlayer/HandRender 钩子、
            // 网络与能力桥、ArrowPotionAccessor——ReplacePlayerRenderForgeHook 已移出至
            // 1206/1211 树）+ 1213 树 = 1.21.2 render-state 化分歧独占（RenderLivingBridgeImpl
            // state 形 / ReplacePlayerRenderForgeHook state 形 + 实体反查缓存 /
            // ToolActionBridgeImpl 双参 onEntitySwing / 1211 同形桥副本：ShieldBlock 冷却与
            // BufferBuilder 桥）；shim 沿用 1211 副本（1.21.2+ ResourceLocation 面与 1.21.1
            // 同形，21.3 编译实证）
            // 21.2（MC 1.21.2）混合形态（批二 c-2 实证 21.2.1-beta-sources）：vanilla 侧
            // render-state 化/ClientInput/inGround 封装已与 21.3 同形（1213 树可用），但
            // neoforge 侧 net.neoforged.neoforge.client.renderstate 包与
            // RegisterRenderStateModifiersEvent 21.3 才引入 → 1213 版
            // PlayerRenderStateEntityCache 不可用：剔除后挂 212 小树（异包 event212 孪生 +
            // PlayerRenderStateStashMixin 在 vanilla PlayerRenderer.extractRenderState
            // stash 实体，mixins.json 条目经 processResources per-line 注入；exclude 相对
            // 路径双杀同名 RAW 文件，故 212 版必须异包——批二 a「异包策略」先例）
            if (stonecutter.current.version == "21.2") {
                exclude(
                    "com/elfmcys/yesstevemodel/platform/neoforge/event/PlayerRenderStateEntityCache.java",
                    "com/elfmcys/yesstevemodel/platform/neoforge/event/ReplacePlayerRenderForgeHook.java",
                )
                srcDir(rootProject.file("src/neoforge-212/java"))
            }
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1213/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<21.6")) {
            // 21.5：1215 树 = (neoforge ∪ neoforge-1205) 1.21.5 代副本（已在上方基础挂载处挂载）。
            // 分裂动因：CompoundTag.getCompound Optional 化（ForgeCapabilityHooks）与
            // KeyModifier.getActiveModifier 删除（KeyMappingFactoryImpl）在 RAW 树无条件下不可
            // 两代共存（同 FQCN 二选一挂载防双份类定义）；1213 树 21.5 编译零残差，继续共用。
            srcDir(rootProject.file("src/neoforge-1213/java"))
        } else {
            // 21.6~21.9：1218 树（含网络树 net16/net18）已在上方基础挂载处挂载，本段
            // （1205/1206/1211/pre1213/1213/1215）不再挂载（21.6/21.7 与 21.8 vanilla/neoforge
            // 同形实证见上方分支注；二次挂载会触发 sourcesJar 重复条目）
            // 1218 分裂动因（21.6 起逐项前移实证）：EventBusSubscriber 删 bus 属性、
            // RenderLevelStageEvent 拆子事件类、PacketDistributor.sendToServer→
            // ClientPacketDistributor（21.7）、GPU 面六点分界 21.8→21.6（见共享源条件注）
        }
        // shim：<1.21 挂原件；1.21+ 挂整树副本（ResourceLocation 私有构造 /
        // isValidResourceLocation 删除 → Rl/parse，2 文件已修，RAW 无条件化能力）；
        // >=21.11 挂 2111 副本（ResourceLocation→Identifier 同步改名）
        if (stonecutter.eval(stonecutter.current.version, "<1.21")) {
            srcDir(rootProject.file("versions/1.16.5-forge/src/shim/rip/ysm/compat"))
        } else if (stonecutter.eval(stonecutter.current.version, "<21.11")) {
            srcDir(rootProject.file("src/neoforge-1211/shim/rip/ysm/compat"))
        } else if (stonecutter.eval(stonecutter.current.version, "<26.2")) {
            srcDir(rootProject.file("src/neoforge-2111/shim/rip/ysm/compat"))
        } else if (stonecutter.eval(stonecutter.current.version, "<26.3")) {
            // 26.2 代 shim 副本（SlashBladeRenderer collector 形签名分歧 1 文件，m3-262-submit-dag-port）
            srcDir(rootProject.file("src/neoforge-262-shim/shim/rip/ysm/compat"))
        } else {
            // 26.3 代 shim 副本（262-shim 分代；ItemUseAnimationPredicate 26.3 swing 字段
            // 换代 isSwinging()——RAW 树无条件化能力，照 262 分代先例 fork）
            srcDir(rootProject.file("src/neoforge-263-shim/shim/rip/ysm/compat"))
        }
        // 26.1 三线 FirstPersonModel 真 compat（fpm-26x-pr659 卡A）：shim 的
        // rip/ysm/compat/firstperson/FirstPersonCompat.java（恒 false）对这三线剔除，
        // 换 src/neoforge-261 孪生小树真实现。孪生异包 platform/neoforge/firstperson
        // （212/2610 树先例：exclude 按相对路径双杀同名 RAW 文件，孪生必须异包），
        // 接缝消费方四文件 import 经 stonecutter 行条件交换（AuthModelsCapability 缝先例）。
        // 本块只影响挂载表/依赖清单，零新机制。
        if (fpm261) {
            // exclude 相对路径以各 srcDir 根为基准：2111 shim 挂载根=.../shim/rip/ysm/compat，
            // 其 firstperson 文件相对路径只有 firstperson/FirstPersonCompat.java
            exclude("firstperson/FirstPersonCompat.java")
            // 2111 树 ReplacePlayerRenderForgeHook 同剔，换 261 孪生（异包
            // platform/neoforge/firstperson，2610 event26x 先例）：加旗标采样链消费窗
            exclude("com/elfmcys/yesstevemodel/platform/neoforge/event/ReplacePlayerRenderForgeHook.java")
            srcDir(rootProject.file("src/neoforge-261/java"))
        }
        // 21.2~21.8 七线 FirstPersonModel 真 compat（fpm-card-b-2128 卡B）：机制同上
        // fpm261 段——shim 的 firstperson/FirstPersonCompat.java（恒 false）对七线剔除，
        // 换 src/neoforge-2128 孪生小树（异包 platform/neoforge/firstperson，与 261 孪生
        // 同 FQCN：两段条件轴不相交，按线二选一挂载）。与 26.1 的差异：不剔
        // ReplacePlayerRenderForgeHook（三族 FPM jar javap 实证旗标盖 render 期，
        // 无快照采样链，既有 1213/1215/1218/212 树 hook 原样保留）。
        if (fpm2128) {
            exclude("firstperson/FirstPersonCompat.java")
            srcDir(rootProject.file("src/neoforge-2128/java"))
        }
        // ===== d3-gpu-218-revive：21.8/21.11 线 GPU 路径复活捕获 mixin =====
        // vanilla 21.8 删 RenderSystem CPU 投影/雾读取（GpuBufferSlice 化）后，四个捕获
        // mixin 在 vanilla 打包投影/雾 UBO 前的 CPU 现算点喂回 GpuCapability 捕获面
        //（1218 证据：GameRenderer:671/FogRenderer:166→updateBuffer:213；21111：
        // GameRenderer:771/FogRenderer:162→updateBuffer:205，两线捕获面逐参同构）。
        // 仅挂两实证线（21.6/21.7/21.9/21.10/26.x 未实证不挂，门未捕获=保持 CPU 现状）。
        // RAW 树绕开 stonecutter（212 树 PlayerRenderStateStashMixin 先例）；类面零
        // vanilla 类型引用差异（org.joml/java.nio/mojmap 目标两线同名），单树双线共用，
        // 无 Identifier 改名/RenderTypes 改写问题（后处理只走生成树，不触 RAW）。
        // mixins.json 条目经 processResources per-line 注入（stash212 先例），见任务块尾。
        if (stonecutter.current.version == "21.8" || stonecutter.current.version == "21.11") {
            srcDir(rootProject.file("src/neoforge-gpu218/java"))
        }
        // 第三方触点源码闸门 + platform/forge 树整体排除（清单与 build.forge.gradle.kts pre120
        // 块同源）。孪生走异包策略：src/neoforge/java 下 platform/neoforge 包（类名不变），
        // 接缝消费方 import 交换（transform_neoforge.py），排除 glob 不会误伤孪生：
        exclude(
            "rip/ysm/compat/**",
            "com/elfmcys/yesstevemodel/client/compat/**",
            "com/elfmcys/yesstevemodel/platform/forge/**",
            "com/elfmcys/yesstevemodel/client/gui/button/ConfigCheckBoxForge.java",
            "rip/ysm/api/attribute/platform/forge/**",
            "rip/ysm/api/client/platform/forge/**",
            "rip/ysm/api/entity/platform/forge/**",
            "rip/ysm/api/item/platform/forge/**",
        )
    }
    resources {
        srcDir(rootProject.file("src/neoforge-resources"))
    }
}

tasks {
    processResources {
        // forge 口径 mods.toml 与第三方 accessor 配置不进 neoforge 产物
        exclude("META-INF/mods.toml", "yes_steve_model_forge.mixins.json",
            "**/fabric.mod.json", "**/*.accesswidener")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    // ===== 1.21.11 Identifier 改名（stonecutter 生成树后处理，>=21.11 含 26.x）=====
    // 1.21.11 vanilla net.minecraft.resources.ResourceLocation 改名 Identifier：同包同 API 面
    //（neoforge-21.11.45-sources net/minecraft/resources/Identifier.java:16 实证——parse/
    // fromNamespaceAndPath/withDefaultNamespace/tryParse/withPrefix/withSuffix/getPath/getNamespace
    // 全数保留，仅 isAllowedInResourceLocation→isAllowedInIdentifier，共享源零调用），共享树 104 文件
    // 的类型名引用不值得逐行条件化 → 生成树一次性语义等价改写，共享源零搅动、在产线零接触
    //（任务只在 >=21.11 线注册）。生成树由 stonecutterGenerate 重刷时恢复 RL 名，本任务幂等重写；
    // RAW 源集（平台/compat shim 树）绕开 stonecutter，走 2111 分代副本（见 sourceSets 挂载注）。
    // @OnlyIn 剥离门分代（m3-neoforge-server-dist-fix）：21.7（loader 9.0.14）起 OnlyInWarningsHandler
    // 把 mod 类 @OnlyIn 记为 ERROR（专用服 ERROR 流+client 阻断警告屏，21.7 runServer 复现实证），
    // 与 21.8（loader 9.0.18 同款警告屏）同代 → 剥离门从 >=21.8 下探 >=21.7；<=21.6 线
    // RuntimeDistCleaner 成员剥离仍在（loader jar 实证），@OnlyIn 是专用服保护面，注解必须保留
    if (stonecutter.eval(stonecutter.current.version, ">=21.7")) {
        val genJavaDir = layout.buildDirectory.dir("generated/stonecutter/main/java")
        // 配置缓存铁律：doLast 只可捕获局部 String/Provider，stonecutter 脚本对象引用不可序列化
        val curVersion = stonecutter.current.version
        val is21_11 = stonecutter.eval(stonecutter.current.version, ">=21.11")
        val is26 = stonecutter.eval(stonecutter.current.version, ">=26")
        val is26_2 = stonecutter.eval(stonecutter.current.version, ">=26.2")
        val is26_3 = stonecutter.eval(stonecutter.current.version, ">=26.3")
        val rlToIdentifier = register<org.gradle.api.DefaultTask>("rlToIdentifier") {
            dependsOn("stonecutterGenerate")
            mustRunAfter("stonecutterGenerate")
            // 声明输出=生成树：doLast 的原位改写必须让下游 compileJava 失效重编
            //（无 outputs 声明时 Gradle 的 up-to-date 检查看不到本次改写，21.10 实证）
            outputs.dir(genJavaDir)
            doLast {
                val root = genJavaDir.get().asFile
                if (root.isDirectory) {
                    var count = 0
                    // [>=21.8] @OnlyIn(Dist.CLIENT) 注解行剥离——注解残留被 OnlyInWarningsHandler
                    //  记为加载错误：loader 10 卡死 Client network registry lock（21.10 runClient
                    //  实证）；21.8（loader 9.0.18）为阻断式「Warning while loading mods」警告屏，
                    //  须手点 Proceed 才进主菜单（21854 runClient 截图实证）
                    // [>=21.11] a) ResourceLocation → Identifier（同包纯改名）
                    //           b) （撤销）location()→identifier() 仅 ResourceKey 系成立，
                    //              TagKey/自有 ItemTag 保留 location() → 共享源位点级双行
                    //           c) RenderType 静态工厂（entityCutoutNoCull/entityTranslucent/
                    //              lineStrip/outline/entityCutoutNoCullZOffset）→ RenderTypes
                    //              同名工厂（rendertype 包）
                    //（全部基于 neoforge-21.10.64/21.11.45-sources 实证）
                    val rules = mutableListOf(
                        Regex("(?m)^[ \\t]*@OnlyIn\\(Dist\\.CLIENT\\)[ \\t]*\\r?\\n") to ""
                    )
                    if (is21_11) {
                        rules.add(Regex("\\bResourceLocation\\b") to "Identifier")
                        rules.add(Regex("\\bRenderType\\.(armorCutoutNoCull|entityCutoutNoCullZOffset|entityCutoutNoCull|entitySolid|entityTranslucentEmissive|entityTranslucent|lineStrip|outline)\\(")
                            to "net.minecraft.client.renderer.rendertype.RenderTypes.$1(")
                    }
                    if (is26) {
                        // 26.1 GUI 换代（vanilla-26.1 实证）：net.minecraft.client.gui.GuiGraphics 类
                        // 删除，同包改名 GuiGraphicsExtractor（extract 模型；Screen.render→
                        // extractRenderState/AbstractWidget.renderWidget→extractWidgetRenderState
                        // 等方法面换代在共享源分代，见各 GUI 文件）。纯类型名机械改写同 Identifier 先例。
                        rules.add(Regex("\\bGuiGraphics\\b") to "GuiGraphicsExtractor")
                        // render-state 包整体搬家：net.minecraft.client.gui.render.state →
                        // net.minecraft.client.renderer.state.gui（GuiElementRenderState.java 包声明实证；
                        // neoforge-26.1 GuiGraphicsExtractor patch 的 submitGuiElementRenderState
                        // 签名同步新包，方法名不变）
                        rules.add(Regex("net\\.minecraft\\.client\\.gui\\.render\\.state\\.") to "net.minecraft.client.renderer.state.gui.")
                        // 26.1 RenderTypes 工厂更名：entityCutoutNoCull → entityCutout
                        //（26.1 RenderTypes.java:451；本条须排在 is21_11 FQN 改写规则之后）
                        rules.add(Regex("RenderTypes\\.entityCutoutNoCull\\(") to "RenderTypes.entityCutout(")
                    }
                    if (is26_2) {
                        // 26.2 submit-dag 换代（/tmp/vanilla-262 + neoforge-26.2 实证，m3-262 卡）：
                        // a) Minecraft.setScreen/screen 收进 Gui 门面（Gui.java:221 screen()/:225
                        //    setScreen；vanilla Options.java:1405 minecraft.gui.screen() 同款消费形）
                        rules.add(Regex("\\bMinecraft\\.getInstance\\(\\)\\.setScreen\\(") to "Minecraft.getInstance().gui.setScreen(")
                        rules.add(Regex("\\b(minecraft|mc|client)\\.setScreen\\(") to "$1.gui.setScreen(")
                        rules.add(Regex("\\bMinecraft\\.getInstance\\(\\)\\.screen\\b") to "Minecraft.getInstance().gui.screen()")
                        rules.add(Regex("\\b(minecraft|mc|client)\\.screen\\b") to "$1.gui.screen()")
                        // b) Minecraft.renderNames() 删（GUI-enabled 检查 26.x 无对位 API，恒真语义）
                        rules.add(Regex("Minecraft\\.renderNames\\(\\) && ") to "")
                        rules.add(Regex(" && Minecraft\\.renderNames\\(\\)") to "")
                        // d) I18n.exists(String) 删（26.2 I18n.java 仅余 get）→ Language.has（locale 包）
                        rules.add(Regex("\\bI18n\\.exists\\(") to "net.minecraft.locale.Language.getInstance().has(")
                        // c) ChatFormatting 色值字段删（26.2 ChatFormatting.java 仅余 code/toString）→
                        //    legacy 0xRRGGBB 色板字面量（ getColor().intValue() 恒等替换）
                        rules.add(Regex("ChatFormatting\\.DARK_RED\\.getColor\\(\\)\\.intValue\\(\\)") to "0xAA0000")
                        rules.add(Regex("ChatFormatting\\.DARK_GRAY\\.getColor\\(\\)\\.intValue\\(\\)") to "0x555555")
                        rules.add(Regex("ChatFormatting\\.AQUA\\.getColor\\(\\)\\.intValue\\(\\)") to "0x55FFFF")
                        rules.add(Regex("ChatFormatting\\.GRAY\\.getColor\\(\\)\\.intValue\\(\\)") to "0xAAAAAA")
                        rules.add(Regex("ChatFormatting\\.GOLD\\.getColor\\(\\)\\.intValue\\(\\)") to "0xFFAA00")
                        rules.add(Regex("ChatFormatting\\.GREEN\\.getColor\\(\\)\\.intValue\\(\\)") to "0x55FF55")
                    }
                    if (is26_3) {
                        // 26.3 PoseStack 换代（/tmp/vanilla-263 PoseStack.java:44-95 实证）：
                        // mulPose(Quaternionf) 删 → rotate(Quaternionfc)/rotate(Axis,float)
                        //（Quaternionf 实现 Quaternionfc，单参调用直换）。mulPose(Matrix4fc) 保留，
                        // 负向先行排除 pose.pose() 实参（本仓全部 3 处 Matrix4fc 位点均为
                        // inner.mulPose(pose.pose()) 形，CustomPlayerElytraLayer:163 等）；
                        // 生成树注释内同形文本一并命中=注释漂移，不入源、不审计
                        rules.add(Regex("\\.mulPose\\((?!pose\\.pose\\()") to ".rotate(")
                    }
                    root.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { f ->
                        val text = f.readText()
                        if (rules.any { (re, _) -> re.containsMatchIn(text) }) {
                            var next = text
                            for ((re, rep) in rules) next = re.replace(next, rep)
                            f.writeText(next)
                            count++
                        }
                    }
                    println("[ysm] rlToIdentifier: ${count} files rewritten for ${curVersion}")
                }
            }
        }
        named<org.gradle.api.tasks.compile.JavaCompile>("compileJava") { dependsOn(rlToIdentifier) }
        // sourcesJar 打包生成树源码，同样依赖改写后内容
        matching { it.name == "sourcesJar" }.configureEach {
            dependsOn(rlToIdentifier)
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }

    jar {
        // 跨版本测试 harness 只进 dev run classpath，不进生产产物（同 forge 线口径）
        exclude("rip/ysm/harness/**")
        // ImageStream 生产内嵌：类文件 + javax.imageio SPI services 并入主 jar（剥 manifest/签名）
        from(imageStreamEmbedJars.map { zipTree(it) }) {
            include("rip/**", "META-INF/services/**")
        }
        // 生产环境 Mixin 配置发现：NeoForge 20.4+ 自动扫描 mod 内 mixin 配置（dev run 实证），
        // manifest 项为 belt-and-braces（老版本 discovery 兜底，与 forge 线口径一致）
        manifest {
            attributes("MixinConfigs" to "yes_steve_model.mixins.json")
        }
    }
}

java {
    withSourcesJar()
    // 1.20.4=17；1.20.6/1.21.x=21（docs.neoforged.net Java 表）；26.x=25（piston-meta 实证，
    // toolchain 由 MDG 按 NeoForge userdev 能力数据自动请求，MDG 源码
    // ModDevArtifactsWorkflow.java:103 convention(javaVersion()) 实证）。26.x 用
    // toVersion(25) 规避枚举面差异（Gradle 9.2.1 JavaVersion 未必含 VERSION_25 字面量）
    val javaCompat = when {
        pre1205 -> JavaVersion.VERSION_17
        v26 -> JavaVersion.toVersion(25)
        else -> JavaVersion.VERSION_21
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

tasks.withType<JavaCompile>().configureEach {
    options.release = when {
        pre1205 -> 17
        v26 -> 25
        else -> 21
    }
}

tasks.named<ProcessResources>("processResources") {
    // 26.2：logoFile 弃用警告会经 ModLoadingIssue 顶起 LoadingErrorScreen（见下方 filter 注）
    val stripLogoFile262 = stonecutter.eval(stonecutter.current.version, ">=26.2")
    val props = mapOf(
        "mod_id" to project.property("archives_name") as String,
        "mod_name" to project.property("mod_name") as String,
        "mod_version" to project.property("mod_version") as String,
        "mod_license" to project.property("mod_license") as String,
    )


    // 1.20.4 线：neoforge.mods.toml 模板 rename 回 META-INF/mods.toml（20.5 起才改名）
    if (pre1205) {
        rename { fileName ->
            if (fileName == "neoforge.mods.toml") "mods.toml" else fileName
        }
    }
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
    // ===== per-line 资源口径替换（模板基线值=1.20.6 线；与共享源字面量逐字对照）=====
    // neoforge.mods.toml / 1.20.4 改名后的 mods.toml：neoforge/minecraft 装载区间
    //（loaderVersion 三线统一 "[1,)"，docs version-1.20.4/1.20.6/1.21.1 modfiles.md 明文）
    // 配置缓存铁律：filter lambda 只可捕获任务配置块内的局部 val（脚本顶层 val = 脚本对象
    // 引用，不可序列化——1.20.6 build 配置缓存实测报错），先物化为局部量
    val neoMajorLocal = neoMajor
    // FML 版本比较用游戏真实版本串：21.x 线的 ID 是缩写，MC 真身 = "1."+ID（21.8→1.21.8，
    // crash report "Currently, minecraft is 1.21.8" vs 范围 "[21.8,)" 不匹配实证）；26.x 起新纪元 ID 即真身
    val mcReal = if (mcVersion.startsWith("21.")) "1.$mcVersion" else mcVersion
    val mcRealLocal = mcReal
    filesMatching(listOf("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        filter { line: String ->
            var out = line.replace("versionRange = \"[20.6,)\"", "versionRange = \"[$neoMajorLocal,)\"")
                .replace("versionRange = \"[1.20.6,)\"", "versionRange = \"[$mcRealLocal,)\"")
            // 26.2 ClientModLoader.java:92-102：任何 ModLoadingIssue（含 logoFile 弃用警告）
            // 都会顶起 LoadingErrorScreen 挡住主菜单 → 26.2 改用 bannerFile（警告文案指定键）
            if (stripLogoFile262) {
                out = out.replace("logoFile = ", "bannerFile = ")
            }
            out
        }
    }
    // pack.mcmeta 资源包格式（共享源为 1.20.1 口径 15）：
    // 1.20.4=22 / 1.20.6=32 / 1.21.1=34（批二 a）；批二 b 各线取 minecraft.wiki Pack format
    // 表（2026-09-13 实拉，26.1 与本机 vanilla-mc/26.1 version.json pack_version
    // resource_major=84 互证）：1.21.3=42 / 1.21.4=46 / 1.21.5=55 / 1.21.8=64 /
    // 1.21.10=69 / 1.21.11=75 / 26.1.2=84 / 26.2=88
    // 批二 c-2（2026-09-14 同表实拉）：1.20.2=18 / 1.20.3=22（1.20.3~1.20.4 同档）/
    // 1.20.5=32（1.20.5~1.20.6 同档）/ 1.21=34（1.21~1.21.1 同档）/ 21.2=42（1.21.2~1.21.3
    // 同档）/ 21.6=63 / 21.7=64（1.21.7~1.21.8 同档）/ 21.9=69（1.21.9~1.21.10 同档）
    // 26.x 适配（2026-09-15 同表实拉）：26.1=84 / 26.1.1=84（wiki 表 84.0 档跨
    // 26.1~26.1.2 全线，与 MC 26.1 client version.json resource_major=84 互证）/ 26.2=88
    val is26Pack = stonecutter.eval(stonecutter.current.version, ">=26")
    val packFormat = mapOf(
        "1.20.4" to 22,
        "1.20.6" to 32,
        "1.21.1" to 34,
        "21.3" to 42,
        "21.4" to 46,
        "21.5" to 55,
        "21.8" to 64,
        "21.10" to 69,
        "21.11" to 75,
        "26.1" to 84,
        "26.1.1" to 84,
        "26.1.2" to 84,
        "26.2" to 88,
        "26.3" to 97,
        "1.20.2" to 18,
        "1.20.3" to 22,
        "1.20.5" to 32,
        "1.21" to 34,
        "21.2" to 42,
        "21.6" to 63,
        "21.7" to 64,
        "21.9" to 69,
    )[mcVersion] ?: 15
    filesMatching("pack.mcmeta") {
        // 26.x：PackFormat 新制——pack_format 声明 >lastPreMinorVersion(81) 时强制
        // min_format/max_format 双字段（26.1.2 PackFormat.java:159-166 validate + 26.1
        // tour 首跑 server JsonParseException 实证）→ 改双 int 字段声明
        filter { line: String ->
            if (is26Pack) {
                line.replace("\"pack_format\": 15", "\"min_format\": $packFormat,\n        \"max_format\": $packFormat")
            } else {
                line.replace("\"pack_format\": 15", "\"pack_format\": $packFormat")
            }
        }
    }
    // mixins.json compatibilityLevel：1.20.4 产物 Java 17 字节码 → 保持 JAVA_17；
    // 1.20.6/1.21.1 产物 Java 21 字节码 + Java 21 运行时 → JAVA_17 声明双不符，替换为 JAVA_21
    //（机制同 build.forge.gradle.kts 的 pre118 JAVA_17→JAVA_16 替换；产物实测归终验 d）
    // Arrow 效果 accessor 版本化：1.20.5+ Arrow.effects 字段删除（数据组件化），换成
    // PotionContents Invoker（src/neoforge-{1205,1211}/java .../mixin/client/ArrowPotionAccessor）
    if (!pre1205) {
        // 配置缓存铁律：lambda 内只引任务配置块局部 val（stonecutter 是脚本对象引用）
        val dropBufferBuilderMixin = stonecutter.eval(stonecutter.current.version, ">=1.21")
        // 26.x 产物 Java 25 字节码（major 69 实测）→ 声明 JAVA_25（26.x 运行时 mixin=
        // fabric sponge-mixin 0.17.3+mixin.0.8.7，userdev config.json 实证）
        val is26 = stonecutter.eval(stonecutter.current.version, ">=26")
        // 配置缓存铁律：条件在配置期物化为局部量，filter 内不可捕 stonecutter 脚本对象
        val dropRenderSystemAccessor = stonecutter.eval(stonecutter.current.version, ">=21.6")
        // 21.2 混合形态：render-state 实体 stash mixin 注入（src/neoforge-212 小树配套，
        // 见 sourceSets 挂载注）
        val stash212 = stonecutter.current.version == "21.2"
        // d3-gpu-218-revive：21.8/21.11 线 GPU 复活捕获 mixin 注册（src/neoforge-gpu218
        // 小树配套，挂载见 sourceSets 块）。共享 mixins.json 恒不加条目——forge vcs 直通线
        // 无本 filter，目标类缺席会运行时崩（1.20.1 红线）。锚=client.ThrowableItemProjectileAccessor
        //（前序规则不触碰该条目）
        val gpuCapture218 = stonecutter.current.version == "21.8" || stonecutter.current.version == "21.11"
        // 26.2 BufferSourceMixin 剔除闸（render-dag 换代，见 filter 内注）
        val stripBufferSourceMixin262 = stonecutter.eval(stonecutter.current.version, ">=26.2")
        filesMatching("*.mixins.json") {
            filter { line: String ->
                var out = line.replace("\"JAVA_17\"", if (is26) "\"JAVA_25\"" else "\"JAVA_21\"")
                    .replace("\"client.ArrowEntityAccessor\"", "\"client.ArrowPotionAccessor\"")
                if (stash212) {
                    out = out.replace(
                        "\"client.BufferSourceMixin\"",
                        "\"client.BufferSourceMixin\", \"client.PlayerRenderStateStashMixin\""
                    )
                }
                if (stripBufferSourceMixin262) {
                    // 26.2 BufferSourceMixin 目标类（MultiBufferSource.BufferSource）删
                    //（render-dag 换代）→ 条目按线剔除；类本体空类+MixinTweaker 双保险
                    out = out.replace("\"client.BufferSourceMixin\", ", "")
                }
                if (dropRenderSystemAccessor) {
                    // 1.21.9 RenderSystem.shaderLightDirections 改 GpuBufferSlice → accessor 失效，
                    // 注冊表剔除（GpuRenderPath.refreshLights 已有默认平行光兜底）
                    out = out.replace("\"client.RenderSystemAccessor\", ", "")
                }
                if (dropBufferBuilderMixin) {
                    // 1.21 BufferBuilder 原生内存重构（无 buffer/nextElementByte/ensureCapacity，
                    // vanilla-1.21.1 BufferBuilder.java:18-31）——JNI SIMD 直传面不存在，
                    // mixin 条目移除（原生渲染按 m2 ADR 降级为 vanilla 路径，归 native 卡验收）
                    out = out.replace(", \"client.BufferBuilderMixin\"", "")
                }
                if (gpuCapture218) {
                    out = out.replace(
                        "\"client.ThrowableItemProjectileAccessor\"",
                        "\"client.ThrowableItemProjectileAccessor\", \"client.FogUniformCaptureMixin\", " +
                            "\"client.LevelProjectionCaptureMixin\", \"client.HudProjectionCaptureMixin\", " +
                            "\"client.GuiProjectionCaptureMixin\""
                    )
                }
                out
            }
        }
    }
}

tasks.named<org.gradle.api.tasks.JavaExec>("runServer") {
    standardInput = System.`in`
}
