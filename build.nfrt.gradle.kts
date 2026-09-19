// ===== NFRT 直驱线构建脚本（nfrt-poc-1202 立架，nfrt-flatline-1203-1205 扩全量 mod 面）=====
// 挂载线（settings.gradle.kts match 键）：1.20.2-neoforge(tile 20.2.93) /
// 1.20.3-neoforge(20.3.8-beta) / 1.20.5-neoforge(20.5.21-beta)。三线官方 maven 无
// Gradle .module 元数据，MDG capability 解析必负（tasks.m3-nfrt-direct-drive-poc 判负），
// 本脚本 JavaExec 直调 NFRT CLI 自建工件/资产/运行面，与 build.moddev.gradle.kts 零共享代码路径。
//
// 旗标与机制出处（逐字对齐，禁凭记忆）：MDG 2.0.141 源码
// tmp/harvest/m3-asm-mdg-src/src/main/java/net/neoforged/：
//   - nfrtgradle/CreateMinecraftArtifacts.java:266-394（run 旗标序列：--neoforge/--dist joined/
//     --write-result <id>:<path>；:345-346 clientResources 结果与 gameJar 同次请求）
//   - nfrtgradle/DownloadAssets.java:96-113（download-assets --write-properties/--neoforge）
//   - nfrtgradle/NeoFormRuntimeTask.java:107-172（--home-dir/--work-dir；NFRT 本体 Java 21）
//   - moddevgradle/internal/ModDevPlugin.java:63（NFRT notation 必带 :userdev——无主 jar 线同根）
//   - moddevgradle/internal/ModDevRunWorkflow.java:99-141/235-240/327-351（legacyClasspath 面：
//     game jar + client-extra + 游戏库 + additionalRuntimeClasspath；modLocatorRework=false 代
//     MOD_CLASSES 走 mod 文件夹注记）
//   - moddevgradle/internal/PrepareRunOrTest.java:238-335（userdev config.json runs.{client,server}
//     的 jvmArgs {modules}/props -D 全量下传 + args {assets_root}/{asset_index} 插值 +
//     -Dlog4j2.configurationFile）
//   - moddevgradle/internal/RunUtils.java:41-44/61-142/270-299（log4j2.xml 模板；
//     MOD_CLASSES=modid%%dir File.pathSeparator 串接）
//   - moddevgradle/internal/RepositoriesPlugin.java:46-53（net.neoforged:minecraft-dependencies
//     由 https://maven.neoforged.net/mojang-meta/ 以 gradleMetadata 提供）
//   - minecraftdependencies/{MinecraftDistribution,OperatingSystem}.java（variant 属性名
//     net.neoforged.distribution=client / net.neoforged.operatingsystem=linux）
// 三线 userdev config.json（runs/libraries/modules 真相源，本脚本配置期直读）：
//   20.2.93=本机缓存解包实证（FML 1.0.16，spec 2）；20.3.8-beta/20.5.21-beta=maven userdev
//   200 实证（2026-09-19），FML 版本各线以各自 config.json 为准。
// FML 1.0.16 的 [[mixins]] 表支持：loader-1.0.16.jar ModFileParser getConfigList("mixins")
// 字节码实证（javap ldc "mixins" → IConfigurable.getConfigList），模板声明面三线通用。
// ⚠ 本脚本配置期解析 userdev jar（singleFile）——三线一律 --no-configuration-cache 跑
//  （harness/tour.sh 内建该旗标；根 gradle.properties 的 configuration-cache 对本线不适用）。

import java.util.Properties
import java.util.zip.ZipFile

plugins {
    `java-library`
}

version = "${property("mod_version")}-${property("deps.minecraft")}-neoforge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

val neoVersion = property("deps.neoforge") as String
val mcVersion = property("deps.minecraft") as String
val modId = property("archives_name") as String
// NFRT 版本 = MDG 2.0.141/2.0.147 默认（NeoFormRuntimeExtension.java:15
// DEFAULT_NFRT_VERSION = "2.0.31"），与 26.x 线在用版本同款（本机缓存实证）
val nfrtVersion = "2.0.31"
// 1.20.2/1.20.3 = Java 17；1.20.5 = Java 21（piston-meta javaVersion；
// MDG VersionCapabilitiesInternal.getJavaVersion 同判：1.18~1.20.4 段=17、1.20.5 起=21）
val javaLevel = if (stonecutter.eval(stonecutter.current.version, "<1.20.5")) 17 else 21

