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
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
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

// stub 阶段不挂共享源：stonecutter 默认会把共享 src/main 展开树塞进 main 源集
//（实测 NativeLibLoader/YesSteveModel 等 modern 面符号 1.7.10 全不在，首轮 100 错截断）。
// L0 只编版本 stub（零 //? 条件，无需展开树）；共享源分代白名单挂载是 L1+ 的事
//（build.legacy122.gradle.kts legacy122Include 先例）。
afterEvaluate {
    sourceSets.main {
        java.setSrcDirs(listOf(file("src/main/java")))
        resources.setSrcDirs(listOf(file("src/main/resources")))
    }
}

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
