# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。2026-09-11 全量重写（历史行漂移清理）。

## 阶段
- phase: 架构重构（Stonecutter 迁移）——M3 全谱平铺收官；FPM/RC/iris/native 四线全清（2026-09-17）
- done: [M0 骨架+源码合并, native 子模块, M1 八卡全过, M2 十一卡全合入, M2.5/M2.6/M2.6.1 GUI 修复链, harness ccb6b23, 内置模型同步 ce1aaf9, M3 批一 ebc4427 + 批二a a129edc + 批二b 07e171e + 批二c-1 5c5888f, M2.7 8444dbf, reobf 撞名修复 d14e610, FPM/RC 修复链 18a91f7→57bb90e, 2026-09-17 批：daff957 五卡（iris 实验支线 d4d314c/26.1 FPM 511ab9b/neoforge Iris 检测 241e683/FPM 颈隐藏 b5f3bf3/RC 变换矩阵 daff957）→ iris memFree e47beed → native 整合 87d552b → RC 锚点 d4850d6 → cleanup 3c81354 → 探针差分 96be7aa]
- current: 无在途分支；dev 顶=96be7aa；用户终测 jar=ea519e08（3c81354，与 96be7aa 仅差 armed 门控差分工具，生产零影响）
- next: D2（21.2~21.4 IrisRenderPath 移植）→ D3（21.8+ proj/fog 捕获复活）→ 卡B（21.2~21.8 FPM）→ 1211 带包 tour 格 → 26.1.2 tour 基线 → 26.3 适配（26.2 submit-dag 前置）→ 生产发布卡

## 2026-09-17 收官纪要（四线全清）
- **FPM 颈三路径隐藏**：根因=setHidden 双写 offset9/10，SIMD 只读 10、GPU selfHidden 不含 9；native offset9 治本+Java 兜底拆除（native 单独证责：差分 harness drift 10/10+semantic 7/7，无补丁态三路径全过）。用户真机确认剔除正常。
- **iris 直绘终审 NO-GO**：真机二次独立失败+绘制侧打点全净（glErr=0/drawCount 正常）→ 空产出=pack 着色器内部动态 uniform（Iris 不开放、RenderDoc 才可深挖）。整路砍除（IrisRenderPath/BoneXformCompute/iris 顶点机制/配置键），实验分支入场券保留在 git 历史。
- **有包路径定案**：光影包在场→CPU（943ff31 语义恢复；用户实测推翻 pack→SIMD 直切）。无包 SIMD/GPU 照旧。
- **native 子模块第一轮**（openysm-align c89cb51…012316e 未 push 归用户）：offset9 治本、非树防御、92B/93B 步长裁决（shipped+Java 为基准）、假绿差分 harness 修复（heap ByteBuffer→GetDirectBufferAddress=NULL 空转）、tools/build-native.sh、六平台产物回填。JNI 12 参协议不变。
- **RC 绑定解终审无罪**：探针真值差分（ armed 门控+审查官毒化背书）全姿势 ≤1.5px、蹲 1616 帧不放大、dStale≈0；蹲/趴感知偏差=同 json 双引擎动画求值差异（用户裁决可接受）。锚点 UV 唯一候选=脸面逐位一致；roll 链 up+翻滚→终态 top=(0,1,0) 零预应用零双重。
- **26.1 三线 FPM 真 compat**：extract 期旗标快照（对位官方 r3 契约）+RealCamera/阴影/GUI 三排除+offset handler 对齐 ViewLocator 语义；stock FPM 降级=官方行为。
- **1.20.1 clean**：26.1.2 四消费面字节码逐指令零漂移（审查独立复核）。
- **教训入账**：JDK allocateDirect×LWJGL memFree 配对陷阱（KG TRAPS）、截图/工具假绿双前科（B1 帧 md5+#106 harness）、增量对≠绝对对（锚点须绝对位置断言）。
- **归用户**：子模块 push（openysm-align 012316e）、RC 上游反馈（DisableHelper || isSpectator 应 &&，0.7.8 同病）、有包 SIMD 已撤故无真机待验项。

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

