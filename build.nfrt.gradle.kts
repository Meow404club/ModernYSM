// ===== NFRT 直驱 POC 线构建脚本（nfrt-poc-1202，仅 1.20.2-neoforge 线挂载）=====
// 背景：1.20.2 线官方 maven 无 Gradle .module 元数据，MDG requireCapability
// ("net.neoforged:neoforge-moddev-bundle") 解析必失败（tasks.m3-nfrt-direct-drive-poc 判负）。
// 本脚本绕过 MDG，用 JavaExec 直调 NeoForm Runtime（NFRT）CLI 产出可编译工件。
// 每个旗标出处（逐字对齐，禁凭记忆）：MDG 2.0.141 源码
// tmp/harvest/m3-asm-mdg-src/src/main/java/net/neoforged/nfrtgradle/：
//   - CreateMinecraftArtifacts.java:266-394（run 子命令旗标序列：--neoforge/--dist joined/
//     --write-result <id>:<path>/--problems-report），
//   - NeoFormRuntimeTask.java:131-172（--home-dir/--work-dir 前置；NFRT 本体跑 Java 21），
//   - ModDevArtifactsWorkflow.java:119-171（toolsJavaExecutable=MC 对应 toolchain，1.20.2=Java 17；
//     needsNeoForgeInMinecraftJar 对 <=1.21.11 为 true → 请求 gameJarWithNeoForge）。
// NFRT CLI 面真相源（本机 jar 实证，2026-09-19）：
//   ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoform-runtime/2.0.31/
//   neoform-runtime-2.0.31-all.jar 的 `run --help` 输出 +
//   javap ResultIds 常量（gameJarWithNeoForge 等全数在册）。
// compileJava classpath：NFRT 产物 jar（MC+NeoForge 补丁类，mojmap 直名）+
//   net.neoforged.fancymodloader:loader:1.0.16（@Mod 注解所在 jar；
//   neoforge-20.2.93-userdev.jar!config.json libraries 清单原文；NFRT 产物 jar 只含
//   net/neoforged/neoforge/**，FML 为独立库——1.21.1 线 NFRT 产物 jar 目录清单同构实证）。

plugins {
    `java-library`
}

version = "${property("mod_version")}-${property("deps.minecraft")}-neoforge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

val neoVersion = property("deps.neoforge") as String
// NFRT 版本 = MDG 2.0.141/2.0.147 默认（NeoFormRuntimeExtension.java:15
// DEFAULT_NFRT_VERSION = "2.0.31"），与 26.x 线在用版本同款（本机缓存实证）
val nfrtVersion = "2.0.31"

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
}

// NFRT 本体（shadowed jar，classifier "all"；MDG 经 Bundling.SHADOWED attribute 解析同款文件）
val neoFormRuntimeTool by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    neoFormRuntimeTool("net.neoforged:neoform-runtime:$nfrtVersion:all")
}

// ===== NFRT 直驱任务：等价 MDG createMinecraftArtifacts（无 mod 侧 AT/parchment 输入，POC 不需要）=====
val nfrtArtifactsDir = layout.buildDirectory.dir("moddev-artifacts")
val gameJar = nfrtArtifactsDir.map { it.file("minecraft-patched-$neoVersion.jar") }

val createMinecraftArtifacts by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Runs the NeoForm Runtime CLI directly (bypasses MDG capability resolution)."
    // NFRT 本体 Java 21（NeoFormRuntimeTask.java:114-117 convention）
    javaLauncher.set(the<JavaToolchainService>().launcherFor { languageVersion = JavaLanguageVersion.of(21) })
    // 内存闸（主会话裁决 2026-09-19）：NFRT JVM 封顶 4G——本机与他仓常驻 runServer 并行，
    // 反编译重活不受控堆会挤压背景负载（gradle.properties 同因的个体化边界）
    maxHeapSize = "4g"
    classpath(neoFormRuntimeTool)
    // 缓存/工作目录与 MDG 同款（NeoFormRuntimeTask.java:107-112）：缓存复用全局
    // ~/.gradle/caches/neoformruntime（26.x 线 intermediate_results 同仓），工作目录落本线 build/
    args(
        "--home-dir", gradle.gradleUserHomeDir.resolve("caches/neoformruntime").absolutePath,
        "--work-dir", layout.buildDirectory.dir("tmp/neoformruntime").get().asFile.absolutePath,
        "run",
        // MC 1.20.2 外部工具 toolchain = Java 17（MDG VersionCapabilitiesInternal.getJavaVersion：
        // 1.18~1.20.4 段=17；launcher 经 foojay 解析，本机 java-17 在册）
        "--java-executable", the<JavaToolchainService>()
            .launcherFor { languageVersion = JavaLanguageVersion.of(17) }
            .map { it.executablePath.asFile.absolutePath }
            .get(),
        "--neoforge", "net.neoforged:neoforge:$neoVersion",
        "--dist", "joined",
        // needsNeoForgeInMinecraftJar(<=1.21.11)=true → NeoForge 类并入产物 jar，
        // 请求 gameJarWithNeoForge（CreateMinecraftArtifacts.java:364-367 同款分支）
        "--write-result", "gameJarWithNeoForge:${gameJar.get().asFile.absolutePath}",
        "--problems-report", layout.buildDirectory.file("tmp/nfrt-problem-report.json").get().asFile.absolutePath,
    )
    outputs.file(gameJar)
}

// ===== 源集：POC stub（version 本地源，绕开共享生成树）=====
// stonecutter 在其 afterEvaluate（注册序先于本脚本的 afterEvaluate）把根 src/main 的
// 生成树（java+resources）追加进 main 源集（1.20.1 线 build/generated 实证）；本卡只证
// "本项目类路径下能对 NFRT 工件编译+打包"，mod 平铺是 GO 后另立卡——故在自身
// afterEvaluate（后注册→后执行）里整体 setSrcDirs 换根，把生成树从源集挤出去。
val pocStubDir = file("src/main/java")
val pocResourcesDir = file("src/main/resources")

afterEvaluate {
    sourceSets.main {
        java.setSrcDirs(listOf(pocStubDir))
        resources.setSrcDirs(listOf(pocResourcesDir))
    }
}

// 编译面：NFRT 产物（MC+NeoForge，mojmap 直名）+ FML loader（@Mod）
dependencies {
    compileOnly(files(gameJar))
    // 版本取 neoforge-20.2.93-userdev.jar!config.json libraries 原文
    compileOnly("net.neoforged.fancymodloader:loader:1.0.16")
}

java {
    // MC 1.20.2 = Java 17（piston-meta javaVersion=17，MDG 同判）
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(createMinecraftArtifacts)
    options.release = 17
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named<Jar>("jar").map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
    dependsOn("build")
}
