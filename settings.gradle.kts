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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.kikugie.stonecutter") version "0.7"
}

rootProject.name = "openysm"

stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders
            .forEach { vers("$version-$it", version).buildscript = "build.$it.gradle.kts" }

        match("1.20.1", "forge")
        // <1.17 的 forge 走 unimined 线（MDG/NFRT 拒绝 pre-1.17，实证 tmp/poc-1165/RUN-REPORT.md 实测 4）；
        // Celeritas 生产先例：forge <1.17 → unimined，>=1.17 → legacyforge
        vers("1.16.5-forge", "1.16.5").buildscript = "build.unimined.gradle.kts"

        vcsVersion = "1.20.1-forge"
    }
}
