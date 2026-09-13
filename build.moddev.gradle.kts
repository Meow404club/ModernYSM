plugins {
    id("net.neoforged.moddev")
}

version = "${property("mod_version")}-${property("deps.minecraft")}-neoforge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

// ===== M3 批二 a：neoforge 三线（1.20.4/1.20.6/1.21.1）共用本脚本 =====
// 分段依据（本机 vanilla 索引 + MDG 2.0.141 源码 tmp/harvest/m3-asm-mdg-src + NeoForge 官方文档
// tmp/refs/neoforge-docs-full，检索证据见行内）：
//  - <1.20.5（仅 1.20.4）：META-INF/mods.toml 装载（docs version-1.20.4 modfiles.md
//    "The mods.toml file, located at src/main/resources/META-INF/mods.toml"）；
//    >=1.20.5 起改名 META-INF/neoforge.mods.toml（docs version-1.20.6 modfiles.md:61）
//  - Java：1.20.4=17、1.20.6/1.21.1=21（docs.neoforged.net Java 表；MDG 按 MC 版本自动
//    请求对应 toolchain——legacyforge 1.17.1 请求 16 同机制，批一实证）
//  - 运行时命名：NeoForge 1.20.2+ 全链 mojmap（无 SRG reobf）→ 无 reobfJar、无 refmap：
//    不挂 org.spongepowered:mixin:processor 注解处理器、mixins.json 不注入 refmap 键
//    （批一 forge 线的 refmap 注入是 SRG 运行时专用，neoforge 线字面 mojmap 名即真名）
val pre1205 = stonecutter.eval(stonecutter.current.version, "<1.20.5")
val mcVersion = property("deps.minecraft") as String
// neoforge 装载区间用线大版本（20.4.251→20.4 / 21.1.250→21.1）
val neoMajor = (property("deps.neoforge") as String).substringBeforeLast('.')

// NFRT 类路径含版本区间依赖（log4j-core 2.11.+），每次解析都 HEAD maven-metadata——
// maven.neoforged.net 偶发 502 即断构建（实测两次）→ 钉死具体版本（loader 2.0.17 自带 2.19.0）去抖
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j" && requested.name == "log4j-core") {
            useVersion("2.19.0")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
}

