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
    // 第三方 mod 兼容桥编译依赖（libs/ 下 jar），仅编译期；运行时按 isModLoaded 守卫
    flatDir { dirs(rootProject.file("libs")) }
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
    // MixinExtras：EntityRenderDispatcherMixin 的 @WrapOperation 编译期依赖；
    // 1.16.5 运行时注入方式（内嵌/伴生）未定，本卡只保编译
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    // 第三方 mod 兼容桥编译依赖（m2-gate-compat 卡负责 1.16.5 缺失桥的分流）
    compileOnly(fileTree(rootProject.file("libs")))
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
tasks.named<ProcessResources>("processResources") {
    filesMatching("*.mixins.json") {
        filter { line: String -> line.replace("\"JAVA_17\"", "\"JAVA_8\"") }
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
