// unimined 版本路由构建脚本（MC <1.17 的 forge 线；>=1.17 走 build.forge.gradle.kts / MDG legacyforge）
// 配置范式实证来源：
//  - 本仓 POC：tmp/poc-1165/unimined/（RUN-REPORT.md：1.16.5/1.12.2/1.7.10 三代 forge 全 PASS，SRG 发布 jar）
//  - GTNH/Celeritas（stonecutter + unimined 生产先例，Gradle 9.x 同款）
//  - unimined 官方 testing 1.16.5-Forge：minecraftForge { mixinConfig } 用法
//    （tmp/harvest/unimined-testing-1165/build.gradle）
// 已知限制：unimined 1.4.1 的 remapJar 与 Gradle 9.2.1 configuration cache 不兼容
// （任务实现持活 Configuration/Project 引用，报 "Cannot mutate content repository
// descriptor 'modsRemap(...)' after repository has been used"）。remapJar 入任务图时
// （如 :1.16.5-forge:build）需加 --no-configuration-cache；compileJava /
// processResources 等无此问题。1.20.1 线（MDG legacyforge）不受影响。
plugins {
    id("xyz.wagyourtail.unimined") version "1.4.1" // 稳定版；解析/DSL 失败退 "1.3.16-SNAPSHOT"（POC 未触发退路）
}

// mc-forge 身份由 archivesName 承载（防同名覆盖），version 只带 mod 版本号
version = property("mod_version") as String
// POC 实测教训（RUN-REPORT「已知注意点」）：archivesName 必须含 stonecutter.current.version，
// 否则多版本产物在根 build/libs 相互覆盖
base.archivesName = "${property("archives_name")}-${stonecutter.current.version}"
group = property("maven_group") as String

val mcVersion = property("deps.minecraft") as String

// Java 分段（与 build.forge.gradle.kts 口径一致：1.16.5 → 8）。
// daemon/toolchain 跑 JDK21（foojay 兜底可拉），javac --release 8 出目标字节码——unimined 官方 testing 同款
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
    // 注意：1.16.5 线不注册 libs/ flatDir——29 个第三方 jar 全是 1.20.1 口径，
    // 进 compile classpath 既编译必炸也无意义（m2-gate-compat）。compat 门面符号由
    // versions/1.16.5-forge/src/main/java 下的版本独有 no-op shim 提供（mod-absent 语义）。
}

unimined.minecraft {
    version(mcVersion)

    mappings {
        searge() // 发布命名空间：重映射回 SRG（remapJar 产物 func_/field_ 命名）
        mojmap() // 编译命名空间：1.16.5 官方映射（与 1.20.1 共享源码的 mojmap 口径一致）
        // 注：POC 脚本的 devFallbackNamespace("searge") 在 unimined 1.4.1 已 deprecated
        //（"No longer needed"），平移时删除
    }

    minecraftForge {
        loader(property("deps.forge") as String)
        // 主 mixin 配置注册（unimined 官方 testing-1165 同款，同时接通 refmap 注解处理器）；
        // yes_steve_model_forge.mixins.json（第三方 accessor 桥）1.16.5 下目标类不存在，
        // 不注册也不打包（见 processResources exclude），由 m2-gate-compat 卡做版本分流
        mixinConfig("yes_steve_model.mixins.json")
    }
}

dependencies {
    // ImageStream（rip.ysm.imagestream 纯 Java 库）：编译期可见，保编译基线干净；
    // 生产 jar 内嵌（照 1.20.1 的 ImageStream 先例）属后续卡
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // MixinExtras：保留（查证记录，m2-gate-compat）——本 mod 自有 mixin
    // EntityRenderDispatcherMixin 用 @WrapOperation，且它注册在 1.16.5 有效的
    // yes_steve_model.mixins.json（platform/forge 第三方 accessor 配置才是被闸对象）；
    // 运行时注入方式（内嵌/伴生）未定，本卡只保编译
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    // 1.16.5 不声明 libs/ fileTree：第三方 compat 依赖整体闸在本版本构建外
    //（排除规则见下方 stonecutterGenerate 块，门面 shim 见 versions/1.16.5-forge/src/main/java）
}

