# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。2026-09-11 全量重写（历史行漂移清理）。

## 阶段
- phase: 架构重构（Stonecutter 迁移）——M3 全谱平铺进行中（M2 已收官）
- done: [M0 骨架+源码合并, native 子模块, M1 八卡全过, M2 十一卡全合入, M2.5/M2.6/M2.6.1 GUI 修复链, harness ccb6b23, 内置模型同步 ce1aaf9, M3 批一 ebc4427 + 批二a a129edc + 批二b 07e171e + 批二c-1 5c5888f, M2.7 8444dbf, reobf 撞名修复 d14e610]
- current: 批二c 剩余接手卡 m3-batch2c1b-forge-remainder 进行中（1.16.2/3/4 修绿 + 1.18/1.18.1/1.19 build 补绿 + 双在产线 javap 终验 + compatLevel 表 + 功能差入账）
- next: 批二c-2 neoforge 八线 → 26.x 适配卡 → FPM/RealCamera 修复卡（B1）→ Iris 系保性能重推导三卡 → 生产发布卡；完整队列见 state:tasks.handoff-2026-09-14

## ADR 摘要（decisions.adr-stonecutter-2026-09-10 + adr-m2-1165-stonecutter-entry）
- stonecutter 0.7 + Gradle 9.2.1 单仓；路由：forge ≥1.17 → legacyforge(MDG 2.0.141)，<1.17 → unimined 1.4.1（Celeritas 生产先例）；NeoForge 1.20.5+ → moddev（M4）；1.20.1 一 jar 双跑 NeoForge 47.1
- architectury-api 彻底移除（M1 门禁三零命中）；cardinal/forge_config_api_port 已删（仅 fabric 用）
- 目录：根 settings.gradle.kts/stonecutter.gradle.kts/build.forge.gradle.kts/build.unimined.gradle.kts + versions/<mc>-<loader>/；共享源码 src/main；fabric/ 保留不挂 sourceSet（13 文件 net.fabricmc 为声明例外）
- 条件规范（钉死）：唯一合法块条件 `//? if <1.17 {`…`//?}`；共享源恒 1.20.1 展开态（活跃裸写/非活跃预包或整行 //）；JSON 禁条件；!= 不支持；嵌套 /^ ^/；详见 docs/RESEARCH-NOTES.md
- JNI：GeoModel+natives 冻结；矩阵新知：1.16.5 moj store=GL 列主序等价 JOML get(float[])，JOML 字段名数字位=（列,行）序（KG #90）
- 里程碑：M2 收官（余 compile-green 复审+native-poc+smoke-gate）→ M3 全谱平铺（1.16.5+ 全版本，maven 官方清单枚举，地图玩家友好）→ M4 NeoForge 1.20.5+ → M5 1.12.2/1.7.10

