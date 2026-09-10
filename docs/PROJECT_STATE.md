# 项目状态镜像（MCP state 为权威，本文件人工/自动同步）

> 由主 Agent 在每次 state_update 重要变更后同步镜像。

## 阶段
- phase: 架构重构（Stonecutter 迁移）规划期
- done: [RAG 34 源全量索引(407308块), openysm.cpp 研究(zig 构建可复现,JNI 漂移点已记录, b573c7b), StonecutterTemplate 调研（研究卡见 RESEARCH-NOTES.md）]
- current: architect 出迁移 ADR/模块卡；1.16.5 Forge 工具链 POC 并行；harvest 审查入库
- next: 收 ADR → 拆卡派发 coder（里程碑 M1 = 1.20.1 forge+neoforge 编译通过）

## 任务板摘要
| slug | status | branch | note |
|---|---|---|---|
| rag-bootstrap | merged | dev | cfeaae1；407308 块；共享库 402946 块净化副本 |
| native-openysm-cpp | closed | - | 用户裁决：自带实现已完整，上游仅档案存查（tmp/harvest/openysm-cpp） |
| native-align-src | in_progress | work/native-align-src | 仓内 native 只有二进制——上游 MIT 源码打底，重建 nInitSIMD+stateArray，对拍验证 |
| harvest-curator-1 | merged | - | stonecutter-template 入 RAG（17 文件）；openysm-cpp 不入库零污染 |
| arch-stonecutter-refactor | planning | - | 目标收窄：1.16.5+ 仅 forge/neoforge，fabric 代码保留不建项；legacy 推迟 |
| poc-forge-1165 | research | - | 1.16.5 Forge 能否进 stonecutter 单 Gradle（FG4/5 vs parchment-loom vs 拆 legacy） |

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
