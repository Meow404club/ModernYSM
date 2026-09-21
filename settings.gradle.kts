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
        // RetroFuturaGradle 插件 marker（com.gtnewhorizons.retrofuturagradle）只在 GTNH nexus
        //（gradlePluginPortal 404 实证 2026-09-20；1.12.2 线构建脚本所需）
        maven("https://nexus.gtnewhorizons.com/repository/public/") { name = "GTNH" }
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

        // M3 26.x 适配：26.1/26.1.1 两线注册（Java 25，piston-meta version_manifest_v2
        // 2026-09-15 实拉：26.1/26.1.1/26.1.2/26.2 均 release，java_version=25）。
        // 26.x 制式=完整 MC 版本+build：26.1 是独立 MC 版本线非 26.1.2 旧构建，
        // deps.minecraft 用 MC 真身版本串（"26.1"/"26.1.1"）；neoforge tile 取
        // tmp/harvest/m3-matrix/maven-metadata.xml 枚举快照该线最新：
        //   26.1=26.1.0.19-beta（该线止于 beta）/ 26.1.1=26.1.1.15-beta（同）。
        // 批二 b 的 26.1.2 park 随本卡解除。26.1/26.1.1/26.1.2 三线 compileJava+build 绿。
        // ⚠ 26.2 二次 park（本卡 2026-09-15 实证，任务卡"TextureFormat 移包系同类打法"预期被
        // 证伪）：26.2 是第二波渲染换代——MultiBufferSource/RenderBuffers 全删（vanilla-26.2
        // patched sources 零引用），立即模式缓冲被 submit-dag（SubmitNodeCollector/
        // OrderedSubmitNodeCollector/CustomGeometryRenderer）取代；TextureFormat→
        // com.mojang.blaze3d.GpuFormat.RGBA8_UNORM、setScreen→gui.setScreen。移植面
        // =36 共享文件+4 RAW+12 处 getBuffer 立即绘制（geckolib3/SIMD 路径在内），独立一卡。
        // 注册保留+债入账（tasks.feature-debts.debt-26x-adaptation）。
        vers("26.1-neoforge", "26.1").buildscript = "build.moddev.gradle.kts"
        vers("26.1.1-neoforge", "26.1.1").buildscript = "build.moddev.gradle.kts"
        vers("26.1.2-neoforge", "26.1.2").buildscript = "build.moddev.gradle.kts"
        vers("26.2-neoforge", "26.2").buildscript = "build.moddev.gradle.kts"

        // M3 26.3 增量适配（m3-263-increment）：26.3 线挂载（tile=26.3.0.3-beta，该线全 beta
        // 无 stable——maven-metadata.xml:6-7,1715-1718 2026-09-16 快照 latest/release 实证；
        // 生产发布目标仍=26.2 GA，beta 期间适配先行）。26.3=26.2 submit-dag 延续收敛非换代
        //（tasks.m3-263-delta-survey 六项 delta：renderpearl 包迁移/OIT 进帧图/手部 API/
        // FPM shim 档等，>=26.2 既有条件轴自动覆盖本线），零新构建机制。
        vers("26.3-neoforge", "26.3").buildscript = "build.moddev.gradle.kts"

        // M3 批二 c-2：neoforge beta/ga 补线五线（1.21/21.2/21.6/21.7/21.9 全 Java 21）。
        // 版本号取官方 maven metadata 冻结快照（tmp/harvest/m3-matrix/maven-metadata.xml，
        // 任务卡给定 tile）：1.21=21.0.167(ga) / 21.2=21.2.1-beta / 21.6=21.6.20-beta /
        // 21.7=21.7.25-beta / 21.9=21.9.16-beta。版本 ID 沿批二 b 缩写约定：
        // 21.2 落 <21.5 分支=21.3 同形、21.6/21.7 落 <21.8=21.5 同形、21.9 落
        // <21.10=21.8 同形；1.21 用全称落 <1.21.2 分支=1.21.1 同形——零新机制，
        // 全部落既有分代树分支。
        // ⚠ POC 判负记录（2026-09-14，任务卡 POC 条款）：1.20.2=20.2.93 / 1.20.3=20.3.8-beta /
        // 1.20.5=20.5.21-beta 三线官方 maven 无 Gradle .module 元数据（目录 404 实证，
        // 构件仅 pom/userdev/universal/sources/installer），Gradle 从 POM 推导的 variant
        // 不携带 capability，MDG 解析必失败。实跑证据：:1.20.2/1.20.3/1.20.5-neoforge:
        // compileJava 三线同报 "Unable to find a variant with the requested capability:
        // coordinates 'net.neoforged:neoforge-moddev-bundle'"（零新机制不可修复，注册撤除；
        // 预案二=NFRT 直驱另立项，见判负报告）。
        vers("1.21-neoforge", "1.21").buildscript = "build.moddev.gradle.kts"
        vers("21.2-neoforge", "21.2").buildscript = "build.moddev.gradle.kts"
        vers("21.6-neoforge", "21.6").buildscript = "build.moddev.gradle.kts"
        vers("21.7-neoforge", "21.7").buildscript = "build.moddev.gradle.kts"
        vers("21.9-neoforge", "21.9").buildscript = "build.moddev.gradle.kts"

        // NFRT 直驱线（nfrt-poc-1202 立架，nfrt-flatline-1203-1205 三线平铺）：绕过 MDG
        // capability 解析——判负三线全数挂载（tiles=官方 maven metadata 冻结快照：
        // 1.20.2=20.2.93 / 1.20.3=20.3.8-beta / 1.20.5=20.5.21-beta；后两线 userdev 构件
        // maven 200 实证 2026-09-19）。构建脚本走 build.nfrt.gradle.kts（JavaExec 直调
        // NFRT CLI，旗标逐一对齐 MDG nfrtgradle/CreateMinecraftArtifacts.java 的调用面），
        // 与 build.moddev.gradle.kts 零共享代码路径：本键作用域外任何线不触碰该脚本，
        // 既有 35 线门禁不受影响。
        vers("1.20.2-neoforge", "1.20.2").buildscript = "build.nfrt.gradle.kts"
        // 1.20.3 线挂载（nfrt-flatline-1203-1205 卡线 2；tile=20.3.8-beta 冻结快照）
        vers("1.20.3-neoforge", "1.20.3").buildscript = "build.nfrt.gradle.kts"
        // 1.20.5 线挂载（nfrt-flatline-1203-1205 卡线 3；tile=20.5.21-beta 冻结快照）
        vers("1.20.5-neoforge", "1.20.5").buildscript = "build.nfrt.gradle.kts"

        // ===== legacy 1.12.2 线（legacy-1222-l0-poc）=====
        // RetroFuturaGradle 构建 POC（Celeritas-mva forge122 先例形态：
        // tmp/harvest/celeritas-mva/forge122/build.gradle.kts:12 RFB 1.4.8 + Java21 toolchain
        // + lwjgl3ify + mixinbooter 10.5，自带 SRG reobf）。独立 buildscript
        // build.legacy122.gradle.kts，与 build.forge/moddev/nfrt/unimined 零共享代码路径。
        // forge 版本 14.23.5.2859（任务卡冻结 tile；maven metadata 该线另有 2860~2864 后补
        // 构建但 2859 为社区公认末版稳定线）。
        vers("1.12.2-forge", "1.12.2").buildscript = "build.legacy122.gradle.kts"

        // ===== legacy 1.7.10 线（legacy-1710-l0-poc）=====
        // unimined 构建 POC（Celeritas-mva forge1710 先例形态：
        // tmp/harvest/celeritas-mva/forge1710/build.gradle:3,71-82,125,155-160：
        // searge+mcp stable_12 映射、forge 10.13.4.1614（1.7.10 末版）、
        // unimixins 0.1.19:dev、RFB bootstrap 主类、run Java21、lwjgl3ify）。
        // 独立 buildscript build.legacy1710.gradle.kts，与 build.legacy122/unimined
        // 零共享代码路径。
        vers("1.7.10-forge", "1.7.10").buildscript = "build.legacy1710.gradle.kts"

        vcsVersion = "1.20.1-forge"
    }
}
