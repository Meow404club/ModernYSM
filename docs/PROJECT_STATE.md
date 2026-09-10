# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。

## 阶段
- phase: 架构重构（Stonecutter 迁移）执行期——M0 骨架
- done: [RAG 34 源全量索引(407308块), openysm.cpp 研究(b573c7b), Stonecutter 调研(a780867), ADR 定案(decisions.adr-stonecutter-2026-09-10), curator 入库]
- current: M1 门禁卡 in_review（自测全绿：grep 零命中/进世界截屏实证/runServer Done 28.5s）
- next: 审查合入 → M1 收官锚点 → M2 1.16.5（unimined，POC 就绪）；M2 前定 mixinextras JIJ 发布策略

## ADR 摘要（decisions.adr-stonecutter-2026-09-10）
- architectury-api 彻底移除（@ExpectPlatform 无织入方）；**cardinal 与 forge_config_api_port 直接删**（全仓仅 fabric 源集使用，forge 通道纯 net.minecraftforge capability / ForgeConfigSpec）
- 版本矩阵：1.20.1 单项 `1.20.1-forge`（NeoForge 1.20.1=47.1.x 兼容 fork，一份 jar 双跑）；NeoForge 独立 API 自 1.20.5+ 归 M4
- 目录：根 settings.gradle.kts/stonecutter.gradle.kts/build.forge.gradle.kts + versions/<mc>-<loader>/；common→src/main；forge 实现类→platform/forge 子包；fabric/ 原目录保留不迁移不挂 sourceSet；legacy/ 空占位
- JNI：GeoModel+natives 整包平移冻结；nInitSIMD 是运行时读 @BufferBuilderMapping 注解（非硬编码），机制零改动
- 里程碑：M0 骨架+源码合并 → M1 1.20.1-forge 全绿（architectury=0 门禁）→ M2 1.16.5（unimined 路线实测 PASS，gating 已解）→ M3 平铺 1.18.2/1.19.2/1.19.4/1.20.4 → M4 NeoForge 1.20.5+（gating: 1.20.5 API 漂移研究）→ M5 1.12.2/1.7.10 源码条件化（build 已证可行，按用户指示推迟）
- **legacy/ 独立文件夹方案作废**（2026-09-10 POC 实跑：unimined 1.4.1 三版本全 PASS，报告 tmp/poc-1165/RUN-REPORT.md）；实装注意：archivesName 须带版本防产物同名覆盖、旧版本 runClient dev 运行时待验证

## 任务板摘要
| slug | status | branch | note |
|---|---|---|---|
| rag-bootstrap | merged | dev | cfeaae1；407308 块；共享库 402946 块净化副本 |
| arch-stonecutter-refactor | executing | - | ADR 定案，M0~M4 里程碑，11 张任务卡已登记 state |
| m0-stonecutter-skeleton | merged | dev | e9f0b61（no-ff+GPG）；审查全项通过；遗留 license=TODO 归 M1 |
| m0-merge-sources | queued | - | 三端源码合并进 src/main（依赖骨架合并） |
| mig-capability | merged | 70ae2cc | 8 stub 直调+CapabilityEvent Forge 原生；Clone 时序核证 |
| mig-network | merged | 7cc7c48 | SimpleChannel 原生；协议零改动 |
| mig-events-common | merged | 2a6f219 | 事件全映射+EventResult 复刻（javap 等价） |
| mig-compat | merged | 9f6d998 | 29 文件 97 stub；NCDFE 三层防护抽查过 |
| mig-registry-config | merged | 8657f78 | DeferredRegister+ModLoadingContext 直调 |
| mig-events-client | merged | d5362f8 | 键位/输入域全迁移；取消语义清零字节码证实 |
| mig-platform-util | merged | 7414160 | YsmPlatform+主类接线+NCDFE 修复（NFRT LegacyClasspath 根因，A/B 实证 27 models） |
| mig-purge-architectury | in_review | work/mig-purge-architectury | 3 commits；grep 门禁零命中+进世界截屏实证+runServer Done；审查中（分支③/5） |
| mig-purge-architectury | queued | - | M1 收尾门禁：全仓 dev.architectury=0 |
| poc-forge-1165 | done | - | 定案：unimined 线三代全 PASS，legacy/ 作废；1.7.10/1.12.2 源码条件化推迟 M5 |
| poc-execute-unimined | merged | - | 实跑闭环：三版本 BUILD SUCCESSFUL+SRG 抽查过；报告 tmp/poc-1165/RUN-REPORT.md |
| native-openysm-cpp | closed | - | 用户裁决：自带实现已完整，上游仅档案存查（tmp/harvest/openysm-cpp） |
| native-align-src | merged | dev | 7db591f 合入（rebase 后 564da91）；子模块 pin af8f642 可远端解析；对拍 10/10 全绿；主会话决策 natives 二进制暂不换装 |
| m0-merge-sources | in_review | work/m0-merge-sources | 5 commits：1749 文件 R100 平移+过渡依赖+42 stub 直调；build 过/runClient 主菜单零 FATAL；review-merge 审查中 |
| harvest-curator-1 | merged | - | stonecutter-template 入 RAG（17 文件）；openysm-cpp 不入库零污染 |

## 最近决策
- 2026-09-10 架构重构方向：脱离 Architectury——1.16.5+ 用 StonecutterTemplate(IAFEnvoy)
  多版本模板；1.7.10/1.12.2 独立 legacy 文件夹；跨版本维护策略届时调研。现状代码仍为
  Architectury，动手前先调研（任务 arch-stonecutter-refactor, status=research）。
- 2026-09-09 索引复用策略：ModernYSM 建全新 rag.db，不共用 gregtech6 库；范围内已索引 chunk
  以 (source, path, mtime) 三元组从 gregtech6 rag.db 直接移植（同名 source key + cp -a 保 mtime
  → index.py 增量零重嵌）；完成后净化副本（剔除 project/harvest/KG/state/memories）放
  `~/workspace/MGT6GA/rag-shared/` 供其他 mod 复用。
- 2026-09-09 版本选型：vanilla 13 版（1.7.10/1.12.2 用 MCP 名，1.16.5~26.1 用 Mojang 名）；
  forge-api 9 分支（1.7.10/1.12.x/1.16.x/1.17.x/1.18.x/1.19.2/1.19.x/1.20.1/1.20.4，1.20.5 后
  Forge 官方停更）；neoforge-api 10 版本（1.20.1~26.2.x）；cleanroom 1.12.2。1.20.5 世代由
  1.20.6 代表（补丁版差异可忽略）。
- 2026-09-09 服务端口：brain MCP :8999（本项目独立）；embed :8937 / rerank :8938 与
  gregtech6 全局共用（同一 llama-server 实例，模型一致才可共享向量空间）。