val neoForgeNotation = "net.neoforged:neoforge:$neoVersion:userdev"

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    // Mojang 游戏库（FML loader 传递依赖 com.mojang:logging 等仅在此仓库；
    // MDG RepositoriesPlugin 对 moddev 线同款注入）
    maven("https://libraries.minecraft.net") { name = "Mojang Libraries" }
    // MC 自身库聚合件（guava/netty/LWJGL 等）：MDG RepositoriesPlugin.java:46-53 同款
    maven("https://maven.neoforged.net/mojang-meta/") {
        name = "Mojang Meta"
        metadataSources { gradleMetadata() }
        content { includeModule("net.neoforged", "minecraft-dependencies") }
    }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
}

// MC 自身库 variant 属性（minecraftdependencies/MinecraftDistribution.java:13、
// OperatingSystem.java:12；mojang-meta .module 的 variant 以 Named 串发布，消费面用同名
// String 属性匹配；mdg 侧 client/linux disambiguation 默认值此处以显式声明替代）
val distAttr = Attribute.of("net.neoforged.distribution", String::class.java)
val osAttr = Attribute.of("net.neoforged.operatingsystem", String::class.java)
val usageAttr = Attribute.of("org.gradle.usage", String::class.java)
val categoryAttr = Attribute.of("org.gradle.category", String::class.java)

// ===== NFRT 本体（shadowed jar，classifier "all"；MDG 经 Bundling.SHADOWED attribute 解析同款）=====
val neoFormRuntimeTool by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
// userdev jar（内含 config.json=runs/libraries/modules 真相源）。
// 配置期 singleFile 解析：三线构建/tour 均 --no-configuration-cache（见头注）
val userdevConfig by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}
// ===== 游戏库桶：config.json libraries（FML/ModLauncher/ASM 等）+ MC 自身库聚合件 =====
// FML 库清单非传递（清单即 NeoForge 策展集；开传递会连带各 pom 的 slf4j 1.7.30/1.8-beta4
// 与 2.0.9 strict 冲突——首轮 compileJava 实证）；MC 库走 minecraft-dependencies 聚合件
// 自带的传递闭包。两者经 extendsFrom 进编译/运行 classpath（MDG modDevCompile/
// RuntimeDependencies 同构，ModDevArtifactsWorkflow.java:197-226）
val nfrtGameLibs by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}
// MC 自身库（version json 全集的 Gradle 聚合件，guava/netty/LWJGL 等在此）。
// variant 属性须在 configuration 层显式请求（dep 属性只作用单条边，首跑 tour 实证
// prepareRuns 解析时 natives×3 按 operatingsystem 歧义）：
//   库集=clientCompileDependencies（dist=client+usage=java-api+category=library；44 项与
//   clientRuntimeDependencies 同清单——version json 无 scope 区分；runtime 变体因缺 os 属性
//   无法与 natives 消歧，编译面变体是唯一可精确选中者）
//   本机库=clientLinuxNatives（追加 os=linux，精确选中，错误文案同口径）
val nfrtMcLibs by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(distAttr, "client")
        attribute(usageAttr, "java-api")
        attribute(categoryAttr, "library")
    }
}
val nfrtMcNatives by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(distAttr, "client")
        attribute(usageAttr, "java-runtime")
        attribute(categoryAttr, "library")
        attribute(osAttr, "linux")
    }
}
// dev 运行专属 jar（生产面由 implementation+jar 内嵌承担）：ImageStream 必须进 FML legacy
// classpath 才对游戏可见（MDG additionalRuntimeClasspath → legacyClasspath 同构）
val nfrtRunExtra by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
// boot module path（config.json modules：securejarhandler/ASM/bootstraplauncher 等，
// BootstrapLauncher -p 消费）
val nfrtModules by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

// MC version json 的 strictly 钉版可能低于 NeoForge 库清单版本（1.20.2 实证：MC slf4j-api
// {strictly 2.0.7} × 清单 2.0.9 → 解析死锁）。FML 清单=NeoForge dev/生产实际运行面，以它为准
// force（slf4j 2.x 向后兼容；eachDependency 压过 strictly）。版本号自清单推导，零硬编码。
// 位置约束：必须先于 userdevConfig 首次解析注册（解析后禁变），cfgLibraries 为
// 脚本成员属性，闭包执行期（后续配置解析时）才读取，此时已完成赋值
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.slf4j" && requested.name == "slf4j-api") {
            cfgLibraries.firstOrNull { it.startsWith("org.slf4j:slf4j-api:") }
                ?.let { useVersion(it.substringAfterLast(':')) }
        }
    }
}