## FPM/RC 兼容修复（tasks.diag-fpm-rc-symptom-matrix → tasks.fix-fpm-rc，已合入 dev=18a91f7，2026-09-16）
- **重诊断**（渲染修复后，用户症状矩阵驱动，七格 llvmpipe 实证）：FPM=隐藏旗标烤入 boneParams offset9/10，native 只消费 offset10、**CPU 路径可见性判定被注释**（兼容开全不隐）；FPM 只隐 allHeadBone 子树；RC=世界内绑定失败链（initialize 每帧重置 active→computeCamera 失败→2 帧翻假→MixinCamera 早退 vanilla 眼位="滞留身后看背影"）；GUI 矢量只需 weakAvailable≠相机可用
- **逆向反转**：用户授权逆向官方 YSM 2.6.5-1.20.1（tmp/harvest/ysm1201-reverse/），coder 逐行证伪研究卡初读——官方 eval 含 `!event.isFirstPerson()` 门（与我方逐字同构）、offset=ViewLocator 骨属性×scale 动态式（24.0F 仅兜底）、官方异步同硬编码——**OpenYSM 的 FPM 兼容代码本就是官方忠实镜像**，F2①②③ 按"机制对齐官方"跳过=正确执行（审查独立裁决维持）；研究卡 decoded_recipe 三条作废
- **落地三笔**：F1=CPU 恢复 offset9/10 消费（镜像 native dllmain.cpp:667/725/1250，scale AND→OR 官方对齐）修"兼容开全不隐"；B1=RealCameraApiBinder 反射软注册 registerFunction(priority 1000) 骨骼直产 BindResult **挂用户选中 target**（offsets/bindConfig 生效=GUI 手动覆盖保留）+available 屏蔽探针（rc 格 Binding failed 2→0，相机走 bindConfig 微调位）；R3=ModelViewScreen 打开期间强制 CPU 管线（GPU 格 GUI UV 可读）+四端 shim（1165/1211/2111）
- **Trissy 颈判据更正**（用户亲验截图）：颈立方虽在 AllHead 子树外，F1 落地后颈正确隐藏；官方 1.20.1 同机制同表现——用户"闭源 YSM 头颈全剔"参照来自 26.1.2（官方 26.x submit 期隐头新机制，PR #659），26.x 路线另立卡不可平移 1.20.1
- 已知边界：RC+FPM 同开时 AllHead 旗标双写路径（render-thread/烘焙-worker）无锁，与官方同代一致，渲染实证正确
- 官方 YSM 逆向授权与边界（用户 2026-09-15/16）：FPM 方向允许逆向取机制参考，实现自研；身份独立不整体对齐

## Windows 真机二轮修复（tasks.debug-win-gpu-round2，已合入 dev=661927e，2026-09-16）
- 用户回测揭穿首轮验证造假：B1 从未生效（rc_uc_gpu Binding failed×2=失败基线；fp 帧对 md5 相同；rcfpm fp 纯色空屏）——lesson#89 截图验证幻觉（同类二发），验收协议升级=几何特征对照+日志负向计数真实数字+帧 md5 去重，参照图判读以用户文字描述为唯一权威
- **RC 双层根因**：L1=用户 realcamera 0.7.5-beta 的 BindResult 只有 (BindTarget,boolean mirrored) 双参构造，首轮 B1 单参反射 NoSuchMethodException **真机从未注册**（审查亲读上游 commit 8b82d0deec18 源码实证）；L2=llvmpipe 假绿真因=getBindTargetList contains 语义，测试环境动态计数器 textures/1、9 撞不上用户 textures/10（用户环境恒 10 恰好匹配）
- **修复 eaa3d02**：RealCameraApiBinder 构造扫描（首参 BindTarget 最短公有构造+尾参 boolean 兜底，0.7.5/0.7.8 双兼容，无匹配干净失败）+EMPTY reason 限频打点（原 catch 全静默）；rc 四格 0 Binding failed+fp=头锚俯视腰带白裙+帧 md5 互异
- **FPM"颈不剔"重新定性=相机锚点伪影**（无需动 native）：Trissy main.json 亲验 388 骨/AllHead 子树 206 骨/无 neck 骨/颈=UpperBody 顶部立方在子树外且无 first_person_mod_hide 动画——**三路径+官方 1.20.1 全都渲染颈**；"兼容剔/GPU 不剔"=绑定死后相机落位伪影；B1 修好相机回头锚，低头见领口+裙子，颈问题从视场消失。**"颈已修复"预期撤销为"颈保留=官方 1.20.1 同款边界"**（模型侧可用 first_person_mod_hide 动画自行覆盖）
- 已知边界维持：RC+FPM 同开相机贴模型（无锁，官方同代一致）

## RC BindResult 补全（tasks.fix-rc-bindresult-completeness，已合入 dev=e6d5091，2026-09-16）
- 用户回测：L1 生效（注册成功相机动了）但同配置与 RC+官方 YSM 错位——怀疑缺配置消费
- **源码级翻案**：offsets/bindConfig 消费已在官方侧（BindResult.computeCamera:75-83=target 配置唯一消费点，函数/探针结果一视同仁：offsets 位移 position+=R·(z,y,x)·scale、yaw/pitch/roll rotateLocal；MixinCamera:53/57+EventHandler:17 逐轴与 rotation 门）——任务卡"缺一大堆配置消费"假设证伪
- **真根因 M1**：我方基准帧用 bodyRot 原始值（getRawPos 局部坐标直加 entityPos 无旋转）→转头漂移+offsets 世界方向错=用户错位根因；修复=180−bodyRot→180−lerp(yRotO,yRot)（与探针 view yaw 基准同式）
- **结构边界（与官方 YSMCompat 同款）**：顶部矢量/前向 UV 仅探针路径可读（VertexData.normal，YSMCompat:95-96 重渲染采样）；骨驱动路径以模型轴替代，朝向微调走 offsets yaw/pitch/roll（已消费、已进数值打点）
- **数值验收（审查亲算+独立新证）**：GPU 格 Δ=(−0.080,0,−0.149)=R·(z,y,x)·scale 逐分量吻合+euler roll=90；compat 稳态 (0.057,0.003,−0.160) 三分量全吻合（含 1° pitch 项）；审查另起两个不同 yaw spawn 独立采样复核命中；打点限频 3+每 600 帧不刷屏；**用户验收纪律=数值行对照，禁看图**
- 回测注意：用户当前 bindRotation=false，官方门下朝向本就不进视图——验证朝向需先设 true（roll=90 应见视图滚转）