// ===== 1.16.5 compat 闸门（m2-gate-compat）=====
// 第三方触点源码整树不进 1.16.5 编译，core 代码经同包同名 no-op shim 保持符号可解析。
// 引用图盘点（grep 传递闭包实证，基线 3483e9d）：
//  - rip/ysm/compat/**（60 文件）：门面 + platform/forge *Impl，Impl 链到 client/compat
//  - com/elfmcys/yesstevemodel/client/compat/**（76 文件）：第三方 import 集中地
//  - platform/forge/mixin/client/{create,parcool}/**：yes_steve_model_forge.mixins.json
//    注册的 8 条第三方 accessor（1.16.5 目标类不存在，配置本身已被 processResources 排除）
//  - 7 个边界文件（第三方直连或引 client/compat，反向引用全落在排除树内，编译闭环安全）：
//    ForgeClientSetupHooks（引 24 个 client/compat + 1.20.1-only RegisterGuiOverlaysEvent，
//    1.16.5 API 不存在本就编不过；被注解自动扫描加载，无代码级引用者）、
//    TouhouMaidAnimationPredicate、TouhouMaidModelScreen、TouhouMaidTextureScreen、
//    TouhouMaidModelButton、TouhouMaidTextureButton、SophisticatedBackpackLayer
sourceSets.main {
    java {
        srcDir("src/main/shim/rip/ysm/compat")
        exclude(
            "rip/ysm/compat/**",
            "com/elfmcys/yesstevemodel/client/compat/**",
            "com/elfmcys/yesstevemodel/platform/forge/mixin/client/create/**",
            "com/elfmcys/yesstevemodel/platform/forge/mixin/client/parcool/**",
            "com/elfmcys/yesstevemodel/platform/forge/ForgeClientSetupHooks.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/animation/predicate/TouhouMaidAnimationPredicate.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/gui/TouhouMaidModelScreen.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/gui/TouhouMaidTextureScreen.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/gui/button/TouhouMaidModelButton.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/gui/button/TouhouMaidTextureButton.java",
            "com/elfmcys/yesstevemodel/platform/forge/client/renderer/layer/SophisticatedBackpackLayer.java",
        )
    }
    // shim 落点说明：srcDir 根即 rip/ysm/compat 包目录（文件相对路径只剩文件名），
    // 天然不命中上方 "rip/ysm/compat/**" 排除模式——否则排除会把版本独有 shim 一并杀掉
    //（shim 与被排除源同包同名 FQCN，靠 sourceSets 级 exclude 与生成树互斥）。
    // shim 内容 = 门面签名镜像 + mod-absent 返回值，运行时语义与 1.20.1 守卫链缺席分支一致。
}
tasks {
    processResources {
        // 第三方 accessor mixin（Create/ParCool 桥）：1.16.5 无效且加载即炸 → 不进 1.16.5 jar
        exclude("yes_steve_model_forge.mixins.json")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(named("remapJar")) // unimined 产物：已重映射到 SRG 的发布 jar
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }
}

// mixin json 的 compatibilityLevel 是 JSON 字面量（stonecutter 注释预处理不适用于 json），
// 构建期 token 替换：JAVA_17 → JAVA_8（1.16.5 线产物即 Java 8 字节码，与其保持一致）
// pack.mcmeta 同理：pack_format 未版本化（骨架卡移交项），1.16.5 资源包 = 6（共享源写
// 的是 1.20.1 口径的 15），同一替换模式按版本 token 替换
tasks.named<ProcessResources>("processResources") {
    filesMatching("*.mixins.json") {
        filter { line: String -> line.replace("\"JAVA_17\"", "\"JAVA_8\"") }
    }
    filesMatching("pack.mcmeta") {
        filter { line: String -> line.replace("\"pack_format\": 15", "\"pack_format\": 6") }
    }
    val props = mapOf(
        "mod_id" to project.property("archives_name") as String,
        "mod_name" to project.property("mod_name") as String,
        "mod_version" to project.property("mod_version") as String,
        "mod_license" to project.property("mod_license") as String,
    )
    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
}