## 任务板摘要
| slug | status | 产物/分支 | note |
|---|---|---|---|
| rag-bootstrap | merged | cfeaae1 | 34 源 407308 块 |
| m0-stonecutter-skeleton | merged | e9f0b61 | 双验收过；遗留 license=TODO 已在 purge 解决 |
| native-align-src | merged | 7db591f | 子模块 openysm-align@af8f642（已推 fork）；对拍 10/10 |
| m0-merge-sources | merged | d086af3 | 1749 文件 R100 平移；natives 逐字节校验 |
| mig-capability / network / events-common / compat / registry-config / events-client / platform-util / purge | merged | 70ae2cc/7cc7c48/2a6f219/9f6d998/8657f78/d5362f8/7414160/0f81660 | M1 八卡 8/8 approve 零打回 |
| m2-skeleton-unimined | merged | 3483e9d | 1.16.5 版本项+unimined 路由 |
| m2-java8-downshift | merged | acd46c8 | 116 文件次；12 处语义等价抽查全过 |
| m2-gate-compat | merged | c5606ae | 一次打回（srcDir 错位）修复后过 |
| m2-forge-thinlayer-condition | merged | 8f23248 | 64 条件块合规；协议字节零变化 |
| m2-mixin-versioning | merged | 5b6d3ac | 18 条版本化；两条任务卡断言被证伪 |
| m2-gui-hud-port | merged | fa83f37 | YsmGui 双轴门面；GPU 三降级；映射 3/3 双源命中 |
| m2-render-pipeline-condition | merged | ea9bfb3 | 打回一次（方向争议）：coder 成立，真 BUG 仅 normal() 反射路径 |
| m2-compile-green-gate | merged | 2f860aa | 双版本绿+四内嵌（JOML111/ImageStream391/mixinextras108/unsafe8）+双冒烟；四处语义修复 1.20.1 零回退逐项实证；ImageStream vendor 240 文件与上游一致 |
| m2-native-poc-1165 | in_review | work/m2-native-poc-1165 | 53bb77c：**GO**——@Unique 桩字段方案，SIMD vs 兼容 1236 顶点 0 失配；审查中（③/5） |
| m2-prod-refmap-1165 | merged | 4634082 | 根因=unimined refmap 非 AP 产物+MixinExtras 需 enableMixinExtra()（ForgeLike 不自开）；生产 A/B（A 复现崩溃/B 330s 到主菜单）；1.20.1 惰性炸弹诊断归生产卡 |
| m2-native-poc-1165 | merged | 68f50bb | **GO**：@Unique 桩字段方案，三路径 1236 顶点 0 失配；机制链亲证（g_modeFieldID 唯二不读） |
| m2-native-poc-1165 | queued | - | GO/NO-GO；联验输入：GeoModel.initSIMD 传 VertexFormat.Mode.class 1.16.5 编不过（native 期望 int-mode，安全降级门槛已证） |
| m2-ingame-smoke-gate | queued | - | 双版本进世界终验（含模型不变形视觉核验） |
| feature-debts-1165 | ledger | - | 6 项功能差（blur/iris/tooltip/extraplayer/biome-molang/compat-matrix），M2 收官后排期发卡 |
| poc-forge-1165 / poc-execute-unimined | done | - | unimined 路线定案（三代 POC 全 PASS） |
| native-openysm-cpp | closed | - | 自带实现完整；上游仅档案存查 |
| harvest-curator-1/2 | merged | - | stonecutter-template+stonecutter-src-07+celeritas-mva 入 RAG |

## M2.5 GUI 全量排查修复（tasks.m2.5-gui-full-fix，已合入 dev=39db64a，2026-09-11）
- S1 Z 轮盘崩溃：Pie 1.16.5 分支 rewind 在 GL 填充之后，第二次 drawSlice remaining=0 IAE——修复=填充前 clear()（5d52a31），>1.17 分支零改动（1.20.1 jar javap 逐指令 0 diff 实证）
- S2 Alt+Y 按钮空白：1.16.5 vanilla Screen 双列表（addButton=渲染+事件、addWidget 仅事件不渲染），ysmAddWidget 桥误用 addWidget——修复=instanceof 分流：AbstractButton→addButton、纯 AbstractWidget→addWidget+ScreenAccessor 入渲染列表（bc7c9b8）
- 审查裁决 MERGE：refmap 疑云解除（ScreenAccessor 条目在 refmap mappings 层，buttons→field_230710_m_ 与真实安装器 Screen 逐字命中）；生产安装器 6 次启动 0 mixin 失败至主菜单+进世界（~/m25-prod-audit/shots/）；15/15 屏走查过
- **构建陷阱入册**：1.16.5 生产形态必须 `buildAndCollect`（裸 remapJar 缺 MixinExtras/JOML/unsafe8 内嵌，MixinTweaker onLoad 即 NCDFE）；harness 重建脚本+README 在 tmp/refmap-audit/
- 教训：xvfb+llvmpipe 下进程内 glReadPixels 截图 100% 制造 native 堆损坏假崩溃——测试一律外部抓屏

## M2.6 GUI 逻辑修复（tasks.m2.6-gui-1165-logic-fixes，已合入 dev=a710a8c，2026-09-13）
- tag 炸弹根因：目录复数格式本就正确，缺 crossbows.json 文件（ItemTagsConstants 引用无资源）——补 `{"values":["minecraft:crossbow"]}`（双轴 vanilla 均无 crossbows 标签，直列单物品是唯一正解）+ YsmTag.matches <1.17 未绑定守卫（无公开 isBound，try-catch ISE→false）
- s1/s3/s2 同根：1.16.5 无 clearWidgets，导航/翻页/轮盘回调原地重入 init() 控件无限累积——两屏 init 头部块形式补 vanilla 清场三连（生成树实证活跃非死注释），1.20.1 轴零改动（javap 11/11 全同）
- **订正**：1.20.1 标签目录也是复数 tags/items（单数化是 1.21+），mod 标签在 1.20.1 正常绑定——此前"静默失效"判断证伪，无修复卡
- 待办：其余九屏重入 init 同型坑未查（SPEC 外）；用户实机复测三症状

