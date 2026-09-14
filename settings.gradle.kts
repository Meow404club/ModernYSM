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
        // M3 批二 c-1：forge beta/latest 六线（同走 legacyforge；官方支持域 1.17~1.20.1 零越界）。
        // 版本号取 maven metadata（tmp/harvest/m3-matrix-forge/maven-metadata.xml 2026-08-27 快照，
        // 证据行号 state:tasks.m3-beta-lines-enumeration）：
        //   1.18=38.0.17(latest-only,xml:871) / 1.18.1=39.1.2(rec,xml:1044) /
        //   1.19=41.1.0(rec=latest,xml:521) / 1.19.1=42.0.9(latest-only,xml:863) /
        //   1.19.3=44.1.23(rec,xml:707) / 1.20=46.0.14(latest-only,xml:212)。Java 17 全部。
        vers("1.18-forge", "1.18").buildscript = "build.forge.gradle.kts"
        vers("1.18.1-forge", "1.18.1").buildscript = "build.forge.gradle.kts"
        vers("1.19-forge", "1.19").buildscript = "build.forge.gradle.kts"
        vers("1.19.1-forge", "1.19.1").buildscript = "build.forge.gradle.kts"
        vers("1.19.3-forge", "1.19.3").buildscript = "build.forge.gradle.kts"
        vers("1.20-forge", "1.20").buildscript = "build.forge.gradle.kts"
        // <1.17 的 forge 走 unimined 线（MDG/NFRT 拒绝 pre-1.17，实证 tmp/poc-1165/RUN-REPORT.md 实测 4）；
        // Celeritas 生产先例：forge <1.17 → unimined，>=1.17 → legacyforge。
        // M3 批二 c-1：1.16.1~1.16.4 四线同款铺入（用户裁量 2026-09-13，构建数 92/46/58/55，
        // latest=32.0.108(xml:1530)/33.0.61(1484)/34.1.42(1426)/35.1.37(1371)，1.16.3/4 有 rec），
        // unimined + Java 8 口径与 1.16.5 完全一致
        vers("1.16.5-forge", "1.16.5").buildscript = "build.unimined.gradle.kts"
        vers("1.16.1-forge", "1.16.1").buildscript = "build.unimined.gradle.kts"
        vers("1.16.2-forge", "1.16.2").buildscript = "build.unimined.gradle.kts"
        vers("1.16.3-forge", "1.16.3").buildscript = "build.unimined.gradle.kts"
        vers("1.16.4-forge", "1.16.4").buildscript = "build.unimined.gradle.kts"

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

        // M3 批二 c-2：neoforge beta/ga 补线八线（1.20.2/1.20.3 Java 17 + 1.20.5~21.9 Java 21）。
        // 版本号取官方 maven metadata 冻结快照（tmp/harvest/m3-matrix/maven-metadata.xml，
        // 任务卡给定 tile）：1.20.2=20.2.93(ga) / 1.20.3=20.3.8-beta / 1.20.5=20.5.21-beta /
        // 1.21=21.0.167(ga) / 21.2=21.2.1-beta / 21.6=21.6.20-beta / 21.7=21.7.25-beta /
        // 21.9=21.9.16-beta。版本 ID 分段沿现役约定：<1.21 用全称（"1.21" 落 <1.21.2 分支
        // =1.21.1 同形），21.x 缩写（21.2 落 <21.5=21.3 同形、21.6/21.7 落 <21.8=21.5 同形、
        // 21.9 落 <21.10=21.8 同形）。
        // ⚠ POC 注记：20.2.93/20.3.8-beta/20.5.21-beta 无 Gradle .module 元数据
        //（maven.neoforged.net 目录 404 实证 2026-09-14），MDG capability 解析是否可行
        // 见任务卡 POC 判定；判负线注册随判负报告处置。
        vers("1.20.2-neoforge", "1.20.2").buildscript = "build.moddev.gradle.kts"
        vers("1.20.3-neoforge", "1.20.3").buildscript = "build.moddev.gradle.kts"
        vers("1.20.5-neoforge", "1.20.5").buildscript = "build.moddev.gradle.kts"
        vers("1.21-neoforge", "1.21").buildscript = "build.moddev.gradle.kts"
        vers("21.2-neoforge", "21.2").buildscript = "build.moddev.gradle.kts"
        vers("21.6-neoforge", "21.6").buildscript = "build.moddev.gradle.kts"
        vers("21.7-neoforge", "21.7").buildscript = "build.moddev.gradle.kts"
        vers("21.9-neoforge", "21.9").buildscript = "build.moddev.gradle.kts"

        vcsVersion = "1.20.1-forge"
    }
}
