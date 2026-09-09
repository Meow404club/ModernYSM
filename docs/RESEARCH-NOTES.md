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
