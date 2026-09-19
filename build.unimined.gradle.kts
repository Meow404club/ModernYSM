import java.util.jar.JarFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.FileInputStream
import java.io.FileOutputStream
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
// forge 大版本号（36.2.39→36）：mods.toml loaderVersion/forge 装载区间用
val forgeMajor = (property("deps.forge") as String).substringBefore('.')
// 版本独有源（shim/imagestream vendor/mcp stub）物理落点：1.16.5 拥有本体，
// 其余 unimined 线（1.16.1~1.16.4，批二 c-1）复用 1.16.5 树（全部为 <1.17 通用
// API/纯 Java 8 源，零 1.16.5 专属符号；机制同 build.forge.gradle.kts 中段线的
// rootProject 锚定先例）。1.16.5 相对路径语义不变（在产线脚本形态保持）。
val vendoredBase: java.io.File = if (stonecutter.current.version == "1.16.5") projectDir
else rootProject.file("versions/1.16.5-forge")

// Java 分段（与 build.forge.gradle.kts 口径一致：1.16.5 → 8）。
// daemon/toolchain 跑 JDK21（foojay 兜底可拉），javac --release 8 出目标字节码——unimined 官方 testing 同款
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    // javac 默认 -Xmaxerrs=100 会截断报错清单（1951 错基线只见首 100），
    // 放开上限让各条件卡的"域内错误清零"可按 file:line diff 取证
    //（与 work/m2-render-pipeline-condition a2183c9 同行同内容，合并自动收敛）
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

// ===== unsafe8 源集（m2-compile-green-gate，audit 例外清单唯一条目）=====
// sun.misc.Unsafe 在 javac --release 8 的 ct.sym 审计器下不可见（ct.sym 只收录 JDK8 文档化
// API；sun.misc.Unsafe 是非文档 API，1.16.5 真实 JDK8 rt.jar 本就有）。-source/-target 8
// 直读 JDK 系统镜像则可见。处置：把仓库唯二 Unsafe 编译期触点（rip/ysm/zstd/** 纯 Java
// zstd 实现 + util/UnsafeUtil）隔离进本源集，用 source/target 8 编译（放弃 ct.sym 审计=
// audit 例外），其余全部主源集代码保持 --release 8 审计不放松。
// 注：这些文件经 grep 验证零 stonecutter 条件（//? if），raw 编译与展开树逐字等价。
sourceSets {
    create("unsafe8") {
        java {
            // 版本子项目 projectDir=versions/1.16.5-forge，共享源必须 rootProject 锚定
            srcDir(rootProject.file("src/main/java"))
            include("rip/ysm/zstd/**")
            include("com/elfmcys/yesstevemodel/util/UnsafeUtil.java")
        }
    }
}
sourceSets.main {
    java {
        exclude("rip/ysm/zstd/**")
        exclude("com/elfmcys/yesstevemodel/util/UnsafeUtil.java")
        // neoforge 三线专属跨版本工厂（forge 1.16.5 线零引用，见 build.forge 同款注释）
        exclude("rip/ysm/util/Rl.java", "com/elfmcys/yesstevemodel/util/YsmFrame.java")
    }
}
tasks.named<JavaCompile>("compileUnsafe8Java") {
    // audit 例外：此处不得用 --release 8（见 unsafe8 源集头注）；字节码目标仍是 major 52
    options.release = null
    sourceCompatibility = "8"
    targetCompatibility = "8"
}
// main（GeoEntity 等）引用 util.UnsafeUtil → unsafe8 产物进 main 编译/运行时类路径
//（sourceSets.output 自带任务依赖，compileJava 自动 dependsOn compileUnsafe8Java；
//  runtimeClasspath 供 unimined dev runClient 载入 zstd 类）
sourceSets.main {
    compileClasspath += sourceSets.getByName("unsafe8").output
    runtimeClasspath += sourceSets.getByName("unsafe8").output
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

// ===== dev 运行面依赖代差矫正（unimined-env-116x）=====
// 本脚本只服务 1.16.1~1.16.5 五线（settings vers 路由），configurations.all 天然圈定范围。
// 逐项机制（全部 runServer 实测）：
//  1) modlauncher 8.0.6(1163)/8.0.9(1164) 的 SecureJarHandler.createCodeSource 硬编码
//     invokespecial sun/security/util/ManifestEntryVerifier."<init>":(Ljava/util/jar/Manifest;)V
//     —— 该单参构造在 JDK 8u312+ 已删（本机 azul 1.8.0_504 只剩 (Manifest,String)），
//     TCL 首次建 CodeSource 即 NSME（1163/1164 runServer 双复现，栈 SecureJarHandler.java:66）。
//     8.1.x（1165 原生在用、azul-8 亲证）已改 MEV_FACTORY 反射双兼容 → 同槽强升 8.1.3。
//     1161(6.1.1)/1162(7.0.1) 的 modlauncher 无 SecureJarHandler 类，不受此炸点影响，不动。
//     先例：build.forge.gradle.kts pre118 的 asm 9.8 eachDependency 强升。
//  2) mixin 0.8(1161/1162)/0.8.2(1163/1164) 在 prepare 相位经 MixinLaunchPlugin.getClassNode
//     → ITransformerLoader.buildTransformedClassNodeFor 全量 transform 管线回读目标类，
//     重入类恰有 pending mixins 即抛 ReEntrantTransformerError（1162 runServer 复现，
//     MixinProcessor.applyMixins:278 prepare 锁）。1165 的 0.8.4 同配置无此炸 → 五线同代化 0.8.4。
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "cpw.mods" && requested.name == "modlauncher" &&
            requested.version in listOf("8.0.6", "8.0.9")
        ) useVersion("8.1.3")
        if (requested.group == "org.spongepowered" && requested.name == "mixin" &&
            requested.version in listOf("0.8", "0.8.2")
        ) useVersion("0.8.4")
    }
}

