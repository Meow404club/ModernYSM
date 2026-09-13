pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        // unimined（xyz.wagyourtail.unimined）插件仓库：<1.17 forge 版本路由构建脚本所需
        maven("https://maven.wagyourtail.xyz/releases") { name = "WagYourTail" }
        // 1.3.16-SNAPSHOT 退路（unimined 1.4.1 解析/DSL 失败时 Celeritas 同款回退）
        maven("https://maven.wagyourtail.xyz/snapshots") { name = "WagYourTail Snapshots" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.7"
}

rootProject.name = "openysm"

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { vers("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        // M3 平铺第一批：forge 中段四线同走 legacyforge（MDG 官方支持域 1.17~1.20.1，
        // tmp/harvest/m3-matrix-mdg/LEGACY.md:3）；forge 版本号取 maven promotions：
        // 1.17.1=37.1.1（该线唯一构建）/1.18.2=40.3.0、1.19.2=43.5.0、1.19.4=45.4.0（recommended）
        vers("1.17.1-forge", "1.17.1").buildscript = "build.forge.gradle.kts"
        vers("1.18.2-forge", "1.18.2").buildscript = "build.forge.gradle.kts"
        vers("1.19.2-forge", "1.19.2").buildscript = "build.forge.gradle.kts"
        vers("1.19.4-forge", "1.19.4").buildscript = "build.forge.gradle.kts"
        // <1.17 的 forge 走 unimined 线（MDG/NFRT 拒绝 pre-1.17，实证 tmp/poc-1165/RUN-REPORT.md 实测 4）；
        // Celeritas 生产先例：forge <1.17 → unimined，>=1.17 → legacyforge
        vers("1.16.5-forge", "1.16.5").buildscript = "build.unimined.gradle.kts"

        // M3 批二 a：neoforge 三线走 moddev（MDG 2 主插件，1.20.4/1.20.6 由 MDG2 公告点名支持，
        // 1.21 起全支持，tmp/harvest/m3-matrix-mdg/）；neoforge 版本号取 maven metadata 最新 stable
        //（tmp/harvest/m3-matrix/maven-metadata.xml：20.4 线 beta 止于 20.4.99-beta，ga 止于
        // 20.4.251；20.6 线 beta 止于 20.6.114-beta，ga 止于 20.6.141；21.1 线全 ga 至 21.1.250）
        vers("1.20.4-neoforge", "1.20.4").buildscript = "build.moddev.gradle.kts"
        vers("1.20.6-neoforge", "1.20.6").buildscript = "build.moddev.gradle.kts"
        vers("1.21.1-neoforge", "1.21.1").buildscript = "build.moddev.gradle.kts"

        // M3 批二 b：neoforge 后八线（21.3/21.4/21.5/21.8/21.10/21.11 Java 21 +
        // 26.1.2/26.2 Java 25）。版本号取 maven metadata 该线最新 stable（2026-09-13 实拉
        // https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml，
        // 与 tmp/harvest/m3-matrix/maven-metadata.xml 2026-09-11 快照互证，仅 26.2 线由
        // .86 → .87 前进一版）：
        //   21.3.97 / 21.4.157 / 21.5.98 / 21.8.54 / 21.10.64 / 21.11.45
        //   26.1.2.109 / 26.2.0.87
        // 21.2/21.6/21.7/21.9 无 stable 线不注册（tasks.m3-matrix-research 跳过依据）。
        // Java 分段依据 piston-meta version_manifest_v2（2026-09-13 实拉）：21.3~21.11
        // javaVersion.majorVersion=21、26.1/26.2=25；26.x 版本号去 1.x 前缀后 stonecutter
        // 版本轴直接用 "26.1.2"/"26.2"（数值段比较 ">=1.21.x" 恒 true、"<1.21" 恒 false）。
        vers("21.3-neoforge", "21.3").buildscript = "build.moddev.gradle.kts"
        vers("21.4-neoforge", "21.4").buildscript = "build.moddev.gradle.kts"
        vers("21.5-neoforge", "21.5").buildscript = "build.moddev.gradle.kts"
        vers("21.8-neoforge", "21.8").buildscript = "build.moddev.gradle.kts"
        vers("21.10-neoforge", "21.10").buildscript = "build.moddev.gradle.kts"
        vers("21.11-neoforge", "21.11").buildscript = "build.moddev.gradle.kts"
        vers("26.1.2-neoforge", "26.1.2").buildscript = "build.moddev.gradle.kts"
        vers("26.2-neoforge", "26.2").buildscript = "build.moddev.gradle.kts"

        vcsVersion = "1.20.1-forge"
    }
}