// ===== config.json 配置期解包解析（三线各读各的 userdev，零硬编码）=====
// 注意次序：依赖声明必须先于 singleFile 解析，否则配置为空
dependencies {
    userdevConfig(neoForgeNotation)
}
val userdevJar = userdevConfig.singleFile
val userdevCfg: Map<*, *> = ZipFile(userdevJar).use { zip ->
    val entry = zip.getEntry("config.json")
        ?: throw GradleException("config.json missing in $userdevJar")
    groovy.json.JsonSlurper().parse(zip.getInputStream(entry)) as Map<*, *>
}
@Suppress("UNCHECKED_CAST")
val userdevRuns: Map<String, Map<String, *>> = userdevCfg["runs"] as Map<String, Map<String, *>>
// @zip 条目（neoform 数据包）非游戏库，不进 classpath；其余 @jar 记法剥后缀直用
val cfgLibraries: List<String> = (userdevCfg["libraries"] as List<String>)
    .filterNot { it.endsWith("@zip") }
    .map { it.removeSuffix("@jar") }
val cfgModules: List<String> = (userdevCfg["modules"] as List<String>)
    .map { it.removeSuffix("@jar") }

dependencies {
    neoFormRuntimeTool("net.neoforged:neoform-runtime:$nfrtVersion:all")

    cfgLibraries.forEach { nfrtGameLibs(it) }
    cfgModules.forEach { nfrtModules(it) }
    // MC 自身库（MDG gameLibrariesDependency 的 capability 变体在 POM-less 线缺席，
    // 此处直用其底层聚合件；variant 选择走上方 configuration 属性）
    "nfrtMcLibs"("net.neoforged:minecraft-dependencies:$mcVersion")
    "nfrtMcNatives"("net.neoforged:minecraft-dependencies:$mcVersion")

    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）：编译 + 生产内嵌 + dev 运行
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    add("nfrtRunExtra", "com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // MixinExtras：@WrapOperation 等注解编译面；运行时 NeoForge 20.2+ 自带模块
    //（20.2.93 userdev config.json ignoreList 含 mixinextras-neoforge-0.3.1.jar 实证）
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
}

configurations {
    // MC 库仅进编译面（compileClasspath 需自带 dist=client 请求——extendsFrom 只合并依赖
    // 不合并属性，variant 选择按消费配置自身属性判定）与 legacy classpath 文件（prepareRuns
    // 直读 nfrtMcLibs/nfrtMcNatives）；运行面 JVM classpath 不重复携带——游戏侧经
    // BootstrapLauncher legacyClassPath.file 消费
    compileClasspath {
        attributes { attribute(distAttr, "client") }
        extendsFrom(nfrtGameLibs, nfrtMcLibs)
    }
    runtimeClasspath {
        extendsFrom(nfrtGameLibs, nfrtRunExtra)
    }
}

// ===== NFRT 直驱任务：等价 MDG createMinecraftArtifacts + downloadAssets =====
val nfrtArtifactsDir = layout.buildDirectory.dir("moddev-artifacts")
// 命名=MDG ArtifactNamingStrategy.createNeoForge（同文件 ：21-32）：游戏 jar 必须名
// 为 neoforge-<v>.jar（FML ForgeUserdevLaunchHandler 按 neoforge- 前缀定位主 jar），
// 资源 jar 必须名 client-extra-<v>.jar（否则 FML 误认主 jar）
val gameJar = nfrtArtifactsDir.map { it.file("neoforge-$neoVersion.jar") }
// client-extra 资源 jar（FML 在 legacy classpath 找 client resources，
// ModDevRunWorkflow.java:235-240 "it has to contain client-extra to be loaded by FML"）
val clientExtra = nfrtArtifactsDir.map { it.file("client-extra-$neoVersion.jar") }
val assetsPropsFile = layout.buildDirectory.file("moddev/minecraft_assets.properties")
// toolchain 解析在脚本层（Project 扩展表）；the<T>() 在任务 lambda 内会解析到任务扩展表
val nfrtLauncher = the<JavaToolchainService>()
    .launcherFor { languageVersion = JavaLanguageVersion.of(21) }
val toolsJavaExecutable = the<JavaToolchainService>()
    .launcherFor { languageVersion = JavaLanguageVersion.of(javaLevel) }
    .map { it.executablePath.asFile.absolutePath }
    .get()
// 游戏运行 JVM 与编译同 toolchain（the<T>() 在任务 lambda 内会解析到任务扩展表，故脚本层预解析）
val runLauncher = the<JavaToolchainService>()
    .launcherFor { languageVersion = JavaLanguageVersion.of(javaLevel) }

val createMinecraftArtifacts by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Runs the NeoForm Runtime CLI directly (bypasses MDG capability resolution)."
    // NFRT 本体 Java 21（NeoFormRuntimeTask.java:114-117 convention）
    javaLauncher.set(nfrtLauncher)
    // 内存闸（主会话裁决 2026-09-19）：NFRT JVM 封顶 4G——本机与他仓常驻 runServer 并行，
    // 反编译重活不受控堆会挤压背景负载（gradle.properties 同因的个体化边界）
    maxHeapSize = "4g"
    // 主类显式 + classpath 只留 shadowed 单 jar：classifier 记法下 Gradle 仍会连带解析
    // 默认 variant 的传递依赖（picocli/ecj 实证），多文件 classpath 令 JavaExec 拒绝猜主类；
    // -all.jar 自带 Main-Class（MANIFEST.MF 实证）且自包含
    mainClass = "net.neoforged.neoform.runtime.cli.Main"
    classpath(neoFormRuntimeTool.filter { it.name.endsWith("-all.jar") })
    // 缓存/工作目录与 MDG 同款（NeoFormRuntimeTask.java:107-112）：缓存复用全局
    // ~/.gradle/caches/neoformruntime（26.x 线 intermediate_results 同仓），工作目录落本线 build/
    args(
        "--home-dir", gradle.gradleUserHomeDir.resolve("caches/neoformruntime").absolutePath,
        "--work-dir", layout.buildDirectory.dir("tmp/neoformruntime").get().asFile.absolutePath,
        "run",
        "--java-executable", toolsJavaExecutable,
        // notation 带 :userdev classifier（ModDevPlugin.java:63 原文）——POM-less 线
        // 无主 jar，无 classifier 时 NFRT 解析主 jar 必炸
        "--neoforge", neoForgeNotation,
        "--dist", "joined",
        // needsNeoForgeInMinecraftJar(≤1.21.11)=true → NeoForge 类并入产物 jar，
        // 请求 gameJarWithNeoForge（CreateMinecraftArtifacts.java:364-367 同款分支）
        "--write-result", "gameJarWithNeoForge:${gameJar.get().asFile.absolutePath}",
        // clientResources 同次请求（CreateMinecraftArtifacts.java:345-346；
        // needsNeoForge 线 MDG 恒请求，ModDevArtifactsWorkflow.java:164-166）
        "--write-result", "clientResources:${clientExtra.get().asFile.absolutePath}",
        "--problems-report", layout.buildDirectory.file("tmp/nfrt-problem-report.json").get().asFile.absolutePath,
    )
    outputs.file(gameJar)
    outputs.file(clientExtra)
}

