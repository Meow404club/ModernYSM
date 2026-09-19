# RELEASE-MANIFEST — OpenYSM 2.6.6.6 全线生产构建清单

- 生成：2026-09-19；分支 `work/manifest-rebuild-38`（基线 82348d5 = dev 顶：nfrt-flatline 三线平铺合入后的全量重建版，取代上轮 `work/prod-release-202609` @ 4ca4867 的 35 线口径）。
- 用途：**用户自行分发的内部物料清单，不对外发布**。
- 构建口径：gradle 串行（`--no-daemon --no-configuration-cache`，38 线统一——nfrt 三线脚本头注强制该旗标，其余线沿上轮口径）逐线 `:产线:buildAndCollect`（= build + 收集；unimined 线构建含 SRG remapJar + MixinExtras/JOML/ImageStream/unsafe8 内嵌；nfrt 三线 createMinecraftArtifacts = NFRT 2.0.31 JavaExec 直调，见备注 7）。
- **发布取件口径：唯一可分发件 = `versions/<产线>/build/libs/<jar>`（reobf 物，本表 sha256 即该路径实测）。** 根收集目录 `build/libs/2.6.6.6/`：收集任务自 0e39933 起改收 reobfJar 输出（build.forge.gradle.kts:295），本轮 38 件收集件与 `versions/<线>/build/libs/` 取件件 cmp 字节全同（审查实证）——收集目录与取件路径同物。unimined 线 build/libs 内另有 `-dev.jar`（mojmap 开发件），不可分发。
- 门禁：每线 jar 跑 `tools/audit_reobf_collision.py`，**38/38 全部 0 BROKEN（exit 0）**，无红线。
- modid：`yes_steve_model`（全线统一）；maven 坐标 `rip.ysm:openysm`；版本号 **2.6.6.6 为现值——升版待用户裁决，若升版须以新版本号重跑全量构建并再生成本清单（jar 名内嵌版本号）**。
- 重建缘由：基线 82348d5 较上轮发布基线含多批源码合并，35 条既有线产物 sha256 全量平移（旧表值全部作废）；另新增 1.20.2/1.20.3/1.20.5-neoforge 三线（NFRT 直驱）→ 38 线。本表每行均为本批实测新值。
- 已知功能债与限制（不漏报）：见同目录 [CHANGELOG-draft.md](CHANGELOG-draft.md)。

