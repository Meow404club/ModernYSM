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

        vcsVersion = "1.20.1-forge"
    }
}
