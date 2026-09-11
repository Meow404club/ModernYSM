# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。2026-09-11 全量重写（历史行漂移清理）。

## 阶段
- phase: 架构重构（Stonecutter 迁移）——M2 收官冲刺
- done: [M0 骨架 e9f0b61 + 源码合并 d086af3, native 子模块 7db591f, M1 八卡全过 0f81660（Architectury 清零+进世界验收）, M2 七卡合入 3483e9d/acd46c8/c5606ae/8f23248/5b6d3ac/fa83f37/ea9bfb3]
- current: M2 收官双卡并行（native-poc + prod-refmap 阻断修复）
- next: 双卡合入 → ingame-smoke-gate（**须加生产 jar 启动验证**——dev runClient 测不出 refmap 类盲区）→ **M2 收官：记忆整理→压缩上下文→M3 全谱平铺**

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
| m2-native-poc-1165 | in_progress | work/m2-native-poc-1165 | GO/NO-GO：initSIMD 调用点条件化+SIMD vs 兼容路径一致性+结论回写 |
| m2-prod-refmap-1165 | in_progress | work/m2-prod-refmap-1165 | **阻断**：1.16.5 非 dev 启动崩溃（crash log tmp/crash-2026-09-11_11.16.02）——@WrapWithCondition 方法名未重映射，refmap 系统性缺口，18 条全审计 |
| m2-native-poc-1165 | queued | - | GO/NO-GO；联验输入：GeoModel.initSIMD 传 VertexFormat.Mode.class 1.16.5 编不过（native 期望 int-mode，安全降级门槛已证） |
| m2-ingame-smoke-gate | queued | - | 双版本进世界终验（含模型不变形视觉核验） |
| feature-debts-1165 | ledger | - | 6 项功能差（blur/iris/tooltip/extraplayer/biome-molang/compat-matrix），M2 收官后排期发卡 |
| poc-forge-1165 / poc-execute-unimined | done | - | unimined 路线定案（三代 POC 全 PASS） |
| native-openysm-cpp | closed | - | 自带实现完整；上游仅档案存查 |
| harvest-curator-1/2 | merged | - | stonecutter-template+stonecutter-src-07+celeritas-mva 入 RAG |

## M3 平铺原则（用户指示 2026-09-11，tasks.m3-flat-tiling）
- 铺到 1.16.5+ forge/neoforge 支持的全部版本（地图玩家友好：地图绑定 MC 版本）；
- 枚举以 Forge/NeoForge maven 官方清单为准；Forge 终点 1.20.4，NeoForge 1.20.1（一 jar 双跑）+1.20.2 起全谱；
- 相邻补丁版 API 面一致可共享条件组；20+ 条目考虑 settings/properties 生成脚本化。

## 功能差债务清单（tasks.feature-debts-1165，M2 收官后排期发卡）
| id | 功能差 | 现状 | 可行性 | 卡 |
|---|---|---|---|---|
| debt-gui-blur + debt-gui-iris | GUI 毛玻璃 + Iris GPU 路径 | 空实现/恒 false | 中：oculus 桥接 vs 自写 shader 合并调研 | debt-gui-blur-1165 |
| debt-gui-tooltip | 暂停页按钮 tooltip | 桥接未接链 | 高：小卡 | debt-gui-tooltip-1165 |
| debt-overlay-extraplayer | ExtraPlayerOverlay | 1.16.5 空渲染 | 中：照 YsmGui 模式 | debt-overlay-extraplayer-1165 |
| debt-biome-molang | biome molang 查询 | 恒 false | 中：category/name 近似，需模型侧确认 | debt-biome-molang-1165 |
| debt-compat-matrix | 第三方 compat 全矩阵 | shim 缺席 | 大：M3 后立项 | debt-compat-matrix-1165 |

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
