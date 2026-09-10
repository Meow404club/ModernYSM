plugins {
    id("net.neoforged.moddev.legacyforge")
}

version = "${property("mod_version")}-${property("deps.minecraft")}-forge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
    // 第三方 mod 编译依赖（26+ 兼容桥），仅编译期；运行时按 isModLoaded 守卫
    flatDir { dirs(rootProject.file("libs")) }
}

// ImageStream 生产内嵌（发布策略项落地，等价旧仓 JIJ include）：
// dev 运行时已由 additionalRuntimeClasspath 注入（见下方依赖块注释）；
// 生产 jar 由 jar 任务直接并入 ImageStream 类文件与 META-INF/services。
// 不用 MDG jarJar 的原因：JitPack 模块版本字面量为 "-SNAPSHOT"，版本区间
// strictly/prefer 难以稳定表达，且 legacy 插件的 reobfJar 链路不覆盖 jarJar
// 独立产物，发布面多一条易错路径。ImageStream 为唯一包名（rip.ysm.imagestream）
// 纯 Java 库（无 mods.toml/FMLModType/module-info，仅 javax.imageio SPI 两个
// services 文件，主 jar 无同名冲突），并入主 jar 后随 reobfJar 一起发布，
// 对 SRG 重映射不可知，语义与 JIJ 内嵌一致。
val imageStreamEmbed: Configuration by configurations.creating {
    isCanBeResolved = true
    isTransitive = false
}
dependencies {
    imageStreamEmbed("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
}
// 配置期显式解析为 File（configuration cache 安全，勿把 Configuration 持进任务）
val imageStreamEmbedJars: Set<File> = imageStreamEmbed.files

dependencies {

    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）：编译 + dev 运行时（下方
    // additionalRuntimeClasspath）；生产 jar 内嵌见上方 imageStreamEmbed
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // ImageStream 进 dev 运行时游戏类路径（m0 P2 NCDFE 修复）：
    // MDG legacyforge 的 dev run 由 NFRT 生成 *LegacyClasspath.txt（仅 userdev 内置库），
    // BootstrapLauncher.loadLegacyClassPath 优先按 -DlegacyClassPath.file 重建游戏类路径，
    // 项目 implementation/runtimeOnly 依赖一律不可见（plain jar 也不会像带 mods.toml 的
    // jar 那样被当 mod 发现）→ mod 代码 new AvifDecoder() 必 NCDFE。
    // additionalRuntimeClasspath 是 MDG 官方注入口（README "External Dependencies: Runs"，
    // MC ≤1.21.8 必需），NFRT 将其并入 dev 运行时类路径。
    // 生产 jar 内嵌见上方 imageStreamEmbed（jar 任务并入，等价旧仓 JIJ include）。
    // MixinExtras：EntityRenderDispatcherMixin 使用 @WrapOperation
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    implementation("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")

    // ===== 第三方 mod 兼容桥编译依赖（libs/ 下 29 jar，含 touhoulittlemaid 内
    // vendored 的 org.gagravarr 供编译解析；仅 compileOnly，不进运行时）=====
    compileOnly(fileTree(rootProject.file("libs")))

    // Mixin refmap 注解处理器（SRG 重映射，生产 jar 必需）
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

// Mixin 配置注册：refmap 生成 + dev run 加载（生产 jar 另见 jar.manifest.MixinConfigs）
mixin {
    add(sourceSets.getByName("main"), "yes_steve_model.refmap.json")
    config("yes_steve_model.mixins.json")
    config("yes_steve_model_forge.mixins.json")
}

legacyForge {
    version = property("deps.minecraft") as String + "-" + property("deps.forge") as String
    validateAccessTransformers = true

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("archives_name") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

// additionalRuntimeClasspath configuration 由 MDG runs 装配期（上方 legacyForge 块求值时）
// 创建，故依赖声明必须置于其后（Kotlin DSL 无类型安全访问器，按名引用，cache 安全）
dependencies {
    "additionalRuntimeClasspath"("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/neoforge.mods.toml", "**/*.accesswidener")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }

    jar {
        // ImageStream 生产内嵌：类文件 + javax.imageio SPI services 并入主 jar
        //（剥 manifest/签名，见上方 imageStreamEmbed 注释）
        from(imageStreamEmbedJars.map { zipTree(it) }) {
            include("rip/**", "META-INF/services/**")
        }
        // 生产环境 Mixin 配置发现（MDG 文档：MixinConfigs 需写入 jar manifest）
        manifest {
            attributes("MixinConfigs" to "yes_steve_model.mixins.json,yes_steve_model_forge.mixins.json")
        }
        finalizedBy("reobfJar")
    }
}

java {
    withSourcesJar()
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21
    else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) JavaVersion.VERSION_17
    else if (stonecutter.eval(stonecutter.current.version, ">=1.17")) JavaVersion.VERSION_16
    else JavaVersion.VERSION_1_8
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

tasks.named<ProcessResources>("processResources") {
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