val downloadAssets by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Downloads Minecraft assets via NFRT (DownloadAssets.java:96-113 same flags)."
    javaLauncher.set(nfrtLauncher)
    maxHeapSize = "1g"
    mainClass = "net.neoforged.neoform.runtime.cli.Main"
    classpath(neoFormRuntimeTool.filter { it.name.endsWith("-all.jar") })
    args(
        "--home-dir", gradle.gradleUserHomeDir.resolve("caches/neoformruntime").absolutePath,
        "download-assets",
        "--write-properties", assetsPropsFile.get().asFile.absolutePath,
        "--neoforge", neoForgeNotation,
    )
    outputs.file(assetsPropsFile)
}

// ===== 运行准备：legacy classpath 文件 + log4j2 配置（PrepareRun/WriteLegacyClasspath 同构）=====
val runArgsDir = layout.buildDirectory.dir("moddev/run")
val legacyClasspathFile = runArgsDir.map { it.file("legacyClasspath.txt") }
val log4j2ConfigFile = runArgsDir.map { it.file("log4j2.xml") }

val prepareRuns by tasks.registering {
    group = "build"
    description = "Writes legacyClasspath.txt and log4j2.xml for NFRT dev runs."
    dependsOn(createMinecraftArtifacts)
    inputs.files(nfrtGameLibs)
    inputs.files(nfrtMcLibs)
    inputs.files(nfrtMcNatives)
    inputs.files(nfrtRunExtra)
    outputs.file(legacyClasspathFile)
    outputs.file(log4j2ConfigFile)
    doLast {
        // WriteLegacyClasspath.java:38-57 同款：jar/zip/dir 过滤 + 定序
        val entries = (nfrtGameLibs.files + nfrtMcLibs.files + nfrtMcNatives.files + nfrtRunExtra.files +
                files(gameJar).files + files(clientExtra).files)
            .filter { it.isDirectory || it.name.endsWith(".jar") || it.name.endsWith(".zip") }
            .map { it.absolutePath }
            .toSortedSet()
        legacyClasspathFile.get().asFile.parentFile.mkdirs()
        legacyClasspathFile.get().asFile.writeText(
            entries.joinToString(System.lineSeparator(), postfix = System.lineSeparator())
        )
        // RunUtils.writeLog4j2Configuration（Level INFO）同模板
        log4j2ConfigFile.get().asFile.parentFile.mkdirs()
        log4j2ConfigFile.get().asFile.writeText(nfrtLog4j2Template.trim())
    }
}