## M2.6.1 翻页动画重刷（tasks.m2.6.1-page-anim-reset，已合入 dev=62de96e，2026-09-13）
- 翻案：非 M2.6 副作用——每轮模型 flush 广播 onModelsUpdated→init() 整页重建→resetModel 清动画时间轴，M2.6 之前同码已存在，修掉幽灵卡后显性化
- 修复：增量回调只更新 pending→loaded/同 key 换 assembly 槽位（动画时间轴保位），结构变化回退 init() 保 M2.6 清场；旧屏实例守卫；ModelButton pending 缓存抽幂等刷新
- 审查：根因链逐环命中；判据穷举闭合；seekTime 断言（增量 191→191/正控制 292→0）；1.20.1 同病同修
- 发布卡观察项：懒加载 LRU 驱逐（>64）可令可见槽瞬时回 pending，自愈但宜观测

## 内置模型同步 2.6.5（tasks.builtin-sync-265，已合入 dev=ce1aaf9，2026-09-13）
- 367 文件内容更新（default 26/misc 31/wine_fox 310）+ boat.json 标签补丁（上游改 #tag 语法但两轴 vanilla 无 boat tag，补 tag 恢复载具匹配），资源与上游逐字节一致
- 双线 tour 14 屏全过、27 模型零解析错误；双构建绿

## 跨版本通用测试 harness（tasks.cross-version-harness，已合入 dev=ccb6b23，2026-09-13）
- 交付：harness/tour.sh orchestrator（--no-daemon+setsid+PIDFILE/PGID 阶梯清理、和平启动五命令 stdin 注入、版本参数化 case 表单点）+ 零 GL GuiTourDriver/HarnessScreens（cmd.txt→harness.ready，14 屏）+ YsmEventBootstrap 3 指令守卫挂载（惰性解析实证）+ 双 buildscript 生产 jar 排除
- 实证：双线 tour exit 0/16 屏/零崩溃零手工；双产物 jar 零 harness 字节；1.20.1 javap 1661/1662 类全同；生产安装器实启 25s 到主菜单全日志零 harness 引用
- 新增版本线接入约定：tour.sh case 推导表 + buildscript runs.gameDirectory 对齐 + options.txt onboarding 项核对（1.17~1.19 待核）
- 遗留 P1 已修（dev=4983cbb）：cleanup FIFO 写入 timeout 包裹；runs 拆分后旧 run/saves 不自动迁移（冒烟改走 tour 自管世界）；crash 快速失败覆盖窄
- 发布卡新增：主仓 build/libs/2.6.6.6/ 是 mojmap 陈旧副本（真基线在 versions/1.20.1-forge/build/libs/，SRG 形态），发布前必重建

## M3 第一批 forge 四线（tasks.m3-batch1-forge-lines，已合入 dev=ebc4427，2026-09-13）
- 四线注册+条件轴四段化（<1.17/<1.19.4/<1.20/≥1.20）+ 1.19.2/1.18.2/1.19.4/1.17.1 逐线修绿，9 枚提交（三轮会话接力）
- ASM 事件闭环：dev run 继承 toolchain JVM→mixin 0.8.4+ASM9.1 读 JDK 类（major 65）炸；方案 B=toolchain 21+asm force 9.8（Celeritas/GTNH/forge 官方分支三先例），**JVM21 与 Java25 均实测到主菜单**；生产口径=1.17.1 线声明 Java16-17，发布 gate 加"安装器+Java25+生产 jar"实测档（机制推论装 mod 后 Java≥21 会炸，jarJar 不可达）
- 双在产线验收口径演化为"语义零变化"（字面逐字节不可达：条件注释行必漂移 LNT+zip 序随环境变），审查独立构建复核通过（1165=2454 条目 0 增删/160/161 逐指令全同）
- 遗传入账：1171 init 重入清场债（trio<1.17+clearWidgets≥1.18.2 组合解，1171 空档段不清场）；RenderArmEvent/ShieldBlockCooldown/RegisterClientCommandsEvent 三事件功能差；中段线生产 jar 内嵌 MixinExtras（forge 37.1.1 无内置）归发布卡

