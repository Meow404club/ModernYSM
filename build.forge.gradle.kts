plugins {
    id("net.neoforged.moddev.legacyforge")
}

version = "${property("mod_version")}-${property("deps.minecraft")}-forge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

// ===== M3 平铺第一批 per-version 轴（1.17.1/1.18.2/1.19.2/1.19.4 与 1.20.1 共用本脚本；
// 1.16.5 走 build.unimined.gradle.kts 不进本脚本）=====
// 分段依据（本机 vanilla 反编译 + M3 矩阵调研 state:tasks.m3-matrix-research）：
//  - <1.19.3：MC 不内置 JOML（1.19.3 才内置）→ 编译期 implementation + 产物内嵌
//  - <1.20  ：共享源第三方 compat 树按 1.20.1 口径编写且零条件 → 整体闸门（同 1.16.5 先例）
//  - <1.18  ：1.17.1 运行时是 Java 16 → JitPack ImageStream（major 61）不可内嵌，改源码 vendor
// 资源口径替换上界：<1.20.1（含 1.20 线）。1.20 专属教训：原 "<1.20" 把 1.20 线排除在
// mods.toml loaderVersion 参数化外——forge 46 的 javafml=46 拒载 [47,) 声明
//（"Missing language javafml version [47,) wanted by main, found 46"，1.20 tour 实证），
// 生产 jar 同样拒载；1.20.1 线必须继续跳过（在产线产物零字节变化红线）
val pre120 = stonecutter.eval(stonecutter.current.version, "<1.20.1")
val pre1193 = stonecutter.eval(stonecutter.current.version, "<1.19.3")
val pre118 = stonecutter.eval(stonecutter.current.version, "<1.18")
// Forge 自带 MixinExtras 自 40.3.0 起（=1.18.2 线，批一 run_fixes 实证 split-package 边界）：
// 更早的 forge 大版本（37/38/39，即 <1.18.2 三线）dev 运行时缺 MixinExtras 运行体，
// @WrapWithCondition 被纯 Mixin 静默忽略——与 pre118 同因，运行时注入边界随之扩到 <1.18.2
val pre1182 = stonecutter.eval(stonecutter.current.version, "<1.18.2")
// mods.toml 装载区间用 forge 大版本号（37.1.1→37 / 47.4.20→47）
val forgeMajor = (property("deps.forge") as String).substringBefore('.')
val mcVersion = property("deps.minecraft") as String

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    // ImageStream（avif/webp 解码）快照
    maven("https://jitpack.io") { name = "JitPack" }
    // libs/ flatDir 仓库已删（slashblade-classpath 卡）：仓内依赖全经下方 fileTree/files
    // 显式进 compileOnly，全仓零 name:version 两段式坐标（grep 实证），flatDir 解析无人
    // 消费且对同名双 jar 序敏感（1.16.5 线 build.unimined.gradle.kts 同款不注册先例）
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
// ⚠ 1.17.1 例外（pre118）：JitPack 产物字节码 major 61（javap 实测），Java 16 运行时
// 载入即 UnsupportedClassVersionError（同 1.16.5 线 runServer 实测的 1.16.5 版问题）→
// 改用 1.16.5 线已语法下沉的源码 vendor（versions/1.16.5-forge/src/imagestream，8 文件，
// 语义逐行等价），由本线工具链（Java 21 + release 16）编译进主 jar；SPI services 文件
// 随 src/imagestream-resources 一并入包。
val imageStreamEmbed: Configuration? = if (pre118) null
else configurations.create("imageStreamEmbed").apply {
    isCanBeResolved = true
    isTransitive = false
}
dependencies {
    if (imageStreamEmbed != null) add("imageStreamEmbed", "com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
}
// 配置期显式解析为 File（configuration cache 安全，勿把 Configuration 持进任务）
val imageStreamEmbedJars: Set<File> = imageStreamEmbed?.files ?: emptySet()

dependencies {

    // avif/webp/jpeg 解码库（rip.ysm.imagestream 包名）：编译 + dev 运行时（下方
    // additionalRuntimeClasspath）；生产 jar 内嵌见上方 imageStreamEmbed。
    // 1.17.1（pre118）不声明：符号由源码 vendor 源集直接提供（上方注释）
    if (imageStreamEmbed != null) implementation("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    // 1.17.1 userdev 类路径无 org.jetbrains:annotations（1.16.5 mojmap userdev /
    // 1.18.2+ MC 库自带）→ compileOnly 补齐（不进产物）
    if (pre118) compileOnly("org.jetbrains:annotations:24.0.1")
    // JOML：共享源 geckolib3 渲染/动画栈 49 文件 import org.joml，MC 1.19.3 才内置
    //（<1.19.3 三线编译期缺失即红）；1.10.5 = MC 1.20.1 自带版本，字节码 major 46
    //（Java 1.2），1.17.1 的 Java 16 运行时可载。产物内嵌见 jar 任务（运行时同因缺失）。
    if (pre1193) implementation("org.joml:joml:1.10.5")
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
    // slashblade 双 fork 不走 fileTree（确定性钉序）：SlashBladeResharped-0.1.4-patched 与
    // x_SlashBlade-0.1.2 同类坐标重复（232 个重复类，清单 tmp/evidence/slashblade-classpath/），
    // fileTree 以 readdir 序（非排序）喂 javac，ISlashBladeState.getComboSeq 两 fork 返回型
    // 不同（0.1.4→ResourceLocation / 0.1.2→capability.ComboState），x 先到即
    // SlashBladeComboHelper.java:23 "不兼容的类型" 编译红——本机绿仅因 ext4 哈希序偶然
    //（双序实测：x-first=BUILD FAILED / R-first=BUILD SUCCESSFUL）。
    // 钉序语义：Resharped 必须先手（新 API 路径按其签名编写，编译必要条件）；
    // x 后手仅补旧 API 独有符号（capability/slashblade/ComboState，运行时 hasNewApi()=false
    // 分支 SlashBladeStateAccess 编译需要）。运行时行为不受编译序影响：两 jar 均 compileOnly
    // 不进产物，实际分支由已装载 mod 版本经 VersionRange("(,0.1.2]") 判定。
    // neoforge-* 子目录 = 各代 shim 编译域（build.moddev.gradle.kts 按线以
    // fileTree(libs/neoforge-2xx) 专属消费，21.2~21.8 映射表 + 26.1 直挂），非 1.20.1
    // 依赖：fileTree 默认递归会把 7 个 firstperson 同包 fork 全吞进本线 classpath，
    // javac 按 classpath 序首中解析 dev.tr7zw.firstperson.api.*（本线
    // FirstPersonCompat.java 引用面）——btrfs 主仓序 1.20.1 jar 先手=绿，tmpfs 冷检出
    // 序 neoforge-261（mc26.1.2 fork，major 69 > 本线 61）先手即红，与 slashblade
    // 同族非确定（本卡实测三处文件集全等纯 readdir 序差：btrfs 绿 / tmpfs 冷检出
    // a、b 双红，错误原文"类文件具有错误的版本 69.0, 应为 61.0"）。剪枝目录将递归
    // 面归零（libs/ 现存子目录仅此 7 个 neoforge-*）：新增子目录必须由归属构建脚本
    // 显式消费，禁止依赖本 fileTree 递归（同族先例 G 卡 slashblade 钉序 03eb6cf）。
    // 顶层 oculus 双 jar（1.8.0×1.6.13）同类隐患为已挂账独立债（签名现同暂良性），不在本卡。
    // ponytail: fileTree 对未来非 neoforge-* 新子目录仍递归吞入；命名约定破例时改显式 files() 钉序
    compileOnly(fileTree(rootProject.file("libs")) {
        exclude("*SlashBlade*")
        exclude("neoforge-*")
    })
    compileOnly(files(
        rootProject.file("libs/SlashBladeResharped-1.20.1-0.1.4-patched.jar"),
        rootProject.file("libs/x_SlashBlade-1.20.1-0.1.2.jar"),
    ))

    // Mixin refmap 注解处理器（SRG 重映射，生产 jar 必需）
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

// Mixin 配置注册：refmap 生成 + dev run 加载（生产 jar 另见 jar.manifest.MixinConfigs）
// 第三方 accessor 配置（yes_steve_model_forge.mixins.json，目标全为 Create/ParCool 类）
// 仅 1.20.1 注册——中段四线 compat 树整体闸门（见下方 sourceSets 块），配置随之不注册不打包
mixin {
    add(sourceSets.getByName("main"), "yes_steve_model.refmap.json")
    config("yes_steve_model.mixins.json")
    if (!pre120) {
        config("yes_steve_model_forge.mixins.json")
    }
}

// 1.17.1（pre118）工具链固定本地 21：MDG 依 MC 版本请求 Java 16 工具链 → 本机无 16 需
// foojay 下载，而 foojay-resolver-convention 0.9.0 在 Gradle 9.2 上初始化即
// NoSuchFieldError: IBM_SEMERU（JvmVendor 枚举跨版本不兼容，stacktrace 实证）→
// 编译目标以 javac --release 16 下发（Java 16 语义/字节码 major 60），运行期工具链 21。
if (pre118) {
    java.toolchain.languageVersion = JavaLanguageVersion.of(21)
    tasks.withType<JavaCompile>().configureEach {
        options.release = 16
    }
    // ASM force 升级（机制与三重先例见 state:tasks.m3-asm-problem-research）：dev run JVM=21
    // 时 1.17.1 内置 mixin 0.8.4 依赖的 asm 9.1（上限 major 61）解析 JDK 自身类
    // （java/lang/String major 65）→ ClassMetadataNotFoundException 拒启。asm 条目是
    // forge userdev 变体的 Gradle 模块依赖，同 GAV 坐标强升 9.8（V25，覆盖 Java 25 dev run）
    // 无排序问题；MDG 已用 NonStrictDependencyTransform 放宽 strict（先例：Celeritas
    // unimined asm 9.6 强升 + run JVM21、GTNH lwjgl3ify、forge 官方 1.17.x 分支自升 asm 9.6）
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.ow2.asm") useVersion("9.8")
        }
    }
    // 保底 A（注释态；若 asm force 验证受阻按此处启用——MDG 的 launcher set 在 register
    // 动作里，后置 configureEach 必赢）：
    // tasks.matching { it.name in listOf("runClient", "runServer") }.configureEach {
    //     javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
    // }
}

legacyForge {
    version = property("deps.minecraft") as String + "-" + property("deps.forge") as String
    validateAccessTransformers = true

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    // 1.18/1.18.1 dev run 专属：forge 39 的 securejarhandler 读 IMPL_LOOKUP 需显式开模块
    //（"java.base does not open java.lang.invoke to module cpw.mods.securejarhandler"，
    // 1.18.1 tour 实证；forge 40+ 自带解法不透传）。仅 run 配置，零产物影响。
    val securejarhandlerOpens = stonecutter.eval(stonecutter.current.version, ">=1.18") &&
            stonecutter.eval(stonecutter.current.version, "<1.18.2")

    runs {
        // client/server 分目录：默认同 run/ 会双进程互写 logs/latest.log。
        // 与 1.16.5 线（unimined run/server、run/client）约定统一，
        // harness/tour.sh 的 gameDir 推导表因此两线一致。
        register("client") {
            gameDirectory = file("run/client")
            client()
            if (securejarhandlerOpens) jvmArguments.addAll(listOf("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens=java.base/java.lang.invoke=cpw.mods.securejarhandler"))
        }
        register("server") {
            gameDirectory = file("run/server")
            server()
            if (securejarhandlerOpens) jvmArguments.addAll(listOf("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens=java.base/java.lang.invoke=cpw.mods.securejarhandler"))
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
// 1.17.1（pre118）不注入：ImageStream 类已由源码 vendor 进 main 源集产物，随 mods 注册进 dev 类路径
dependencies {
    if (imageStreamEmbed != null) {
        "additionalRuntimeClasspath"("com.github.TartaricAlkaline:ImageStream:-SNAPSHOT")
    }
    // MixinExtras dev 运行时：implementation 的 mixinextras-forge 会被 MDG 当 mod 装载
    //（module 层 mixinextras@0.3.6），其 MixinExtrasConfigPlugin.onLoad 引用
    // MixinExtrasBootstrap（API 体在 mixinextras-common）→ 缺 common 即 NCDFE 拒启。
    // 仅 Forge 未自带 MixinExtras 的线需要（批一实证 40.3.0 起自带）：1.17.1(37.1.1)/
    // 1.18(38.0.17)/1.18.1(39.1.2) 即 <1.18.2 三线；40.3.0+ 再注入 common 会
    // split-package 冲突（Modules mixinextras.common and MixinExtras export ... 实测）。
    // 生产 jar 内嵌归发布卡（同 1165 线 embedMixinExtras 先例）。
    if (pre1182) {
        "additionalRuntimeClasspath"("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    }
    // JOML dev 运行时（pre1193 三线）：implementation 对 MDG dev 不可见（同上），mixin
    // 后插桩/渲染栈 org/joml/Matrix4fc NCDFE 实测；1194+ MC 自带 JOML 不需要
    if (pre1193) {
        "additionalRuntimeClasspath"("org.joml:joml:1.10.5")
    }
}

// ===== 中段四线第三方 compat 闸门（pre120；机制同 1.16.5 线 build.unimined.gradle.kts）=====
// 第三方触点源码（rip/ysm/compat 60 文件 + client/compat 76 文件 + forge 第三方 accessor
// mixin 8 条 + 7 个边界文件）按 1.20.1 口径编写且零 stonecutter 条件，对 1.17~1.19 编译必炸
//（1.20.1-only 符号：GuiGraphics/RegisterGuiOverlaysEvent 等）→ 整树不进中段编译，core 代码经
// 1.16.5 线同包同名 no-op shim（mod-absent 语义，version-neutral API）保持符号可解析。
// shim 复用 versions/1.16.5-forge/src/shim/rip/ysm/compat（仅用 Player/CtrlBinding 等全谱
// 存在的 API，grep 验证零 TextComponent/GuiGraphics 触点，1.16.5~1.20.1 通用）。
// neoforge 三线专属跨版本工厂（Rl/isValid/YsmFrame.partialTick 仅 >=1.20.5、>=1.21 分支被
// 引用，forge 六线零引用）——不编译不打包，维持批一"1.16.5/1.20.1 产物条目集合与 ebc4427
// 基线全同"的语义零变化口径（neoforge 三线由 build.moddev 自行编译这两个类）
sourceSets.main {
    java {
        exclude("rip/ysm/util/Rl.java", "com/elfmcys/yesstevemodel/util/YsmFrame.java")
    }
}
if (pre120) {
    sourceSets.main {
        java {
            srcDir(rootProject.file("versions/1.16.5-forge/src/shim/rip/ysm/compat"))
            // 1.17.1（pre118）：ImageStream 源码 vendor（JitPack 产物 major 61 不可入 Java 16 产物，
            // 同 1.16.5 线 src/imagestream 8 文件，语义逐行等价）——见上方 dependencies 注释
            if (pre118) srcDir(rootProject.file("versions/1.16.5-forge/src/imagestream"))
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
    }
}

// runServer 控制台 stdin（harness/tour.sh 和平启动注入通道）：JavaExec 默认
// standardInput 为空流，daemon 下 Gradle 不自动转发，须显式接管 System.in。
tasks.named<org.gradle.api.tasks.JavaExec>("runServer") {
    standardInput = System.`in`
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/neoforge.mods.toml", "**/*.accesswidener")
        // 中段四线：第三方 accessor mixin 目标类不存在且类已闸出编译 → 配置不进 jar
        if (pre120) {
            exclude("yes_steve_model_forge.mixins.json")
        }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        // 收集件必须是 reobfJar 输出（=versions/<线>/build/libs 发布件）。jar 产物被 MDG
        // legacy 移入 build/devlibs（未 reobf dev jar，ObfuscationExtension.java:103-121
        // destinationDirectory=build/libs 归 reobfJar、jar 改道 devlibs+finalizedBy），
        // 收集 devlibs 件会发 SRG 名错 jar（prod-release-202609 审查踩坑实证）。
        from(named<Jar>("reobfJar").map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }

    jar {
        // 跨版本测试 harness 只进 dev run classpath，不进生产产物（acceptance：unzip -l
        // 零 rip/ysm/harness 条目）；driver 生产侧另有 harness.armed 标记守卫，双保险。
        // reobfJar 以 jar 产物为输入（MDG 文档：Reobfuscation automatically configured
        // for the jar task），exclude 后 reobf 输出天然无 harness。
        exclude("rip/ysm/harness/**")
        // ImageStream 生产内嵌：类文件 + javax.imageio SPI services 并入主 jar
        //（剥 manifest/签名，见上方 imageStreamEmbed 注释）；1.17.1 走源码 vendor 无此项
        if (!pre118) {
            from(imageStreamEmbedJars.map { zipTree(it) }) {
                include("rip/**", "META-INF/services/**")
            }
        }
        // JOML 生产内嵌（<1.19.3 三线，MC 类路径缺失；unimined embedJoml 同款语义）
        if (pre1193) {
            from(files(
                configurations.compileClasspath.get().files
                    .filter { it.name.startsWith("joml-") }
            ).map { zipTree(it) }) {
                include("org/joml/**")
            }
        }
        // 生产环境 Mixin 配置发现（MDG 文档：MixinConfigs 需写入 jar manifest）；
        // 中段四线不打包 forge accessor 配置 → manifest 同步只列主配置
        manifest {
            if (pre120) {
                attributes("MixinConfigs" to "yes_steve_model.mixins.json")
            } else {
                attributes("MixinConfigs" to "yes_steve_model.mixins.json,yes_steve_model_forge.mixins.json")
            }
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
    // mixins.json 注入 refmap 键：1.20.1 生产运行时是 mojmap 类名 + SRG 方法/字段名的混合口径
    //（m2-smoke-gate 生产实测引爆 refmap 卡挂账的"惰性炸弹"：LivingEntityAccessor @Invoker
    // 找不到 setLivingEntityFlag——生产真实方法名是 SRG m_ 系列）。MDG 编译期已产
    // yes_steve_model.refmap.json（mojmap→m_/f_ 映射齐全，jar 内实存），但 mixins.json 无
    // refmap 键则生产全数按字面 mojmap 名查目标=失配。注入后 dev 不受影响（dev 运行时全
    // mojmap，无 refmap 文件时 Mixin 报 warning 后直跑字面名，语义不变；照 1.16.5 线同款先例）。
    filesMatching("yes_steve_model.mixins.json") {
        filter { line: String ->
            if (line.contains("\"required\": true"))
                line.replace("\"required\": true", "\"required\": true,\n  \"refmap\": \"yes_steve_model.refmap.json\"")
            else line
        }
    }
    // ===== 中段四线资源口径替换（1.20.1 跳过：不进 filter 链，保证在产线产物零字节变化；
    // 替换串与共享源 1.20.1 字面量逐字对照，机制同 1.16.5 线 build.unimined.gradle.kts）=====
    if (pre120) {
        // mods.toml 装载区间：loaderVersion/forge=各线 forge 大版本、minecraft=本版本
        // （共享源写 1.20.1 口径 [47,)/[47,)/[1.20.1,)；mandatory=true 三处声明不替换必拒载）
        filesMatching("META-INF/mods.toml") {
            filter { line: String ->
                line.replace("loaderVersion = \"[47,)\"", "loaderVersion = \"[$forgeMajor,)\"")
                    .replace("versionRange = \"[47,)\"", "versionRange = \"[$forgeMajor,)\"")
                    .replace("versionRange = \"[1.20.1,)\"", "versionRange = \"[$mcVersion,)\"")
            }
        }
        // pack.mcmeta：资源包格式 1.17.1=7 / 1.18.x=8 / 1.19~1.19.2=9 / 1.19.3=12 /
        // 1.19.4=13 / 1.20=15（共享源为 1.20.1 口径 15；批一 >=1.19.3 一刀切 15 对
        // 1.19.3/1.19.4 与真实值 12/13 不符，批二 c-1 注册新线时一并修正）
        val packFormat = if (stonecutter.eval(stonecutter.current.version, ">=1.20")) 15
        else if (stonecutter.eval(stonecutter.current.version, ">=1.19.4")) 13
        else if (stonecutter.eval(stonecutter.current.version, ">=1.19.3")) 12
        else if (stonecutter.eval(stonecutter.current.version, ">=1.19")) 9
        else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) 8
        else 7
        filesMatching("pack.mcmeta") {
            filter { line: String -> line.replace("\"pack_format\": 15", "\"pack_format\": $packFormat") }
        }
        // mixins.json compatibilityLevel：1.17.1 产物为 Java 16 字节码（pre118 javac --release 16），
        // 且 1.17.1 运行时 JVM=16 → JAVA_17 级别声明与产物/运行时双双不符（机制同 1.16.5 线
        // build.unimined.gradle.kts 的 JAVA_17→JAVA_8 替换）；1.18.2+ 运行时 JVM=17 保持 JAVA_17
        if (pre118) {
            filesMatching("*.mixins.json") {
                filter { line: String -> line.replace("\"JAVA_17\"", "\"JAVA_16\"") }
            }
        }
    }
}