// 配置期显式解析为 File（moddev imageStreamEmbedJars 同款注记）
val nfrtRunExtraJars: Set<File> = nfrtRunExtra.files

// ===== 源集：stonecutter 生成树（默认挂载）+ neoforge 平台/分代树 + shim =====
// 分代沿用 build.moddev.gradle.kts 既有边界（先 diff 后复制，平铺不新造边界）：
//   1.20.2/1.20.3 落 <1.20.5 分支（=1.20.4 同代：1204 树 + pre1213 树）；
//   1.20.5 落 <1.21 分支（=1.20.6 同代：1205+1206 树 + pre1213 树）；
//   shim <1.21 挂 1.16.5 版本中立副本（moddev :309-310 同源）。
// exclude 清单=moddev :364-373 原样（第三方触点闸门 + platform/forge 树整体排除）。
sourceSets.main {
    java {
        srcDir(rootProject.file("src/neoforge/java"))
        if (stonecutter.eval(stonecutter.current.version, "<1.20.5")) {
            srcDir(rootProject.file("src/neoforge-1204/java"))
        } else {
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1206/java"))
        }
        srcDir(rootProject.file("src/neoforge-pre1213/java"))
        // 1.20.2/1.20.3 代差（NFRT 产物 20.2.93 jar 实证）：capabilities 走
        // common.capabilities（Forge 血统 CapabilityManager/CapabilityToken）、网络走
        // SimpleChannel/NetworkRegistry（20.4 的 registration/handling 包缺席）→ 1202 代树
        // = forge 1.20.1 线同构实现的 neoforge 包名移植（异包孪生 platform.neoforge1202，
        // 212 树先例：exclude 按相对路径双杀同名 RAW 文件，孪生必须异包）。
        // 主类/能力 provider/网络 impl/ForgeCapabilityHooks 四域换代，事件 hook 等其余
        // neoforge 面 20.2/20.4 同形（r4 编译实证）继续挂 1204 树。
        if (stonecutter.eval(stonecutter.current.version, "<1.20.3")) {
            exclude(
                // 基础树 20.4 重铸面（EntityCapability/注册矩阵）→ 1202 孪生替代
                "com/elfmcys/yesstevemodel/platform/neoforge/YesSteveModelForge.java",
                "com/elfmcys/yesstevemodel/platform/neoforge/capability/**",
                // 1204 树的 payload 网络与 20.4 查询语义 hooks → 1202 孪生替代
                "com/elfmcys/yesstevemodel/platform/neoforge/network/YSMChannelImpl.java",
                "com/elfmcys/yesstevemodel/platform/neoforge/network/PacketContextImpl.java",
                "com/elfmcys/yesstevemodel/platform/neoforge/ForgeCapabilityHooks.java",
            )
            srcDir(rootProject.file("src/neoforge-1202/java"))
        }
        // shim：<1.21 挂原件（moddev :309-310 同款）
        srcDir(rootProject.file("versions/1.16.5-forge/src/shim/rip/ysm/compat"))
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

// 编译面：NFRT 产物（MC+NeoForge，mojmap 直名）已含全部平台类（needsNeoForgeInMinecraftJar
// 线无独立 neoforge universal jar），游戏库经 extendsFrom 进 compileClasspath（见上）
dependencies {
    compileOnly(files(gameJar))
}

java {
    // MC 1.20.2/1.20.3=Java 17、1.20.5=Java 21（piston-meta javaVersion）
    toolchain { languageVersion = JavaLanguageVersion.of(javaLevel) }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaLevel
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(createMinecraftArtifacts)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("stonecutterGenerate")
    // forge 口径 mods.toml 与第三方 accessor 配置不进 neoforge 产物（moddev :381-385 同款）
    exclude("META-INF/mods.toml", "yes_steve_model_forge.mixins.json",
        "**/fabric.mod.json", "**/*.accesswidener")
    // ===== per-line 资源口径（moddev :554-707 的 1.20.x 子集，模板基线值=1.20.6）=====
    val props = mapOf(
        "mod_id" to (project.property("archives_name") as String),
        "mod_name" to (project.property("mod_name") as String),
        "mod_version" to (project.property("mod_version") as String),
        "mod_license" to (project.property("mod_license") as String),
    )
    // 1.20.2/1.20.3：neoforge.mods.toml 模板 rename 回 META-INF/mods.toml（20.5 起才改名；
    // moddev :565-570 同款，docs version-1.20.4 modfiles.md）
    val pre1205 = stonecutter.eval(stonecutter.current.version, "<1.20.5")
    if (pre1205) {
        rename { fileName ->
            if (fileName == "neoforge.mods.toml") "mods.toml" else fileName
        }
    }
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    filesMatching("META-INF/mods.toml") { expand(props) }
    // 装载区间：neoforge 用线大版本（20.2.93→20.2）、minecraft 用游戏真实版本串（1.20.x 本就全名）
    val neoMajor = neoVersion.substringBeforeLast('.')
    filesMatching(listOf("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        filter { line: String ->
            var out = line.replace("versionRange = \"[20.6,)\"", "versionRange = \"[$neoMajor,)\"")
                .replace("versionRange = \"[1.20.6,)\"", "versionRange = \"[$mcVersion,)\"")
            // FML 1.0.x（1.20.2/1.20.3）依赖表格式=mandatory 布尔字段（loader-1.0.16
            // ModInfo$ModVersion "Missing required field mandatory" 实证）；20.5+ FML 2.0
            // 才是 type = "required"——模板基线为 FML 2.0 口径，本段按线换写
            if (pre1205) {
                out = out.replace("type = \"required\"", "mandatory = true")
            }
            out
        }
    }
    // pack.mcmeta（minecraft.wiki Pack format 表，批二 c-2 2026-09-14 实拉快照，
    // 与 moddev processResources 同映射）：1.20.2=18 / 1.20.3=22 / 1.20.5=32
    val packFormat = mapOf("1.20.2" to 18, "1.20.3" to 22, "1.20.5" to 32)[mcVersion] ?: 15
    filesMatching("pack.mcmeta") {
        filter { line: String ->
            line.replace("\"pack_format\": 15", "\"pack_format\": $packFormat")
        }
    }
    // mixins.json：1.20.5 产物 Java 21 字节码 → JAVA_21；Arrow.effects 字段 1.20.5 数据
    // 组件化删除 → ArrowPotionAccessor（src/neoforge-1205 .../mixin/client，moddev :646-673 同款）。
    // 1.20.2/1.20.3（pre1205）零替换（JAVA_17/ArrowEntityAccessor/BufferBuilderMixin 全保留）
    if (!pre1205) {
        filesMatching("*.mixins.json") {
            filter { line: String ->
                line.replace("\"JAVA_17\"", "\"JAVA_21\"")
                    .replace("\"client.ArrowEntityAccessor\"", "\"client.ArrowPotionAccessor\"")
            }
        }
    }
}

tasks {
    register<Copy>("buildAndCollect") {
        group = "build"
        // mojmap 直配线无 reobf（NFRT 产物即发布字节码名），jar 产物即发布件
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }

    jar {
        // 跨版本测试 harness 只进 dev run classpath，不进生产产物（moddev :516-518 同款）
        exclude("rip/ysm/harness/**")
        // ImageStream 生产内嵌：类文件 + javax.imageio SPI services 并入主 jar（剥 manifest/签名）
        from(nfrtRunExtraJars.map { zipTree(it) }) {
            include("rip/**", "META-INF/services/**")
        }
        // 生产环境 Mixin 配置发现 belt-and-braces（moddev :523-527 同款；
        // dev/生产主通道=mods.toml [[mixins]] 表，FML 1.0.16 ModFileParser 实证支持）
        manifest {
            attributes("MixinConfigs" to "yes_steve_model.mixins.json")
        }
    }
}

// RunUtils.writeLog4j2Configuration（Level INFO）模板原文
val nfrtLog4j2Template = """
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn" shutdownHook="disable">
    <filters>
        <ThresholdFilter level="WARN" onMatch="ACCEPT" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="NETWORK_PACKETS" onMatch="${'$'}{sys:forge.logging.marker.networking:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="CLASSLOADING" onMatch="${'$'}{sys:forge.logging.marker.classloading:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="LAUNCHPLUGIN" onMatch="${'$'}{sys:forge.logging.marker.launchplugin:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="CLASSDUMP" onMatch="${'$'}{sys:forge.logging.marker.classdump:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="AXFORM" onMatch="${'$'}{sys:forge.logging.marker.axform:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="EVENTBUS" onMatch="${'$'}{sys:forge.logging.marker.eventbus:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="DISTXFORM" onMatch="${'$'}{sys:forge.logging.marker.distxform:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="SCAN" onMatch="${'$'}{sys:forge.logging.marker.scan:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="REGISTRIES" onMatch="${'$'}{sys:forge.logging.marker.registries:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="REGISTRYDUMP" onMatch="${'$'}{sys:forge.logging.marker.registrydump:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="SPLASH" onMatch="${'$'}{sys:forge.logging.marker.splash:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="RESOURCE-CACHE" onMatch="${'$'}{sys:forge.logging.marker.resource.cache:-DENY}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="FORGEMOD" onMatch="${'$'}{sys:forge.logging.marker.forgemod:-NEUTRAL}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="LOADING" onMatch="${'$'}{sys:forge.logging.marker.loading:-NEUTRAL}" onMismatch="NEUTRAL"/>
        <MarkerFilter marker="CORE" onMatch="${'$'}{sys:forge.logging.marker.core:-NEUTRAL}" onMismatch="NEUTRAL"/>
    </filters>
    <Appenders>
        <Console name="Console">
            <PatternLayout>
                <LoggerNamePatternSelector defaultPattern="%highlightForge{[%d{HH:mm:ss}] [%t/%level] [%c{2.}/%markerSimpleName]: %minecraftFormatting{%msg{nolookup}}%n%tEx}">
                    <PatternMatch key="net.minecraft." pattern="%highlightForge{[%d{HH:mm:ss}] [%t/%level] [minecraft/%logger{1}]: %minecraftFormatting{%msg{nolookup}}%n%tEx}"/>
                    <PatternMatch key="com.mojang." pattern="%highlightForge{[%d{HH:mm:ss}] [%t/%level] [mojang/%logger{1}]: %minecraftFormatting{%msg{nolookup}}%n%tEx}"/>
                </LoggerNamePatternSelector>
            </PatternLayout>
        </Console>
        <Queue name="ServerGuiConsole" ignoreExceptions="true">
            <PatternLayout>
                <LoggerNamePatternSelector defaultPattern="[%d{HH:mm:ss}] [%t/%level] [%c{2.}/%markerSimpleName]: %minecraftFormatting{%msg{nolookup}}{strip}%n">
                    <PatternMatch key="net.minecraft." pattern="[%d{HH:mm:ss}] [%t/%level] [minecraft/%logger{1}]: %minecraftFormatting{%msg{nolookup}}{strip}%n"/>
                    <PatternMatch key="com.mojang." pattern="[%d{HH:mm:ss}] [%t/%level] [mojang/%logger{1}]: %minecraftFormatting{%msg{nolookup}}{strip}%n"/>
                </LoggerNamePatternSelector>
            </PatternLayout>
        </Queue>
        <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="[%d{ddMMMyyyy HH:mm:ss.SSS}] [%t/%level] [%logger/%markerSimpleName]: %minecraftFormatting{%msg{nolookup}}{strip}%n%xEx"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <OnStartupTriggeringPolicy/>
            </Policies>
            <DefaultRolloverStrategy max="99" fileIndex="min"/>
        </RollingRandomAccessFile>
        <RollingRandomAccessFile name="DebugFile" fileName="logs/debug.log" filePattern="logs/debug-%i.log.gz">
            <PatternLayout pattern="[%d{ddMMMyyyy HH:mm:ss.SSS}] [%t/%level] [%logger/%markerSimpleName]: %minecraftFormatting{%msg{nolookup}}{strip}%n%xEx"/>
            <Policies>
                <OnStartupTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="200MB"/>
            </Policies>
            <DefaultRolloverStrategy max="5" fileIndex="min"/>
        </RollingRandomAccessFile>
    </Appenders>
    <Loggers>
        <Logger level="${'$'}{sys:forge.logging.mojang.level:-info}" name="com.mojang"/>
        <Logger level="${'$'}{sys:forge.logging.mojang.level:-info}" name="net.minecraft"/>
        <Logger level="${'$'}{sys:forge.logging.classtransformer.level:-info}" name="cpw.mods.modlauncher.ClassTransformer"/>
        <Logger name="io.netty.util.internal.PlatformDependent0">
            <filters>
                <RegexFilter regex="^direct buffer constructor: unavailable${'$'}" onMatch="DENY" onMismatch="NEUTRAL" />
                <RegexFilter regex="^jdk\.internal\.misc\.Unsafe\.allocateUninitializedArray\(int\): unavailable${'$'}" onMatch="DENY" onMismatch="NEUTRAL" />
            </filters>
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="Console" />
            <AppenderRef ref="ServerGuiConsole" level="${'$'}{sys:forge.logging.console.level:-info}"/>
            <AppenderRef ref="File" level="${'$'}{sys:forge.logging.file.level:-info}"/>
            <AppenderRef ref="DebugFile" />
        </Root>
    </Loggers>
</Configuration>
"""

// ===== dev 运行面：userdev config.json runs.{client,server} 直驱 =====
// 等价 MDG RunGameTask+PrepareRun（legacyClasspath 代，modLocatorRework=false）：
//   - main=BootstrapLauncher、jvmArgs（-p {modules}、--add-opens/exports）、props -D 全量
//   - MOD_CLASSES=modid%%<输出目录>（RunUtils ModFoldersProvider :270-299 格式）
//   - args 的 {assets_root}/{asset_index} 由 NFRT download-assets 的 properties 插值
val modClassesArg: String = sourceSets.main.get().output.classesDirs.asSequence()
    .plus(sequenceOf(sourceSets.main.get().output.resourcesDir))
    .filterNotNull()
    .joinToString(File.pathSeparator) { "${modId}%%${it.absolutePath}" }

listOf(Triple("client", "runClient", "run/client"), Triple("server", "runServer", "run/server"))
    .forEach { (runType, taskName, gameDirPath) ->
    val runCfg = userdevRuns[runType]
        ?: throw GradleException("userdev config.json has no '$runType' run (line $mcVersion)")
    tasks.register<JavaExec>(taskName) {
        group = "build"
        description = "Runs the NFRT dev $runType (BootstrapLauncher via userdev config.json)."
        dependsOn("classes", "processResources", prepareRuns, downloadAssets)
        inputs.files(nfrtModules)
        javaLauncher.set(runLauncher)
        mainClass.set(runCfg["main"] as String)
        workingDir(layout.projectDirectory.dir(gameDirPath))
        doFirst { workingDir.mkdirs() }
        classpath(sourceSets.main.get().runtimeClasspath, files(gameJar), files(clientExtra))
        environment("MOD_CLASSES", modClassesArg)
        // 插值物全部留到执行期求值（assets/legacy 文件此时才存在）
        val runProps = runCfg["props"] as Map<String, String>
        val runJvmArgs = runCfg["jvmArgs"] as List<String>
        jvmArgumentProviders.add {
            val lcp = legacyClasspathFile.get().asFile.absolutePath
            buildList {
                for (arg in runJvmArgs) {
                    add(
                        if (arg == "{modules}") nfrtModules.files.joinToString(File.pathSeparator) { it.absolutePath }
                        else arg
                    )
                }
                add("-Dlog4j2.configurationFile=${log4j2ConfigFile.get().asFile.absolutePath}")
                for ((k, v) in runProps) {
                    add("-D$k=${if (v == "{minecraft_classpath_file}") lcp else v}")
                }
            }
        }
        val runArgs = runCfg["args"] as List<String>
        argumentProviders.add {
            val props = Properties()
            val f = assetsPropsFile.get().asFile
            if (f.isFile) f.inputStream().use { props.load(it) }
            runArgs.map { arg ->
                when (arg) {
                    "{assets_root}" -> props.getProperty("assets_root") ?: arg
                    "{asset_index}" -> props.getProperty("asset_index") ?: arg
                    else -> arg
                }
            }
        }
        // runServer 控制台 stdin（harness/tour.sh 和平启动注入通道，forge/moddev 线同款）
        if (runType == "server") {
            standardInput = System.`in`
        }
    }
}