## M3 第二批 neoforge（2a 已合入 dev=a129edc，2026-09-13；2b 进行中）
- 2a：1.20.4/1.20.6/1.21.1 三线全绿+16 屏走查进世界；moddev 构建线建成（MDG2 neoforge、分代树 srcDir、neoforge.mods.toml、[[mixins]] 声明、零 refmap 直配、compatLevel JAVA_17/21/21）；双在产线对基线逐类 javap+资源全同（4 处回归抓回修复）；TouhouMaidCompat @OnlyIn 中立化裁决通过（专用服更安全）
- 2b（已合入 dev=07e171e）：21.3/21.4/21.5/21.8/21.10/21.11 六线绿+主菜单证据；26.1.2/26.2 park（NFRT 由 MDG 2.0.147 解除，剩自身代号适配 26.1.2≈100 错 GuiGraphics 移包/26.2≈100 错 TextureFormat 移包）→26.x 适配卡（26.1/26.1.1 同代一并）
- 批二 c：2c-1 **已合入 dev=5c5888f**（2026-09-14，rebase 8444dbf 后四提交 0dd7641/83c8b5d/f2ecf2f/01754ff 重签）——十线注册（forge beta/latest 六线 + unimined 1.16.1~4）+ 1.16.1/1.18/1.18.1/1.19/1.19.1/1.19.3 六线修绿；审查修正 b59cf0c：1.18/1.18.1 RenderSystem 三断言按 merged jar javap 实证恢复激活（coder 曾整体误砍）、20 处恒假条件死码清除；剩余（1.16.2/3/4 修绿、1.18/1.18.1/1.19 build、双在产线 javap 终验、compatLevel 表）归接手卡 m3-batch2c1b-forge-remainder
- 批二 c 接手卡 **已合入 dev=0478ed2**（2026-09-14，5 提交+审查修正 7b2a6c9）——1.16.2/3/4 修绿（双分界实证修正：RenderArmEvent=forge 36 才有、scissor=1.16.4 起）+ 1.20 线修绿（forge46 renderWidget public abstract 专属段）+ 1.16.1 回归修复（m2.7 getCameraType→CameraUtil 反射门面）+ 1.20 mods.toml 参数化（生产拒载缺陷）+ tour.sh 十四线路由；**终验三层独立复验**：1201/1165 对 5c5888f 零 diff、1165 对 8444dbf^ 恰 5 类=m2.7 预期集，零泄漏（证据 tmp/evidence/rv-m3b2c1b/）；compatLevel 表 1.16 系=JAVA_8/中段=JAVA_17；审查修正=CameraUtil thirdPersonView null 守卫（NPE 隐患闭环）。**forge 侧批二c 至此收官**
- 批二 c-2 **已合入 dev=43e66fc**（2026-09-15，14 提交，重派后零新机制合规交付）：**五线全绿**（1.21/1.21.2/21.6/21.7/21.9，21.6 四断裂修复含 21.8 反射形矫正）；**POC 判负三线**（1.20.2/1.20.3/1.20.5 maven 无 .module 元数据，MDG capability 解析必败，撤注册；NFRT 直驱另立项 tasks.m3-nfrt-direct-drive-poc）；**P0 回归修复**（批二c-1 83c8b5d capability 嵌套行条件链致现役 neoforge 全线必炸，审查在 dev@10aa700 亲跑复现，073f542 平铺修复）；分界前移三批（21.3→21.2/21.8→21.6/21.10→21.9，逐点 sources 实证）+212/net16/net18 分代树
- **审查证伪三项运行期声称**（同根=dev 存量 server-dist 缺口，非本卡引入）：21.9 runServer NCDFE 崩（sendUnavailableMessage @OnlyIn 门 `neoforge && <21.9` 遗留，21.9 落 >=21.9 掉出 DistCleaner 保护）、21.7 loader10 警告屏阻断、21.8"复验"实为崩溃误读 BUILD SUCCESSFUL；21.10/21.11/26.x 现役线推定同病（runServer 从未真验过）。**教训：BUILD SUCCESSFUL ≠ 走查通过，必须 grep `Done(`+日志内容+数 png**。候选卡 tasks.m3-neoforge-server-dist-fix（P1，发布卡前必修，动共享源须重做双在产线终验）
- 方法学新沉淀：等价基线法成立（同环境构建基线 src/main 对照）+ 生成树活跃代码对比法强于 javap（可精确定位泄漏行）；slashblade 双 jar classpath flaky 属环境级既有问题
- 批二 c-2 遗留环境卡：unimined 线 dev-run 三症（1.16.2 mixin Re-entrance/1.16.3-4 SecureJarHandler NSME/1.16.1 进世界断连）；批二c1b 遗留债 debt-forge-linegen（1.18.1 RenderArmEvent 存在被 >=1.18.2 门排外，一行可修）

