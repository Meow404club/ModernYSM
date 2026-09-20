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
dependencies {
    implementation("org.taumc:lwjgl3ify:${lwjgl3ifyVersion}:dev") { isTransitive = false }
    runtimeOnly("org.taumc:lwjgl3ify:${lwjgl3ifyVersion}:forgePatches") { isTransitive = false }
    // mixinbooter（先例 :114；stub 阶段只进 runtime 面，L1 写 mixin 类时再挂配置）
    implementation("zone.rong:mixinbooter:10.5")
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

// ===== 源集换根隔离（nfrt-poc-1202 既有裁决同款机制）=====
// stonecutter 默认把共享生成树（rootProject src/main 的条件展开）挂进版本子项目源集；
// 共享源全量面向 >=1.20.1 API，1.12.2 线编译必负（实测 100 错），且 L0 卡边界明令不碰
// 共享源（分代条件块=L1 的事）。本 afterEvaluate 注册序后于 stonecutter 的 afterEvaluate
// → setSrcDirs 换根把共享生成树挤出源集，只挂本线 stub（java+resources 双杀）。
// ponytail: L1 渲染翻译层落地时把分代树挂回 srcDirs，本块即撤
afterEvaluate {
    sourceSets.main {
        java.setSrcDirs(listOf("src/main/java"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}

tasks.processResources.configure {
    filesMatching("mcmod.info") {
        expand(mapOf("version" to version))
    }
}
