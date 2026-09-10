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
    // common 侧 fabric-loader @Environment 注解的编译载体
    maven("https://maven.fabricmc.net/") { name = "FabricMC" }
    // architectury-api 过渡依赖（M0 仅求编译不红；M1 mig-purge-architectury 删）
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
    // 第三方 mod 编译依赖（26+ 兼容桥），仅编译期；运行时按 isModLoaded 守卫
    flatDir { dirs(rootProject.file("libs")) }
}

dependencies {
    // ===== 过渡期依赖（M1 逐域清零后由 mig-purge-architectury 删除）=====
    // @ExpectPlatform 注解 + dev.architectury 事件/Platform API 的编译与运行时载体。
    // 无 architectury-plugin 织入：@ExpectPlatform stub 运行时直调必 AssertionError，
    // 已知降级，见任务卡 m0-merge-sources 验收条款。
    implementation("dev.architectury:architectury-forge:${property("deps.architectury")}")
    // 注：勿引 dev.architectury:architectury（common 工件）——其签名为 intermediary
    // 命名（class_310 等），需 loom 重映射；architectury-forge 工件本身为 mojmap
    // 命名且含全套 dev.architectury API（382 类，Platform/Event/EventBuses 实测）
    // @ExpectPlatform/@PlatformOnly 注解本体（旧仓由 architectury-plugin 自动注入，此处显式声明）
    compileOnly("dev.architectury:architectury-injectables:1.0.13")
    // common 侧 @Environment(EnvType)/EnvType 注解仅编译期使用（forge 运行时不读取）
    compileOnly("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
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
