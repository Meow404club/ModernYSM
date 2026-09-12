# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。2026-09-11 全量重写（历史行漂移清理）。

## 阶段
- phase: 架构重构（Stonecutter 迁移）——M2 收官冲刺
- done: [M0 骨架 e9f0b61 + 源码合并 d086af3, native 子模块 7db591f, M1 八卡全过 0f81660（Architectury 清零+进世界验收）, M2 七卡合入 3483e9d/acd46c8/c5606ae/8f23248/5b6d3ac/fa83f37/ea9bfb3]
- current: M2 十卡全部合入，收官门禁 ingame-smoke-gate in_review（④/5，三层 PASS+6 文件修补 1f7e1a2）
- next: 门禁合入+tag v2.6.6.6-m2 裁决 → **M2 收官：记忆整理（蒸馏/kg_stats/锚点）→ 用户压缩上下文 → M3 全谱平铺**

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

## 跨版本通用测试 harness（tasks.cross-version-harness，已合入 dev=ccb6b23，2026-09-13）
- 交付：harness/tour.sh orchestrator（--no-daemon+setsid+PIDFILE/PGID 阶梯清理、和平启动五命令 stdin 注入、版本参数化 case 表单点）+ 零 GL GuiTourDriver/HarnessScreens（cmd.txt→harness.ready，14 屏）+ YsmEventBootstrap 3 指令守卫挂载（惰性解析实证）+ 双 buildscript 生产 jar 排除
- 实证：双线 tour exit 0/16 屏/零崩溃零手工；双产物 jar 零 harness 字节；1.20.1 javap 1661/1662 类全同；生产安装器实启 25s 到主菜单全日志零 harness 引用
- 新增版本线接入约定：tour.sh case 推导表 + buildscript runs.gameDirectory 对齐 + options.txt onboarding 项核对（1.17~1.19 待核）
- 遗留 P1 已修（dev=4983cbb）：cleanup FIFO 写入 timeout 包裹；runs 拆分后旧 run/saves 不自动迁移（冒烟改走 tour 自管世界）；crash 快速失败覆盖窄
- 发布卡新增：主仓 build/libs/2.6.6.6/ 是 mojmap 陈旧副本（真基线在 versions/1.20.1-forge/build/libs/，SRG 形态），发布前必重建

## M3 平铺（tasks.m3-flat-tiling，矩阵已定，前置卡 m3-condition-axis）
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