## RC UV 表面点锚点（tasks.diag-rc-preview-anchor-mismatch，已合入 dev=e17ad83，2026-09-16）
- 用户最终报告：相机位置与绑定 GUI 预览**从来没对上过**（跨所有修复轮次）→ 深析坐实**双重根因**：①锚点语义违例——RC 全链（GUI 预览 ModelAnalyser:333-367/探针 RealCameraCore:153-177/函数契约）锚点=UV 命中面重心插值表面点，旧 B1 喂骨 cube 中心（Trissy 量化：posUV=Head 颅 cube north 面中心，表面点距骨中心 3.5px=0.219 块鼻尖向，随姿态摆动）；②offsets 消费帧错位——computeCamera 位移方向由 feed 的 forward/upward 装配，骨轴帧≠GUI 面法线帧
- **修复 cfffa66=方案 A（UV→网格查询）逐字对齐探针**：UV-in-quad 判定（Polygon+1e6 量化同款）+重心插值+命中面法线做 forward/upward+骨变换链同渲染（prepMatrixForBone 祖先链）；/16 烘焙陷阱规避（RawFace.positions 已块级不再除）；quad 命中按（模型，target）弱引用缓存一次构建、每帧 3 查+9 乘微秒级、不进 GL GPU 天然兼容；UV 无命中→骨轴兜底
- 审查独立复算：python 复刻烘焙链+UV 命中+重心插值，staticSurface(px)=(0.000,35.825,-3.500) 逐位一致；**动画形变边界=无**（三路径动画输入均为同一 matrixData 骨矩阵，无骨变换外顶点形变——方案 A 与渲染顶点数学等价）；Trissy posUV 在 388 骨中唯一命中 Head north
- 观测层小缺陷（下轮修）：logAllowed 共享计数器奇偶相位可致 uv 行沉默（bind 行 fwd/up 精确正交=UV 存活判据）；ponytail 可收 12 行

## RC 空间簿记修复（tasks.diag-rc-anchor-space-mismatch，已合入 dev=57bb90e，2026-09-16）
- 用户回测新症状：正前方低头正常、**左右转头后低头错位、跳起转视角异常**——误差随 yaw/跳跃变化=空间簿记不一致
- **M1 前提证伪（审查五环裁决成立，e6d5091 追溯）**：探针 render 调用 yaw 实参（view yaw）被渲染器无视（仅名牌 :283 用），根帧=setupRotations(lerpBodyRot)=**体转基准**；getRawPos 把 BindResult.position 直加 entityPos（无旋转）→契约空间=世界轴向、体转基准、实体脚原点——e6d5091 的 view yaw 基准 M1 为错误前提下的错误修复
- **±50° 钳制发现**：vanilla tickHeadTurn（LivingEntity 2450-2463）站立/滞空不追平，|view−body|>50 时 yBodyRot 钳到 ±50——netHead=±50° 常态稳态=用户"转头后低头错位"主场景；正前方 netHead=0 恰好无错（假象根源）
- **修复 dd7c8fd**：bindRootFrame 改体转基准（R(180−body)·T(0,0.01,0)·S 逐项镜像渲染链，补缺失平移）+rendererLerpBodyRot（含骑乘分支）+logSpaceDiag 双基准打点（dFinal 列=M1 旧行为对照值，非当前误差）
- 数值：误差表 −50°→0.247 格/+50°→0.180/errY 恒 0（修复前）；审查自写独立校验器三轮复算残差 ≤9.0e-5+双 yaw spawn 独立新证；1201 jar 与声称 byte-identical（可复现构建实证）
- 已知边界（minor 三项入账）：睡眠位姿/TLM 载具 translate 未镜像（情境态、前后行为不变）；共享计数器奇偶遗留；commit 措辞勘误
- 回测判据：①站立转视角 >50° 后低头不再横向甩出②跳跃中转视角不穿模③正前方低头不回退；space-diag 行 fed 列=体转基准、dFinal 列=旧基准对照值
- **RC 债务关闭（用户确认 2026-09-16，性能无降低）**：RC 在场=净赚（探针屏蔽省每帧 4+ 次全模型渲染 vs 微秒级 UV 查询）；无 mod=零变化。FPM/RC 两大 P0 正式关闭。压缩锚点=remember#95，完整账=tasks.handoff-2026-09-16
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