| 产线 | MC 版本 | Loader | jar 名 | 大小 | sha256 | 字节码 / 工具链口径 | tour |
|---|---|---|---|---|---|---|---|
| `1.20.1-forge` | 1.20.1 | forge | `openysm-2.6.6.6-1.20.1-forge.jar` | 23.9 MB | `282d6a69df69a1eee20e33399f6b9ca83b7a788c3a79e299bf814e2a9112a742` | 61 / Java 17（MDG 自动请求） | 在案 |
| `1.17.1-forge` | 1.17.1 | forge | `openysm-2.6.6.6-1.17.1-forge.jar` | 24.4 MB | `1beaf67a9907d422cbb8f5b38297dbfc005fdcdb0fa59fa7579968dbb8f27358` | 60 / Java 16（MDG 按 userdev 能力数据自动请求） | 未跑 tour |
| `1.18.2-forge` | 1.18.2 | forge | `openysm-2.6.6.6-1.18.2-forge.jar` | 24.4 MB | `8853b469adbf03066b10fe1e90e3cf155ebec78eb20f97cf7c6ce7e1a5d4ce7a` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.2-forge` | 1.19.2 | forge | `openysm-2.6.6.6-1.19.2-forge.jar` | 24.4 MB | `f91435f141fa2356dd603c39c434e2017fb32210c381e0dcf86ac412bce38f9f` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.4-forge` | 1.19.4 | forge | `openysm-2.6.6.6-1.19.4-forge.jar` | 23.7 MB | `62f86758a06ab040ffa575801e89442de91c48c631bdddea566adee3780c1376` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.18-forge` | 1.18 | forge | `openysm-2.6.6.6-1.18-forge.jar` | 24.4 MB | `2372ad700c1a49f4da0e203109668fa13ea83362d7a04a60f9d3098f96f09a5f` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.18.1-forge` | 1.18.1 | forge | `openysm-2.6.6.6-1.18.1-forge.jar` | 24.4 MB | `417b00ad7f6ab37a82c86cd8d927a70700fbd2eae3b676d972ebf23446c37301` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19-forge` | 1.19 | forge | `openysm-2.6.6.6-1.19-forge.jar` | 24.4 MB | `39e40dcb988d650d3ed2ef23c751ce1f2d9e1e66693354011c68a4dbd80f7495` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.1-forge` | 1.19.1 | forge | `openysm-2.6.6.6-1.19.1-forge.jar` | 24.4 MB | `5e0c887e4a4dcd31f261aa294f8eeeefbb6d39e510ac700cf16d396bf4026cbd` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.3-forge` | 1.19.3 | forge | `openysm-2.6.6.6-1.19.3-forge.jar` | 23.7 MB | `a570c2e98db98994c98011119f7f52637d40a419aabdd5a91cc3fe32662632c5` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.20-forge` | 1.20 | forge | `openysm-2.6.6.6-1.20-forge.jar` | 23.7 MB | `06f35f85f24081d83c76f8b1ba4559fe0bfd61759ac554b5eeb610604e51ad39` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.16.5-forge` | 1.16.5 | forge | `openysm-1.16.5-2.6.6.6.jar` | 24.5 MB | `2c684d3ecccd4ae7e482ac0bc8efcd0692ccf12cc3c51d39f86f8c82a18043ac` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 在案 |
| `1.16.1-forge` | 1.16.1 | forge | `openysm-1.16.1-2.6.6.6.jar` | 24.5 MB | `bc6b026d4a7a97edd499d0aca5f2cf82381c72c3d520d68fac92101a4dc8f580` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.16.2-forge` | 1.16.2 | forge | `openysm-1.16.2-2.6.6.6.jar` | 24.5 MB | `5f08e335b5fab78f9cb35e0522c77a38f9cf8819ff893c3cbd1a8e694fbe1ea7` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.16.3-forge` | 1.16.3 | forge | `openysm-1.16.3-2.6.6.6.jar` | 24.5 MB | `5a7801fec57ca9e304b2c9ab4d6fcfdc383147b80677be9d24442493c5d5c1ae` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.16.4-forge` | 1.16.4 | forge | `openysm-1.16.4-2.6.6.6.jar` | 24.5 MB | `6c9e24fa973d49e1618ea5ddf501bb91d0dc3d2df188654d97a4ca1d8b190229` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.20.4-neoforge` | 1.20.4 | neoforge | `openysm-2.6.6.6-1.20.4-neoforge.jar` | 23.6 MB | `63613cb82c246e732cd70c6d55465b4d41e66ae61957e47cf3787bef61675477` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.20.6-neoforge` | 1.20.6 | neoforge | `openysm-2.6.6.6-1.20.6-neoforge.jar` | 23.6 MB | `f7d7208566a5af6ce5496107e0dda36a07a12707c4e3475aca32c4066b80d2b7` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `1.21.1-neoforge` | 1.21.1 | neoforge | `openysm-2.6.6.6-1.21.1-neoforge.jar` | 23.6 MB | `63a771e80bcf7a50cdd4f7684dce67d9000015933f972ae85eedc293ebeebc16` | 65 / Java 21（MDG 自动请求） | 在案 |
| `21.3-neoforge` | 1.21.3 | neoforge | `openysm-2.6.6.6-21.3-neoforge.jar` | 23.6 MB | `5dcc92d72926db1eb7dc0003b8806fddc652497716e501e22a989f52896d2ef5` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.4-neoforge` | 1.21.4 | neoforge | `openysm-2.6.6.6-21.4-neoforge.jar` | 23.6 MB | `ee2d507b4211ba162e0a081a460f9dc691b5510b1e90d47d3cf05c4b64536fbe` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.5-neoforge` | 1.21.5 | neoforge | `openysm-2.6.6.6-21.5-neoforge.jar` | 23.6 MB | `87e8f1e87f11db7349d1c4a89830da5a31b586f52befe1aa3f308c3101b60cc5` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.8-neoforge` | 1.21.8 | neoforge | `openysm-2.6.6.6-21.8-neoforge.jar` | 23.6 MB | `0cf18a67a4236e50db6f914aff28e4fe88cecf7fc4ded0de5eb44a32659a956f` | 65 / Java 21（MDG 自动请求） | 在案 |
| `21.10-neoforge` | 1.21.10 | neoforge | `openysm-2.6.6.6-21.10-neoforge.jar` | 23.6 MB | `55ba665e8cd50287d8ac1577647a2dcadc71f98b33dd33688b524fb1b3cee856` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.11-neoforge` | 1.21.11 | neoforge | `openysm-2.6.6.6-21.11-neoforge.jar` | 23.6 MB | `1a85bd9c55a4aa02ea50a0eab665828e4cd6874595bf6e9c3472a2cdb9a86e1a` | 65 / Java 21（MDG 自动请求） | 在案 |
| `26.1-neoforge` | 1.26.1 | neoforge | `openysm-2.6.6.6-26.1-neoforge.jar` | 23.6 MB | `254287485ec4736b0814adf096a6cf92b9628665a0b1061c8ce1b70f25f5f2af` | 69 / Java 25（MDG 自动请求） | 在案 |
| `26.1.1-neoforge` | 1.26.1.1 | neoforge | `openysm-2.6.6.6-26.1.1-neoforge.jar` | 23.6 MB | `73b101ac152d5939e3c50e214e35e4c9ab5459cb0f7b05d57c26a3a955d2a29b` | 69 / Java 25（MDG 自动请求） | 未跑 tour |
| `26.1.2-neoforge` | 1.26.1.2 | neoforge | `openysm-2.6.6.6-26.1.2-neoforge.jar` | 23.6 MB | `67ddac284a89f21191a2f2522811ae93293690bd9a476cb8ca8e77d212454685` | 69 / Java 25（MDG 自动请求） | 未跑 tour |
| `26.2-neoforge` | 1.26.2 | neoforge | `openysm-2.6.6.6-26.2-neoforge.jar` | 23.6 MB | `5c39b08ee47d602a15430649f5bdc98da07087df9158f20f98d051a91b987368` | 69 / Java 25（MDG 自动请求） | 在案 |
| `26.3-neoforge` | 1.26.3 | neoforge | `openysm-2.6.6.6-26.3-neoforge.jar` | 23.6 MB | `843989759c5ecdd268cb9e5d0e21cdbb2da01cf80b257bc9fb8e8cbf1b083f46` | 69 / Java 25（MDG 自动请求） | 在案 |
| `1.21-neoforge` | 1.21 | neoforge | `openysm-2.6.6.6-1.21-neoforge.jar` | 23.6 MB | `b23f610a970c5f2b1619150742659efa965ab5357622cc6586ac9fa58f39faa0` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.2-neoforge` | 1.21.2 | neoforge | `openysm-2.6.6.6-21.2-neoforge.jar` | 23.6 MB | `0429c0916c17093226388f855aedbd8e5a34a3a8b80f87625080f9d5ed2573a7` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.6-neoforge` | 1.21.6 | neoforge | `openysm-2.6.6.6-21.6-neoforge.jar` | 23.6 MB | `3e6dfdb56a9353cd3810bd3b7eea36352f0c9e4442624b0945f7d9887a4565a5` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.7-neoforge` | 1.21.7 | neoforge | `openysm-2.6.6.6-21.7-neoforge.jar` | 23.6 MB | `14986c532a4542c7d5cc4175ea2837eb000aa92bc81706db703c812a73bdf9df` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.9-neoforge` | 1.21.9 | neoforge | `openysm-2.6.6.6-21.9-neoforge.jar` | 23.6 MB | `e61a67a18fcbd3b0c45aa6bf1d56361c42181d720a225d63055a220f73ede456` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `1.20.2-neoforge` | 1.20.2 | neoforge | `openysm-2.6.6.6-1.20.2-neoforge.jar` | 23.6 MB | `81b55657b3edae88b2ccd4e5c7420dbb4aa55d2bde746da2fa347904d04b9409` | 61 / Java 17（NFRT 2.0.31 直驱，named 直发无 reobf） | 在案 |
| `1.20.3-neoforge` | 1.20.3 | neoforge | `openysm-2.6.6.6-1.20.3-neoforge.jar` | 23.6 MB | `5916042c1b664547dd7f215615abcdda940c65dcf038f8c6218bea60f8aaea95` | 61 / Java 17（NFRT 2.0.31 直驱，named 直发无 reobf） | 在案 |
| `1.20.5-neoforge` | 1.20.5 | neoforge | `openysm-2.6.6.6-1.20.5-neoforge.jar` | 23.6 MB | `fcb34871c87422b6bafa0196bba9711b3365d5c4224b8e3faa1e2460b26be8cd` | 65 / Java 21（NFRT 2.0.31 直驱，named 直发无 reobf） | 在案 |