// ImageStream 生产内嵌（机制同 build.forge.gradle.kts imageStreamEmbed；三线 Java 17/21
// 运行时均可载 JitPack 产物 major 61，无 1.17.1 的 pre118 源码 vendor 例外）。
// dev 运行时经 additionalRuntimeClasspath 注入（见下方依赖块），生产 jar 由 jar 任务并入。
val imageStreamEmbed = configurations.create("imageStreamEmbed") {
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）：编译 + dev 运行时 + 生产内嵌
    implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    add("imageStreamEmbed", "com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // MixinExtras：@WrapOperation 等注解编译面；运行时 NeoForge 20.2+ 自带模块
    //（forge 线的 mixinextras-forge implementation 与 1171 的 additionalRuntimeClasspath
    // 注入均不适用——neoforge 线无独立 forge 变体 jar，也不存在 split-package 问题）
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
}

neoForge {
    version = property("deps.neoforge") as String
    validateAccessTransformers = true

    runs {
        // client/server 分目录：与 forge 线 run/client、run/server 约定一致
        //（MDG 默认 run/ 会双进程互写 logs/latest.log，build.forge.gradle.kts 同注释）
        register("client") {
            gameDirectory = file("run/client")
            client()
        }
        register("server") {
            gameDirectory = file("run/server")
            server()
        }
    }

    mods {
        register(property("archives_name") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

// additionalRuntimeClasspath configuration 由 MDG runs 装配期（上方 neoForge 块求值时）
// 创建，故依赖声明必须置于其后（同 build.forge.gradle.kts legacyForge 块的次序约束）
dependencies {
    // MDG 主插件 dev run 由 NFRT 生成类路径，项目 implementation 不可见（同 legacyforge，
    // build.forge.gradle.kts 注释实证）→ ImageStream 走 MDG 官方注入口 additionalRuntimeClasspath
    "additionalRuntimeClasspath"("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
}
// 配置期显式解析为 File（configuration cache 安全，勿把 Configuration 持进任务）
val imageStreamEmbedJars: Set<File> = imageStreamEmbed.files

// ===== neoforge 线专属源集（绕开 stonecutter 的 RAW srcDir，同 forge 线 shim srcDir 先例）=====
//  - src/neoforge/java：platform/neoforge 平台实现跨代同形部分（主类/能力 provider/桥，纯
//    neoforge 代码零条件块）——与 platform/forge 树镜像互斥，forge 六线 sourceSet 不含此目录零接触
//  - src/neoforge-1204/java、src/neoforge-1205/java：1.20.5 网络重铸/事件注解换代的孪生分代树
//    （同 FQCN 双版本，按线二选一挂载，防同源集双份类定义冲突）：
//      1204=1.20.4 代（RegisterPayloadHandlerEvent/IPayloadRegistrar/PlayPayloadContext/
//           Mod.EventBusSubscriber/NeoForgeMod.BLOCK_REACH），
//      1205=1.20.6/1.21.1 共用代（RegisterPayloadHandlersEvent/PayloadRegistrar.playBidirectional
//           +StreamCodec/IPayloadContext/fml.common.EventBusSubscriber/vanilla Attributes.*）
//  - src/neoforge-resources：META-INF/neoforge.mods.toml 模板（20.5+ 口径，1.20.4 线在
//    processResources 里 rename 回 mods.toml）——不能放共享 src/main/resources：
//    build.unimined.gradle.kts（1.16.5 在产线）的 processResources 无 neoforge.mods.toml
//    排除项，放共享目录会给 1.16.5 产物加新条目，破坏批一"产物语义零变化"口径
//  - compat shim 复用 1.16.5 线版本中立 shim（同 build.forge.gradle.kts pre120 块）
sourceSets.main {
    java {
        srcDir(rootProject.file("src/neoforge/java"))
        if (pre1205) {
            srcDir(rootProject.file("src/neoforge-1204/java"))
        } else if (stonecutter.eval(stonecutter.current.version, "<1.21")) {
            // 1205 树 = 1.20.6 代共用部分；1206 树 = 1.20.6 独占分歧
            //（ShieldBlockEvent 在 1.21.1 更名 LivingShieldBlockEvent）
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1206/java"))
        } else {
            // 1211 树 = 1.21.1 分歧（LivingShieldBlockEvent、ItemAbilities 更名，
            // neoforge-1.21.1 实证）；shim 整树副本（ResourceLocation 私有构造 /
            // isValidResourceLocation 删除 → Rl/parse，2 文件已修），不挂原 shim 防 RAW 双份
            srcDir(rootProject.file("src/neoforge-1205/java"))
            srcDir(rootProject.file("src/neoforge-1211/java"))
        }
        // shim：<1.21 挂原件；1.21.1 挂整树副本（ResourceLocation 私有构造 /
        // isValidResourceLocation 删除 → Rl/parse，2 文件已修，RAW 无条件化能力）
        if (stonecutter.eval(stonecutter.current.version, "<1.21")) {
            srcDir(rootProject.file("versions/1.16.5-forge/src/shim/rip/ysm/compat"))
        } else {
            srcDir(rootProject.file("src/neoforge-1211/shim/rip/ysm/compat"))
        }
        // 第三方触点源码闸门 + platform/forge 树整体排除（清单与 build.forge.gradle.kts pre120
        // 块同源）。孪生走异包策略：src/neoforge/java 下 platform/neoforge 包（类名不变），
        // 接缝消费方 import 交换（transform_neoforge.py），排除 glob 不会误伤孪生：
        exclude(
            "rip/ysm/compat/**",
            "com/elfmcys/yesstevemodel/client/compat/**",
            "com/elfmcys/yesstevemodel/platform/forge/**",
            "com/elfmcys/yesstevemodel/client/gui/button/ConfigCheckBoxForge.java",
            "rip/ysm/api/attribute/platform/forge/**",
            "rip/ysm/api/client/platform/forge/**",
            "rip/ysm/api/entity/platform/forge/**",
            "rip/ysm/api/item/platform/forge/**",
        )
    }
    resources {
        srcDir(rootProject.file("src/neoforge-resources"))
    }
}

tasks {
    processResources {
        // forge 口径 mods.toml 与第三方 accessor 配置不进 neoforge 产物
        exclude("META-INF/mods.toml", "yes_steve_model_forge.mixins.json",
            "**/fabric.mod.json", "**/*.accesswidener")
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
        // 跨版本测试 harness 只进 dev run classpath，不进生产产物（同 forge 线口径）
        exclude("rip/ysm/harness/**")
        // ImageStream 生产内嵌：类文件 + javax.imageio SPI services 并入主 jar（剥 manifest/签名）
        from(imageStreamEmbedJars.map { zipTree(it) }) {
            include("rip/**", "META-INF/services/**")
        }
        // 生产环境 Mixin 配置发现：NeoForge 20.4+ 自动扫描 mod 内 mixin 配置（dev run 实证），
        // manifest 项为 belt-and-braces（老版本 discovery 兜底，与 forge 线口径一致）
        manifest {
            attributes("MixinConfigs" to "yes_steve_model.mixins.json")
        }
    }
}

java {
    withSourcesJar()
    // 1.20.4 运行时 Java 17；1.20.6/1.21.1 运行时 Java 21（docs.neoforged.net Java 表）
    val javaCompat = if (pre1205) JavaVersion.VERSION_17 else JavaVersion.VERSION_21
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

tasks.withType<JavaCompile>().configureEach {
    options.release = if (pre1205) 17 else 21
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to project.property("archives_name") as String,
        "mod_name" to project.property("mod_name") as String,
        "mod_version" to project.property("mod_version") as String,
        "mod_license" to project.property("mod_license") as String,
    )

    // 1.20.4 线：neoforge.mods.toml 模板 rename 回 META-INF/mods.toml（20.5 起才改名）
    if (pre1205) {
        rename { fileName ->
            if (fileName == "neoforge.mods.toml") "mods.toml" else fileName
        }
    }
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
    // ===== per-line 资源口径替换（模板基线值=1.20.6 线；与共享源字面量逐字对照）=====
    // neoforge.mods.toml / 1.20.4 改名后的 mods.toml：neoforge/minecraft 装载区间
    //（loaderVersion 三线统一 "[1,)"，docs version-1.20.4/1.20.6/1.21.1 modfiles.md 明文）
    // 配置缓存铁律：filter lambda 只可捕获任务配置块内的局部 val（脚本顶层 val = 脚本对象
    // 引用，不可序列化——1.20.6 build 配置缓存实测报错），先物化为局部量
    val neoMajorLocal = neoMajor
    val mcVersionLocal = mcVersion
    filesMatching(listOf("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        filter { line: String ->
            line.replace("versionRange = \"[20.6,)\"", "versionRange = \"[$neoMajorLocal,)\"")
                .replace("versionRange = \"[1.20.6,)\"", "versionRange = \"[$mcVersionLocal,)\"")
        }
    }
    // pack.mcmeta：资源包格式 1.20.4=22 / 1.20.6=32 / 1.21.1=34（共享源为 1.20.1 口径 15）
    val packFormat = if (pre1205) 22
    else if (stonecutter.eval(stonecutter.current.version, "<1.21")) 32
    else 34
    filesMatching("pack.mcmeta") {
        filter { line: String -> line.replace("\"pack_format\": 15", "\"pack_format\": $packFormat") }
    }
    // mixins.json compatibilityLevel：1.20.4 产物 Java 17 字节码 → 保持 JAVA_17；
    // 1.20.6/1.21.1 产物 Java 21 字节码 + Java 21 运行时 → JAVA_17 声明双不符，替换为 JAVA_21
    //（机制同 build.forge.gradle.kts 的 pre118 JAVA_17→JAVA_16 替换；产物实测归终验 d）
    // Arrow 效果 accessor 版本化：1.20.5+ Arrow.effects 字段删除（数据组件化），换成
    // PotionContents Invoker（src/neoforge-{1205,1211}/java .../mixin/client/ArrowPotionAccessor）
    if (!pre1205) {
        filesMatching("*.mixins.json") {
            filter { line: String -> line.replace("\"JAVA_17\"", "\"JAVA_21\"")
                .replace("\"client.ArrowEntityAccessor\"", "\"client.ArrowPotionAccessor\"") }
        }
    }
}

// runServer 控制台 stdin（harness/tour.sh 和平启动注入通道）：与 forge 线同款
tasks.named<org.gradle.api.tasks.JavaExec>("runServer") {
    standardInput = System.`in`
}
