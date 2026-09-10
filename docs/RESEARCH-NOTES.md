# 调研笔记（研究卡的持久镜像）

> researcher 产出的研究卡在语义记忆与 KG 之外，在此按日期归档，便于人读。

## 2026-09-09 · RAG 资料源选型与反编译管线

- **反编译管线两代**：
  - 1.16.5+：Mojang 官方映射（proguard 格式，piston-meta 随版本分发）→ SpecialSource 一遍
    重映射 → vineflower 反编译（复用 gregtech6/tmp/bin 的 jar）。
  - 1.7.10/1.12.2：MCP `joined.srg`（maven.minecraftforge.net de/oceanlabs/mcp/mcp/<ver>）
    + `mcp_stable` CSV（39-1.12 / 10-1.7.10）→ 生成 CSRG（obf→MCP 直接映射，方法带 obf
    描述符）→ FART(ForgeAutoRenamingTool 1.1.2) 单遍重映射 → vineflower。
  - 教训：FART 的 tsrg 成员行必须带描述符（省略时静默零重命名）；bash `local a=$1 b=...$a...`
    同行赋值先展开后赋值（b 拿到旧值），依赖同 statement 变量的声明必须分行——首轮
    1.12.2/1.7.10 因此串版本，已用 CSRG 单遍管线重跑修复。
- **跨库索引移植**：brain 的 chunk 主键 = (source, path相对source根, ord)，向量与文本上下文
  前缀在索引时烘死。只要新库注册同名 source key、文件树相同相对路径、mtime/size 一致
  （cp -a 保留），即可从 gregtech6 rag.db ATTACH 后 INSERT SELECT 移植，index.py 增量扫描
  直接跳过——嵌入零重算。
- **版本代表法**：1.20.5/1.20.6 为补丁关系，取 1.20.6 代表整个 1.20.5 世代；1.21.2/1.21.3
  取 1.21.3；1.21.5~1.21.7 取 1.21.5。Forge 官方在 1.20.4 分支后停更（1.20.5+ 归 NeoForge），
  故 forge-api 终点 = 1.20.4，边界外由 neoforge-api-1206 接棒。

## 2026-09-10 · openysm.cpp（native 渲染库）研究

- **定位**：OpenYSMDev/openysm.cpp（fork: Meow404club，同一 HEAD 3e86bb0）是本项目 Java 侧
  `ysm-core` native 加速库的上游 C++ 实现：单文件 `dllmain.cpp`（SIMD/SSE+AVX2、GPU 顶点
  路径，sse2neon 兜底 ARM）+ `build.zig`（Zig 构建）+ third_party（jni 头、sse2neon）。
- **JNI 对接面**：导出 10 个函数，Java 锚点 = `common/.../geckolib3/geo/render/built/GeoModel.java`
  的 native 方法（nInitModelCache/nComputeModelVertices/nBuildGpuMesh/nComputeBoneMatrices…）。
  ⚠️ 上游 dllmain.cpp **没有** `nInitSIMD` 导出，而本仓库 Java 声明了它且现有打包 so 带 11 个
  符号——上游与本仓库 Java 侧有轻微漂移，换用上游构建需补 nInitSIMD 或裁剪 Java 调用。
- **构建（本地已验证）**：本机 zig 0.16.0，`zig build -Doptimize=ReleaseFast` 单命令出
  linux-x64 `libysm-core.so`（242KB/10 JNI 符号）并成功交叉编译 windows-x64 `ysm-core.dll`
  （导入表仅 UCRT api-ms-win-crt-* + KERNEL32，无 libgcc/msvcrt 依赖）。平台矩阵：
  windows-x64/x86、linux-x64、macos-x64/arm64、android-arm64(需 NDK)。
- **Java 侧装载链**：产物放 `common/src/main/resources/natives/<platform>/`，
  `NativeLibLoader` 按 OS/arch/libc（JNA 探测 gnu/bionic）解包到 `~/.ysm` 或 `%TMP%/ysm`
  后 System.load；开关 `GeneralConfig.USE_GPU_RENDERER`/`USE_COMPATIBILITY_RENDERER`，
  环境变量 `YSM_CORE_LIB` 直指、`OYSM_DISABLE_SMID` 禁用。CPU 无 AVX2 时 JNI_OnLoad
  直接返回 JNI_ERR（load 失败→走兼容渲染路径）。
- 源码副本：`tmp/harvest/openysm-cpp/`（build 产物 zig-out/ 同目录）。

## 2026-09-10 · StonecutterTemplate 调研（researcher 研究卡）

- **覆盖面**：模板官方 Fabric 1.14+ / Forge 1.17+ / NeoForge 1.20.5+。Forge 1.20.1 走
  `net.neoforged.moddev.legacyforge`（官方声明仅支持 Forge 1.17~1.20.1）；**1.16.5 不在
  模板支持范围**，需自写 buildscript（FG4/5 或 parchment-loom），对模板 Gradle 9.2.1 的
  兼容性未验证 → **1.16.5 必须 POC 先行再定案**。
- **组织方式**：每个「MC版本×加载器」= 一个 stonecutter 版本项（`vers("1.20.1-forge")`
  + 独立 `build.forge.gradle.kts` + `versions/<mcver>-<loader>/gradle.properties`）；
  源码只有一份共享 `src/main`，加载器差异用 `constants.match(loader)` 生成常量 +
  `//? if forge {` 条件块（现版语法，旧版 `/*?*/` 行内写法仍支持）。预处理挂在
  processResources/编译前，每版本生成源码变体。
- **迁移路径**：官方文档明示 "从模板新建工程，把 src 挪进去"；@ExpectPlatform 抽象层的
  社区常见做法是用 stonecutter condition 平铺替代，architectury-api 依赖可留可去。
  stonecutter 插件稳定版 0.9.8（模板用 0.7），Gradle 9.2.1，fabric-loom 1.11，MDG 2.0.141。
- **同版本 Forge+NeoForge 双生态**：1.20.1 NeoForge(47.1.x) 与 Forge 47 同 API 面，可
  注册两个版本项各出自建脚本；1.20.4+ 只有 NeoForge。
- **案例**：elytra-trims（分加载器脚本）、YACL（Modstitch）、PatPat（24+ 版本）；
  未检索到 1.16.5×Forge 的 stonecutter 公开先例。
- 模板源码已收割 `tmp/harvest/stonecutter-template/`（settings/stonecutter/build.*.gradle.kts
  全量，迁移期反复参考）。