unimined.minecraft {
    version(mcVersion)

    mappings {
        // 1.16.1 vanilla 混淆事故（1.16/1.16.1 混淆时 Widget 新增 getter 漏跑混淆器，
        // 1.16.2 修正）：vanilla jar 里字面 getHeight() 与被混淆方法 obf e（官方映射同样
        // 命名为 getHeight，SRG=func_238483_d_）是逐指令相同的重复方法（javap 实证，均
        // return field_230689_k_）。searge→mojmap 联合重映射时两者都落到 getHeight →
        // tiny-remapper target-name 冲突（"Mapping target name conflicts detected"；
        // TinyRemapper.handleConflicts 的 targetNameCheckFailed 分支无视 ignoreConflicts
        // 必抛，tiny-remapper 0.8.7 TinyRemapper.java:864 实证）→ 1.16.1 官方 mojmap
        // 本身自冲突，任何 mojmap 工具链都过不去。解法=stub 把字面方法指到别名
        // ysmGetHeight1：编译期 getHeight 由真方法（func_238483_d_）提供，别名成员
        // 运行时无人引用；发布 remapJar（mojmap→searge）中 getHeight→func_238483_d_
        // 反向映射唯一化，SRG 运行时（getHeight 与 func_238483_d_ 并存）两名字都有效。
        if (stonecutter.current.version == "1.16.1") {
            stubs("searge", "mojmap") {
                c("net/minecraft/client/gui/widget/Widget", "net/minecraft/client/gui/components/AbstractWidget") {
                    m("getHeight;()I", "ysmGetHeight1;()I")
                }
            }
        }
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

// ===== MixinExtras 注解 refmap 开关（m2-prod-refmap-1165）=====
// 根因：unimined 1.4.1 的 refmap 不是编译期 Mixin AP 产物（build/classes 无 refmap 文件
// 实证），而是 remapJar 阶段 MixinRemapExtension.RefmapBuilderClassVisitor（tinyremapper
// mixin 扩展）分析 mixin 类注解字符串自产；默认只注册 base Mixin 注解的 visitor，
// MixinExtras 注解（@WrapWithCondition v2/@WrapOperation/@ModifyExpressionValue 等 9 种，
// MixinExtra.refmapBuilder 注册表 javap 实证含 Lcom/llamalad7/mixinextras/injector/v2/
// WrapWithCondition;）需要 enableMixinExtra() 显式追加——modifyRefmapBuilder 是
// 旧 visitor →新 visitor 的组合（追加语义，base mixin 覆盖不丢）；全 unimined 只有
// NeoForged/Fabric transformer 按「loader 是否自带 MixinExtras」自动开，ForgeLike 不开。
// 缺口实锤：生产 1.16.5 崩溃 tmp/crash-2026-09-11_11.16.02-client.txt——
// client.EntityRenderDispatcherMixin @WrapWithCondition(method="render") 在 SRG 运行时
// 无 refmap 条目可查（类名经字节码重映射侥幸正确，方法名字符串原样）→
// InvalidInjectionException APPLY 即崩。dev 全 mojmap 名故测不出。
// 先例：GTNH/Celeritas forge1710 build.gradle tasks.named("remapJar"){ mixinRemap{...} }。
tasks.named<xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask>("remapJar") {
    // 注：DSL 是 receiver 风格 lambda（MixinRemapOptions.() -> Unit），enableMixinExtra()
    // 直接以隐式接收者调用
    mixinRemap {
        enableMixinExtra()
    }
    // 跨版本测试 harness 只进 dev run classpath，不进生产产物（acceptance：unzip -l
    // 零 rip/ysm/harness 条目）；driver 生产侧另有 harness.armed 标记守卫，双保险。
    // 注意：不能排 jar 任务——1.16.5 runClient 走 *-dev.jar（jar 产物），排掉会把
    // runClient 的 driver 一并杀掉；只排 remapJar（SRG 发布产物）。
    // RemapJarTask 接口本体无 CopySpec（编译错 receiver mismatch），内容排除须经
    // JarInterface.asJar 取底层 Jar（AbstractRemapJarTask extends Jar）——unimined
    // 1.4.1 官方 API（javap 实证接口层次）。
    asJar {
        exclude("rip/ysm/harness/**")
    }
}

// runServer 控制台 stdin（harness/tour.sh 和平启动注入通道）：JavaExec 默认
// standardInput 为空流，daemon 下 Gradle 不自动转发，须显式接管 System.in。
tasks.named<org.gradle.api.tasks.JavaExec>("runServer") {
    standardInput = System.`in`
}

dependencies {
    // JOML：共享源 geckolib3 渲染/动画栈 49 文件 import org.joml（MC 1.19.3 才内置，
    // 1.16.5 类路径缺失→"程序包org.joml不存在"）。选 1.10.5（=MC 1.20.1 自带版本，
    // API 与共享源口径一致）；字节码 major 46（Java 1.2），Java 8 运行时可直接载入。
    //（与 work/m2-render-pipeline-condition a2183c9 同行同内容，合并自动收敛）
    implementation("org.joml:joml:1.10.5")
    // ImageStream：不再走 JitPack 依赖（产物 major 61，Java 8 运行时载入即崩），改为
    // src/imagestream 源码 vendor 由本线 Java 8 工具链编译（见 sourceSets 注释）
    // MixinExtras：保留（查证记录，m2-gate-compat）——本 mod 自有 mixin
    // EntityRenderDispatcherMixin 用 @WrapWithCondition（mixinextras v2 注解），且它
    // 注册在 1.16.5 有效的 yes_steve_model.mixins.json（platform/forge 第三方
    // accessor 配置才是被闸对象）；运行时注入方式（内嵌/伴生）未定，本卡只保编译
    // MixinExtras：生产 jar 内嵌（embedMixinExtras，m2 决策=JIJ）；1.16.5 MixinTweaker.onLoad
    // 在配置加载早期调 MixinExtrasBootstrap.init()（<1.17 条件化）→ dev 运行时也必须可见，
    // 故 implementation 而非 compileOnly（compileOnly 不进 dev classpath，runServer/runClient
    // 即 NCDFE，本轮实测）；与 joml/imageStream 同款先例：implementation+生产内嵌并存。
    implementation("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    // jsr305（javax.annotation.*）：1.16.5 mojmap jar 缺 mcp 注解同源的 nullness 注解——
    // forge 侧类签名引用 ParametersAreNonnullByDefault 等，javac attribution 必需
    //（编译期依赖，compileOnly 不进产物；forge 1.16.5 userdev 同款 3.0.2）
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
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
        // shim 落点说明：srcDir 根即 rip/ysm/compat 包目录（文件相对路径只剩文件名），
        // 天然不命中下方 "rip/ysm/compat/**" 排除模式——否则排除会把版本独有 shim 一并
        // 杀掉（shim 与被排除源同包同名 FQCN，靠 sourceSets 级 exclude 与生成树互斥）。
        // shim 内容 = 门面签名镜像 + mod-absent 返回值，运行时语义与 1.20.1 守卫链缺席分支一致。
        srcDir(vendoredBase.resolve("src/shim/rip/ysm/compat"))
        // ImageStream 源码 vendor（上游 TartaricAlkaline/ImageStream master，Java17 编译）：
        // JitPack 产物 major 61，Java 8 dev 运行时一触发模型加载即 UnsupportedClassVersionError
        // （1.16.5 runServer 实测）——改为本线 Java 8 工具链直接编译（自动 major 52），
        // 语法下移仅 8 文件（箭头 switch→经典 switch、pattern instanceof→cast，语义逐行等价）。
        srcDir(vendoredBase.resolve("src/imagestream"))
        // mcp 注解 stub srcDir（根=mcp 包目录）：unimined 1.16.x mojmap jar 全系缺
        // mcp/MethodsReturnNonnullByDefault.class，而 forge 侧 CapabilityProvider/
        // CapabilityDispatcher/LazyOptional 及 11 个 package-info 的签名引用它
        //（javap+zip 字节扫描实证）——capability 继承链 attribution 需要可解析。
        srcDir(vendoredBase.resolve("src/shim/mcp"))
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
    // shim 落点说明见上方 sourceSets.main.java 块内注释（包目录根 + 排除模式互斥原理）
    // ImageStream 的 javax.imageio SPI 注册文件（2 条）随源码一并 vendor，随主 jar 打包。
    resources {
        srcDir(vendoredBase.resolve("src/imagestream-resources"))
    }
}
tasks {
    processResources {
        // 第三方 accessor mixin（Create/ParCool 桥）：1.16.5 无效且加载即炸 → 不进 1.16.5 jar
        exclude("yes_steve_model_forge.mixins.json")
    }

    // ===== 发布 jar 运行时内嵌（m2-mixin-versioning / m2-compile-green-gate）=====
    // 1.16.5 产物为 unimined remapJar 出的 SRG plain jar，运行时依赖需按 1.20.1 imageStreamEmbed
    // 先例（等价旧仓 JIJ include）并入。四个内嵌项：
    //   1) embedMixinExtras  — 1.16.5 Forge 不自带 MixinExtras（Forge 47.x 起内置）；无它时
    //      @WrapWithCondition 被纯 Mixin 静默忽略。mixinextras-common 0.3.6 运行时类平铺
    //      （字节码 major 52=Java 8，缓存 jar 实测可载入），引导=MixinTweaker.onLoad 的
    //      MixinExtrasBootstrap.init()（<1.17 条件化）。
    //   2) embedJoml         — geckolib3 渲染/动画栈 49 文件 import org.joml（MC 1.19.3 才内置），
    //      运行缺类即 NCDFE。1.10.5 = MC 1.20.1 自带版本，字节码 major 46，Java 8 可载。
    //   3) ImageStream        — avif/webp 解码：不再内嵌（JitPack 产物 major 61，Java 8 运行时
    //      载入即 UnsupportedClassVersionError，runServer 实测），改为 src/imagestream 源码
    //      vendor 由本线 Java 8 工具链编译成主 jar 类（major 52），随 classes 直接入包。
    //   4) embedUnsafe8      — unsafe8 源集产物（zstd/UnsafeUtil）并包，见源集头注。
    // 合并算法（mixin 卡 tmp/m2-refmap-poc 实证：manifest-first/类可载入）：
    // 目标 jar 条目流式原样保留（MANIFEST 保持首位，Forge 用 JarInputStream 探测），
    // 追加条目剔除 META-INF/**/签名文件/module-info，重名跳过（目标优先）。
    fun mergeInto(target: File, extraJars: List<File>, extraClassDirs: Iterable<File>) {
        val tmp = File(target.parentFile, target.name + ".embed-merging")
        tmp.delete()
        val existing = HashSet<String>()
        ZipInputStream(FileInputStream(target)).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                existing.add(e.name)
            }
        }
        ZipOutputStream(FileOutputStream(tmp)).use { out ->
            // 第一遍：目标 jar 原样流式拷贝（保持条目顺序=MANIFEST 首位）
            ZipInputStream(FileInputStream(target)).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    out.putNextEntry(ZipEntry(e.name).apply {
                        method = e.method
                        time = e.time
                    })
                    if (!e.isDirectory) zin.copyTo(out)
                    out.closeEntry()
                }
            }
            // 第二遍：追加运行时条目
            fun putEntry(name: String, content: () -> Unit) {
                if (name.isEmpty() || name in existing) return
                if (name.startsWith("META-INF/") || name == "module-info.class" ||
                    name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA")
                ) return
                out.putNextEntry(ZipEntry(name))
                content()
                out.closeEntry()
                existing.add(name)
            }
            for (jar in extraJars) {
                ZipInputStream(FileInputStream(jar)).use { zin ->
                    while (true) {
                        val e = zin.nextEntry ?: break
                        if (!e.isDirectory) putEntry(e.name) { zin.copyTo(out) }
                    }
                }
            }
            for (dir in extraClassDirs) {
                dir.walkTopDown().filter { it.isFile }.forEach { f ->
                    val rel = dir.toPath().relativize(f.toPath()).toString().replace('\\', '/')
                    putEntry(rel) { f.inputStream().use { it.copyTo(out) } }
                }
            }
        }
        val old = File(target.parentFile, target.name + ".embed-old")
        old.delete()
        if (!target.renameTo(old)) throw GradleException("cannot swap embedded jar in place")
        if (!tmp.renameTo(target)) throw GradleException("cannot promote embedded jar")
        old.delete()
    }

    fun jarFromCompileClasspath(prefix: String): Provider<File> =
        provider { configurations.compileClasspath.get().files
            .filter { it.name.startsWith(prefix) }
            .firstOrNull()
            ?: throw GradleException("$prefix not found on compileClasspath") }

    fun registerEmbed(name: String, prev: Provider<Task>, extraJars: List<Provider<File>> = emptyList(), extraClassDirs: Iterable<File> = emptyList()) = register(name) {
            group = "build"
            dependsOn(prev)
            val target = prev.map { it.outputs.files.singleFile }
            inputs.file(target)
            inputs.files(extraJars)
            outputs.file(target)
            doLast {
                mergeInto(target.get(), extraJars.map { it.get() }, extraClassDirs)
            }
        }

    val embedMixinExtras = registerEmbed(
        "embedMixinExtras",
        named("remapJar"),
        listOf(jarFromCompileClasspath("mixinextras-common")),
    )
    val embedJoml = registerEmbed(
        "embedJoml",
        embedMixinExtras,
        listOf(jarFromCompileClasspath("joml-")),
    )
    val embedUnsafe8 = registerEmbed(
        "embedUnsafe8",
        embedJoml,
        extraClassDirs = sourceSets.getByName("unsafe8").output.classesDirs,
    )

    register<Copy>("buildAndCollect") {
        group = "build"
        // unimined 产物：SRG 重映射 + MixinExtras/JOML/ImageStream/unsafe8 四项内嵌后的发布 jar
        from(embedUnsafe8)
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
    // mixins.json 注入 refmap 键：生产（SRG）运行时 mixin 注解的 mojmap 名→SRG 名必须经
    // refmap 桥接（tacz/bettercombat 生产 jar 同款键实证）；本线 refmap 文件名由 unimined
    // remapJar 生成（yes_steve_model.mixins-refmap.json，jar 内实存）。共享源无此键=生产
    // 注入全数 0/1 失败（1.16.5 dev runServer 实测炸点之一）。dev 下该文件不在类路径，
    // Mixin 报 warning 后直跑 mojmap 名（dev 运行时全 mojmap，javap 实证），语义不变。
    filesMatching("yes_steve_model.mixins.json") {
        filter { line: String ->
            if (line.contains("\"required\": true"))
                line.replace("\"required\": true", "\"required\": true,\n  \"refmap\": \"yes_steve_model.mixins-refmap.json\"")
            else line
        }
        // RenderSystemAccessor 在 1.16.5 为普通工具类（非 mixin，空接口 mixin 在
        // Mixin prepare 阶段报 target type mismatch），从 client 注册列表剔除
        filter { line: String -> line.replace("\"client.RenderSystemAccessor\", ", "") }
    }
    // mods.toml 版本口径：共享源为 1.20.1 事实（loaderVersion/forge=47 系、minecraft 1.20.1），
    // 按本线 forge 大版本/MC 版本替换（1.16.5 MDK 模板口径：loaderVersion "[36,)" / forge
    // "[36,)" / minecraft "[1.16.5,1.17)"；批二 c-1 起参数化——1.16.5 算出值与原字面量逐字节
    // 相同，在产线产物零变化）——不替换则 mandatory=true 的三处声明在本线 forge 上必然拒载
    filesMatching("META-INF/mods.toml") {
        filter { line: String ->
            line.replace("loaderVersion = \"[47,)\"", "loaderVersion = \"[$forgeMajor,)\"")
                .replace("versionRange = \"[47,)\"", "versionRange = \"[$forgeMajor,)\"")
                .replace("versionRange = \"[1.20.1,)\"", "versionRange = \"[$mcVersion,1.17)\"")
        }
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
    // builtin 模型文件清单：1.16.5 生产形态拿不到物理 mod jar（getResource 对包内目录返回
    // modjar:// 协议、code source/IModFileInfo 均无物理路径，见 ServerModelManager
    // extractBuiltinModels <1.17 注），运行时按本清单逐文件 getResourceAsStream 提取。
    // 1.20.1 线不受影响（findResource 目录树语义不变），故清单只在本线产物落一份。
    doLast {
        val builtinSrc = rootProject.file("src/main/resources/assets/yes_steve_model/builtin")
        if (builtinSrc.isDirectory) {
            val relPaths = builtinSrc.walkTopDown()
                .filter { it.isFile }
                .map { builtinSrc.toPath().relativize(it.toPath()).toString().replace('\\', '/') }
                .sorted()
                .toList()
            val out = outputs.files.singleFile
                .resolve("assets/yes_steve_model/builtin/index.txt")
            out.parentFile.mkdirs()
            out.writeText(relPaths.joinToString("\n") + "\n")
        }
    }
}