## server-dist 修复卡（tasks.m3-neoforge-server-dist-fix，已合入 dev=7e5a2d9，2026-09-15）
- 机制级诊断（审查独立反编译复证）：新版 loader（21.7/21.8=9.0.14、21.9+=10.0.14）已删 RuntimeDistCleaner 成员剥离，仅存 NeoForgeDevDistCleaner 掩码，@OnlyIn 失去保护力；崩溃=HotSpot 类校验期形参收窄解析（LocalPlayer 实参→Player 形参触发层级加载 CNFE），三路径 YSMForge <init>:48/NetworkHandler register(6)/(8)；instanceof/checkcast 不触发
- 修复：>=21.7 专用服 client 引用结构性隔离（主体移仅 client 加载的 ClientModelManager）+ rlToIdentifier 剥离门 >=21.8 下探 >=21.7 + 21.11/21.9 运行时存量断裂修
- runServer 全谱审计 14/16 Done(（26.1.2/26.2=compileJava 移包定性归 26.x 卡）；21.7/21.9 补齐真 tour 证据
- 审查修正 aaf7796：YsmGui blur fill 门 <21.10 扩 <21.11——21.10 史上首次真 GUI 走查即崩（批二c-2 的"21.10 无限制"声称系误证，批二b 证据为陈旧 jar 主菜单图）；>=21.11 vanilla renderBackground 分支仍未走查（debt-216-blur-degraded 勘误在案，26.x tour 首踩）
- 双在产线终验：1201 2918=2918/1165 2872=2872 条目全同，CRC 差类 javap 去 LNT 全 SAME（注释位移噪声）；教训沉淀：BUILD SUCCESSFUL ≠ 走查通过（必须 grep Done(+数 png）

## 1.20.1 渲染双根因修复（tasks.debug-1201-windows-gpu，已合入 dev=7715a44，2026-09-15）
- 用户 Windows 真机报告模型全不渲染（纸娃娃正常/模型卡坏/世界坏；兼容渲染器救世界不救模型卡）→ 两层根因：
- ①**模型卡预览从未参与编译**（所有环境，非 Windows 专属）：1201 vcs 直通线直编 src/main 原文，`//? if`+`// 代码` 行形式恒死注释（生成树不消费）——ModelPreviewRenderer 预览配方块从没进产物，画原点被 scissor 裁掉；纸娃娃是裸码所以正常。**教训（新铁律）：1201 应活的代码必须裸码或块形式，行条件+注释形式在根节点恒死**；同模式死行全仓 7 文件全转裸码（6 文件+GeckoProjectileEntity tickModel 防御），顺修投射物 12s NPE。审查逐文件核 1165 生成树+产物 javap：FALSE 线操作码零差（唯一有意变更为防御性 tickModel）
- ②**世界全黑=光影包双失败**（仅真 GPU 显形，llvmpipe 假阴性全绿）：Oculus/Iris 在用时 IrisRenderPath 直绘，Iris 管线内 getShader() 返回包装 shader+G-buffer 绑定、真驱动画空但恒 return true 吞回退；回退链 SIMD 直写在 Embeddium BufferBuilder 下也坏（模型巨大化）→双失败必黑。修复=943ff31 光影包在场时回退 CPU 缓冲（官方 IrisApi 检测，无光影包场景零变化；IrisRenderPath 实现保留+勿删标记）——**止血**，真修=debt-iris-shader-compat（1201 线真修+D4/D2/D3，保性能重推导）
- 前情：首派 debugger 排查 62de96e..4fd6c87 窗口**无代码回归**（jar 首尾对拍+audit 零 BROKEN+四场景 llvmpipe 全绿）——回归假设被证伪，用户情报（仅 Windows 复现）转出真凶
- 新原则：**llvmpipe 盲区**——渲染路径改动真 GPU 未验证前不得视为已验证；Windows 回测是渲染/兼容卡验收硬项
- FPM/RealCamera 修复卡已按用户裁决叫停销毁（4d6a2c5 随 worktree 销毁）：其症状观测疑全为 GPU bug 下游症状，1.20.1 修好后重做诊断；FPM 系路径冻结（debt-onrenderhand-iscanceled-frozen）
- 批二 c-2（待发）：neoforge 8 条（1.20.2 POC/1.20.3 POC/1.20.5/1.21/1.21.2/1.21.6/1.21.7/21.9）
- 全谱 37 线；semver 铁律（stonecutter 版本 ID 数值比较，分代用 <21.5/>=21.5 风格）与 vcs 直通铁律（1.20.1 根活动节点，21 轴门控必须存储态）为平铺期两大新沉淀
- 2a：1.20.4（20.4.x stable）/1.20.6（20.6.x）/1.21.1（21.1.x）——moddev 构建线首次建立（MDG neoforge），Java 17/21/21
- 2b：21.3/21.4/21.5/21.8/21.10/21.11/26.1.2/26.2（Java 21→25）
- 已知要点：NeoForge 1.20.2+ 运行时=mojmap（无 SRG reobf，mixin refmap 口径待验证）；1.20.5+ neoforge.mods.toml/Component 体系；26.x 需 Java 25 toolchain

## 2026-09-14 会话四合并（dev→5c5888f）
- **reobf 撞名修复 d14e610**：自有接口方法名与 vanilla 方法同名时，legacy reobf 把调用点误映射到 SRG 名→NoSuchMethodError。IAudioPlayer.isStopped→hasStopped（isFinished 与 vanilla WorldUpgrader 撞名被否）；YSMTickableSoundInstance <1.17 走状态位（vanilla TickableSoundInstance 覆写必须保名）。审计脚本 tools/audit_reobf_collision.py（扫产线 jar 中 owner=自有类的 SRG 引用，三档 OK/INHERITED/BROKEN）→发布门禁
- **gradle OOM 纪律 841aab3**：全项目 parallel=false + workers.max=2（33 线并发实测 20G+ OOM）；个人覆盖走 ~/.gradle。一次 gradle 调用一条命令，版本测试一律串行
- **M2.7 覆盖层修复 8444dbf**（1.16.5 纸娃娃三连）：根因三缺口=ExtraPlayerOverlay 空方法+死注释+注册只 Post(DEBUG)（F3 门控，ForgeIngameGui:208 证实 post(ALL) 才是无条件末点）→<1.20 真实现+MPR <1.17 配方（对照 vanilla InventoryScreen.renderEntityInInventory:101-138）+Post(ALL)；LazyModelAssembly getAnimationBundle SOE 守卫（fallback 崩溃路径改返 null，正常路径透传）；ExtraPlayerRenderScreen 拖拽预览 <1.20 启用。1.20.1 字节码零变化（javap 复验）
- **兼容诊断定案（未动手，修复卡待发）**：FPM=SIMD 天然在链零动作、藏头正常，真实症状=合作分支 offset(1.5-cameraDistance) 对大模型穿模；次级 bug=onRenderHand 不查 isCanceled。RealCamera=双阻塞（贴图 id 不在默认 BindTarget 清单+GPU 直写绕过捕获器），修法 B1=RealCameraAPI.registerFunction 从 viewLocatorBone/headBones 直产 BindResult，**硬约束：绑定 GUI 手动覆盖必须保留**；调试工具 tmp/compat-debug/
- **兼容不降性能原则**：Iris 系重推导三卡（D4 neoforge 真 compat→D2 21.2~21.4 改名移植→D3 21.8+ proj/fog 捕获复活 GPU 路径）；硬上限保留 D1/D6/D7；"检测到 mod 即降级"模式一律重新推导

## M3 平铺总纲（tasks.m3-flat-tiling，矩阵已定）
- **17 行必铺矩阵**（tasks.m3-matrix-research，官方 maven 证据）：forge 6 行 1.16.5/1.17.1/1.18.2/1.19.2/1.19.4/1.20.1（legacyforge，1.16.5 走 unimined）+ neoforge 11 行 1.20.4/1.20.6/1.21.1/21.3/21.4/21.5/21.8/21.10/21.11/26.1.2/26.2（moddev）；1.20.1 一 jar 双跑（neoforge fork 47.1.106 兼容声明）；26.x 需 Java 25 toolchain；短命版官方无 stable 跳过（1.17.0/1.18.0/1.19.1/20.3/20.5/21.2/21.6/21.7/21.9 等）
- **前置：条件轴四段化**（tasks.m3-condition-axis，>1.17 二元轴被证伪）：GuiGraphics=1.20、renderWidget GuiGraphics 签名=1.20、getX/narration 新 API=1.19.3——重排为 <1.17/<1.19.3/<1.20/≥1.20；与 M2.5 同文件域，已解除阻塞
- 相邻补丁版 API 面一致可共享条件组；20+ 条目考虑 settings/properties 生成脚本化

## 功能差债务清单（tasks.feature-debts-1165，M2 收官后排期发卡）
| id | 功能差 | 现状 | 可行性 | 卡 |
|---|---|---|---|---|
| debt-gui-blur + debt-gui-iris | GUI 毛玻璃 + Iris GPU 路径 | 空实现/恒 false | 中：oculus 桥接 vs 自写 shader 合并调研 | debt-gui-blur-1165 |
| debt-gui-tooltip | 暂停页按钮 tooltip | 桥接未接链 | 高：小卡 | debt-gui-tooltip-1165 |
| debt-overlay-extraplayer | ExtraPlayerOverlay | 1.16.5 空渲染 | 中：照 YsmGui 模式 | debt-overlay-extraplayer-1165 |
| debt-biome-molang | biome molang 查询 | 恒 false | 中：category/name 近似，需模型侧确认 | debt-biome-molang-1165 |
| debt-compat-matrix | 第三方 compat 全矩阵 | shim 缺席 | 大：M3 后立项 | debt-compat-matrix-1165 |

## M2 收官记录（2026-09-11）
- 合入链：…→compile-green 2f860aa→native-poc 68f50bb（GO）→prod-refmap 4634082→smoke-gate bf5be3a；11/11 approve（2 次打回均修复过审）
- 收官门禁三层 PASS：1.16.5 dev+生产 / 1.20.1 dev+生产 / 模型不变形视觉终验（12 截图证据 tmp/m2-smoke-gate/）
- 生产四雷修复：builtin modjar:// 提取（index.txt 621 条）/loadDefaultModel SOE/1.20.1 refmap 键激活/InventoryScreenMixin remap 纠正
- tag 裁决：不打 v2.6.6.6-m2——embed 时间戳不可复现，归生产发布卡修可复现构建后打
- 新坑入册：processResources 对构建脚本改动不失效，发版前全清 build 目录

## 生产发布前必查（挂账）
- 1.20.1 生产 jar mixins.json 缺 refmap 键（M1 起既有；需先验证 MDG refmap 与 dev 类路径互作再注入）
- **门禁盲区教训（2026-09-11）**：dev runClient 测不出生产 refmap 缺口——ingame-smoke-gate 必须含生产 jar 启动验证
- JOML 1.10.5 运行期内嵌 1.16.5 产物（compile-green 卡已收口，复核）
- 冒烟 harness（SmokeAutoJoin/init-script）未入库，后续冒烟卡自带

## 最近决策
- 2026-09-11 条件规范钉死：`//? if <1.17 {` 唯一合法（插件源码铁证），已全量广播；swap 闭合符 /*?}*/（lesson#39）
- 2026-09-11 矩阵新知：1.16.5 moj store=GL 列主序等价 JOML get(float[])；JOML 字段名数字位=（列,行）序（方向争议裁决，coder 端到端 Sanity 证伪审查处方）
- 2026-09-11 M3 平铺原则：全版本谱（地图玩家友好）；枚举以 maven 官方清单为准
- 2026-09-10 M3 前置定案：unimined 路线三代全 PASS，legacy/ 文件夹方案作废
- 2026-09-10 架构重构方向：脱离 Architectury 转 StonecutterTemplate；native（openysm.cpp）以子模块接入并完成源码对齐