## 备注

1. **tour 标注口径**：tour 在案 = 历史卡 16 屏走查证据可采信；「未跑 tour」线以 compileJava + build + audit（0 BROKEN）口径入名单。在案来源：派单指定 8 线（1.20.1/1.16.5/21.8/21.11/26.1/26.2/26.3/1.21.1，上轮在册）+ NFRT 三新线（nfrt-flatline-1203-1205 卡走查证据，已合入 dev：1.20.2 Done 2.201s / 1.20.3 Done 2.026s / 1.20.5 Done 0.692s 重跑，16png 在案）。另有可追溯补跑记录未计入标注：21.7/21.9（m3-neoforge-server-dist-fix）、26.1.2（m3-26x 适配卡）。本卡按派单不重跑 tour。
2. **上轮（prod-release-202609）1.19.4 适配修复**：stonecutter 0.7 条件块双语义陷阱致 1.19.4 唯一红（InventoryScreenMixin 注入点更名 `renderEntityInInventoryFollowsMouse(PoseStack,...)`、ArmorItem `getSlot()`→`getEquipmentSlot()`），条件切分修复；对其他线经逐条目 CRC + javap 归一化对拍证明零行为变化。该修复在本基线谱系内，本轮全量重测覆盖。
3. **上轮零漂移证明与六线新鲜度**（1.19.4 修复轮）：与修复前产物逐条目 CRC 对比语义零变化；1.16.1/1.18.2/1.19/1.19.1/1.19.2 五线彼时首次完整 buildAndCollect 全绿。本轮为源码合并后的全新构建，上轮结论不再外推，以本表实测为准。
4. **1.20.1 抽查（可加载性 smoke）**：本轮新 jar `unzip -t` 全 2921 条目 CRC 无错（exit 0）；上轮深检结论（META-INF/mods.toml + MixinConfigs 在位、natives/ 五平台齐、linux-x64 libysm-core.so 可 dlopen、1201 安装器 soak ~24min 0 NSME 先例 d2a8802）按同口径采信。
5. audit 已知局限（审查在案，非阻断）：INHERITED 桶未做 vanilla jar 在场验证；字段引用 desc 丢弃按名匹配有假 OK 风险（见 tasks prod-fix 审查记录）。
6. **unimined 线双产物**：build/libs 下 `openysm-<线>-2.6.6.6-dev.jar` 为 mojmap 开发件，**不可分发**；发布件 = 无 `-dev` 后缀的 remapJar 物（SRG 命名，本表即该物）。
7. **NFRT 直驱三线口径**（1.20.2/1.20.3/1.20.5-neoforge）：走 `build.nfrt.gradle.kts`（NFRT 2.0.31 CLI JavaExec 直调，绕过 MDG capability 解析——上轮 POC 判负三线：官方 maven 无 Gradle `.module` 元数据），tile = 1.20.2=20.2.93 / 1.20.3=20.3.8-beta / 1.20.5=20.5.21-beta。构建**必须** `--no-configuration-cache`（脚本头注：配置期解析 userdev jar）。产物为 named（mojmap）直发、无 reobf/refmap，与 moddev 线同口径；游戏 jar 装配由 NFRT `gameJarWithNeoForge` 供件。上轮判负报告与机制对齐证据见 tasks.m3-nfrt-direct-drive-poc / nfrt-flatline-1203-1205。
