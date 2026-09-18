# RELEASE-MANIFEST — OpenYSM 2.6.6.6 全线生产构建清单

- 生成：2026-09-18；分支 `work/prod-release-202609` @ f5a7542（基线 8cf9355 = dev 2800a3a + docs 镜像 + 1.19.4 适配修复）
- 用途：**用户自行分发的内部物料清单，不对外发布**。
- 构建口径：gradle 串行（`--no-daemon --no-configuration-cache`）逐线 `:产线:buildAndCollect`（= build + 发布 jar 收集；unimined 线含 SRG remapJar + MixinExtras/JOML/ImageStream/unsafe8 内嵌）。
- 门禁：每线 jar 跑 `tools/audit_reobf_collision.py`，**35/35 全部 0 BROKEN（exit 0）**，无红线。
- modid：`yes_steve_model`（全线统一）；maven 坐标 `rip.ysm:openysm`；版本号 **2.6.6.6 沿用现状（是否升级待用户裁定，本次未擅自变更）**。
- 已知功能债与限制（不漏报）：见同目录 [CHANGELOG-draft.md](CHANGELOG-draft.md)。

| 产线 | MC 版本 | Loader | jar 名 | 大小 | sha256 | 字节码 / 工具链口径 | tour |
|---|---|---|---|---|---|---|---|
| `1.16.5-forge` | 1.16.5 | forge | `openysm-1.16.5-2.6.6.6.jar` | 24.5 MB | `be0593571787e356d0874cec45100f153a819dcf3d8e94d6da8175acf88e52bf` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 在案 |
| `1.20.1-forge` | 1.20.1 | forge | `openysm-2.6.6.6-1.20.1-forge.jar` | 23.9 MB | `d4cf7ec749558e84afacfa9b2a0c720d7a31ac3ef837976541b9ded1ae915ade` | 61 / Java 17（MDG 自动请求） | 在案 |
| `1.16.1-forge` | 1.16.1 | forge | `openysm-1.16.1-2.6.6.6.jar` | 24.5 MB | `0d4541d9c46b347c48821f11a51e54f96835da50407c34e6e9496bc69642f628` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.18.2-forge` | 1.18.2 | forge | `openysm-2.6.6.6-1.18.2-forge.jar` | 24.3 MB | `776fb4ec8d77aaff84cf85e08e1a216698fac53fb4b81dc4d4d88216f8043b02` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19-forge` | 1.19 | forge | `openysm-2.6.6.6-1.19-forge.jar` | 24.3 MB | `1ea73a4c21a3b47339c76217fb28f52bca86398f972577ae3afff4d8408ec886` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.1-forge` | 1.19.1 | forge | `openysm-2.6.6.6-1.19.1-forge.jar` | 24.3 MB | `bce9a58499f2f5ee9207081255834106fd98be78c6de697980dd38c03eee67b9` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.2-forge` | 1.19.2 | forge | `openysm-2.6.6.6-1.19.2-forge.jar` | 24.3 MB | `985f98278be4f96f082cfa782f6d1eb0e735fd2ef08acd8dcf71bf69e997444f` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.16.2-forge` | 1.16.2 | forge | `openysm-1.16.2-2.6.6.6.jar` | 24.5 MB | `c670c91295b02ab06f0cc507480c1033a63db54fd878ddd6fb02a074a64a7098` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.16.3-forge` | 1.16.3 | forge | `openysm-1.16.3-2.6.6.6.jar` | 24.5 MB | `48b3ab1ebf3f4d416b7561de01fb2792df4609e72e978445a94717ed7698e7e4` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.16.4-forge` | 1.16.4 | forge | `openysm-1.16.4-2.6.6.6.jar` | 24.5 MB | `772c00685786a84756e018335d2e616bef327ffadd98e286773147ffd2119170` | 52 / Java 8（unimined：toolchain 21 + javac --release 8） | 未跑 tour |
| `1.17.1-forge` | 1.17.1 | forge | `openysm-2.6.6.6-1.17.1-forge.jar` | 24.3 MB | `6d3fc28352d1d1958e4476b5d374831b1853d72c815a307d86ea10a886fe62ae` | 60 / Java 16（MDG 按 userdev 能力数据自动请求） | 未跑 tour |
| `1.18-forge` | 1.18 | forge | `openysm-2.6.6.6-1.18-forge.jar` | 24.3 MB | `06b02517e213806bbcd6877af1b9fbab6ed20c3ce22185c9dc5ecd96db405dc1` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.18.1-forge` | 1.18.1 | forge | `openysm-2.6.6.6-1.18.1-forge.jar` | 24.3 MB | `70d7aa5b2875f9993ef27ae5b7bb8f91d043343b5302a2b41eb21c53b074afbd` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.3-forge` | 1.19.3 | forge | `openysm-2.6.6.6-1.19.3-forge.jar` | 23.7 MB | `67606ca0fba177b6468a3542aeb74074bb5a4caf0f760ccb79342059e7f3cf89` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.19.4-forge` | 1.19.4 | forge | `openysm-2.6.6.6-1.19.4-forge.jar` | 23.7 MB | `5dca7df511a03b8d4a6e4920d516ad74c492a612fd51ee8c1ec114ea35dabf97` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.20-forge` | 1.20 | forge | `openysm-2.6.6.6-1.20-forge.jar` | 23.7 MB | `7e216a681e39923f29c3aba0b3c3f6bbacdf43b0a80bb3b17aad0b152bb77872` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.20.4-neoforge` | 1.20.4 | neoforge | `openysm-2.6.6.6-1.20.4-neoforge.jar` | 23.6 MB | `8abbf3cf25e01769edb9b028c42ab60c3663b14bd7d5e3c7399daa927100421b` | 61 / Java 17（MDG 自动请求） | 未跑 tour |
| `1.20.6-neoforge` | 1.20.6 | neoforge | `openysm-2.6.6.6-1.20.6-neoforge.jar` | 23.6 MB | `436b51e8d0ff614a895be02c4f96eaa3ec42be0759758c7b639ecac96fa49158` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `1.21-neoforge` | 1.21 | neoforge | `openysm-2.6.6.6-1.21-neoforge.jar` | 23.6 MB | `4285803a752ca90b1c38b479cf0bf41a53aaea00398e9ff701053ddbf94a5b96` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `1.21.1-neoforge` | 1.21.1 | neoforge | `openysm-2.6.6.6-1.21.1-neoforge.jar` | 23.6 MB | `308daf0152a9706815539335225298247252f5bd337925fb28082d5b37a688e4` | 65 / Java 21（MDG 自动请求） | 在案 |
| `21.2-neoforge` | 1.21.2 | neoforge | `openysm-2.6.6.6-21.2-neoforge.jar` | 23.6 MB | `97216955a0d0e1d9611df271a9f0e8b181afb7826a57a289ce8a371f0af7a64c` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.3-neoforge` | 1.21.3 | neoforge | `openysm-2.6.6.6-21.3-neoforge.jar` | 23.6 MB | `57c22d0b2db8d7a963638d8db40f38ae142961eba7014abf33a1fd0c27829df9` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.4-neoforge` | 1.21.4 | neoforge | `openysm-2.6.6.6-21.4-neoforge.jar` | 23.6 MB | `9f26b63adf1dfe9296c2ec6f9d7e5d00c194dcd76eadca1b91c66d7e7b7da393` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.5-neoforge` | 1.21.5 | neoforge | `openysm-2.6.6.6-21.5-neoforge.jar` | 23.6 MB | `f82d9f642da4ee5cad54f4f2a8e46afb19af62630c85733803534662147ecef9` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.6-neoforge` | 1.21.6 | neoforge | `openysm-2.6.6.6-21.6-neoforge.jar` | 23.6 MB | `114d11212c6f84d4be9707162f548a8c1a70c23ba30683a1c31fb50581af50a8` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.7-neoforge` | 1.21.7 | neoforge | `openysm-2.6.6.6-21.7-neoforge.jar` | 23.6 MB | `459a3d6fee4a6d7b4cbcd7d9be0abf694f4ba3b399ed6701a71b87edb44ad185` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.8-neoforge` | 1.21.8 | neoforge | `openysm-2.6.6.6-21.8-neoforge.jar` | 23.6 MB | `adfff34886bd60c391d11c06142ea74728191b9ac3ffdd4a4527620e25b67abb` | 65 / Java 21（MDG 自动请求） | 在案 |
| `21.9-neoforge` | 1.21.9 | neoforge | `openysm-2.6.6.6-21.9-neoforge.jar` | 23.6 MB | `cf5117940f82f52c2611b47ae1bde36a1cc9826c04cddb39e03f907fd68a77a5` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.10-neoforge` | 1.21.10 | neoforge | `openysm-2.6.6.6-21.10-neoforge.jar` | 23.6 MB | `e7962e23659a881143c0677d15799379b8beecde7b75895cdc367f1a35dc3da2` | 65 / Java 21（MDG 自动请求） | 未跑 tour |
| `21.11-neoforge` | 1.21.11 | neoforge | `openysm-2.6.6.6-21.11-neoforge.jar` | 23.6 MB | `5c021983b4cb130d59841e3ba487545dc2d27a651cf0dbffa975881917cc0d00` | 65 / Java 21（MDG 自动请求） | 在案 |
| `26.1-neoforge` | 1.26.1 | neoforge | `openysm-2.6.6.6-26.1-neoforge.jar` | 23.6 MB | `a0aece3f36e7b13807d805b87af2e75f9da60e499a618bc8f62239cccd1fd269` | 69 / Java 25（MDG 自动请求） | 在案 |
| `26.1.1-neoforge` | 1.26.1.1 | neoforge | `openysm-2.6.6.6-26.1.1-neoforge.jar` | 23.6 MB | `949a7a97d07875f74ec814c838e65548d39f95d153e0329322410014b59855fb` | 69 / Java 25（MDG 自动请求） | 未跑 tour |
| `26.1.2-neoforge` | 1.26.1.2 | neoforge | `openysm-2.6.6.6-26.1.2-neoforge.jar` | 23.6 MB | `dd2a4d64fc59a4eefd1641dd3c9af564e613fb47fb2db1bef9b59f7f6b2f2ef8` | 69 / Java 25（MDG 自动请求） | 未跑 tour |
| `26.2-neoforge` | 1.26.2 | neoforge | `openysm-2.6.6.6-26.2-neoforge.jar` | 23.6 MB | `32510c62ed431d22b24c13156b94e158a23977ffc30b419deb45983370135c40` | 69 / Java 25（MDG 自动请求） | 在案 |
| `26.3-neoforge` | 1.26.3 | neoforge | `openysm-2.6.6.6-26.3-neoforge.jar` | 23.6 MB | `bef0c269b785237883f876b3fd2b750aba55d8d5aa794f13eb559e3aea689c4d` | 69 / Java 25（MDG 自动请求） | 在案 |

## 备注

1. **tour 标注口径**：tour 在案 = 历史卡 16 屏走查证据可采信（派单指定 8 线：1.20.1/1.16.5/21.8/21.11/26.1/26.2/26.3/1.21.1）；「未跑 tour」线以 compileJava + build + audit（0 BROKEN）口径入名单。另有在案可追溯的补跑记录未计入上表标注：21.7/21.9（m3-neoforge-server-dist-fix，7e5a2d9）、26.1.2（m3-26x 适配卡 tour 首跑）。
2. **1.19.4 适配修复（本卡唯一源码变更）**：首轮全量构建 1.19.4 唯一红。根因 = stonecutter 0.7 条件块内 `/* */` 包裹的双语义（生成线条件为真→剥包裹成活码；1.20.1 vcs 原文直通编译→注释态）：InventoryScreenMixin 六参 `renderEntityInInventory` 块对 1.19.4 走活码，而 1.19.4 目标已更名 `renderEntityInInventoryFollowsMouse(PoseStack,IIIFF,LivingEntity)`（forge-1.19.4-45.4.0 merged jar javap 实证，`InventoryScreen.render():82` 调用点）；CustomPlayerArmorLayer.isArmorItem 同陷阱（1.19.4 ArmorItem 仅 `getEquipmentSlot()` 无参形，javap 实证）。修法 = 条件切分（`>=1.19.3 && <1.19.4` / `>=1.19.4 && <...`）+ 保留包裹护 1201 vcs。
3. **零漂移证明**：修复后全 35 线统一重跑；与修复前产物逐条目 CRC 对比，差异仅限被编辑两文件的 5 个（类，jar）对，javap 去 CP 索引/行注释归一化后逐对全同（语义零变化）；1201 的 InventoryScreenMixin 类字节全同。1.19.4 为唯一行为修复目标（红→绿）。
4. **六线新鲜度**：1.16.1/1.18.2/1.19/1.19.1/1.19.2（570a961 修绿）本次首次完整 buildAndCollect 全绿。
5. **1.20.1 抽查（可加载性 smoke）**：unzip -t 全条目 CRC 无错；META-INF/mods.toml + MixinConfigs 清单在位；natives/ 五平台（win/mac-linux/android）齐；零 rip/ysm/harness 条目；linux-x64 libysm-core.so 依赖仅 libc 族、可 dlopen，JNI 注册期 jar 内类（GeoModel）可解析；fastutil/asm 等缺项均为 MC 运行时必供依赖（裸 JVM 无游戏环境的预期边界）。真机先例：1201 安装器 soak ~24min 0 NSME（prod-fix-reobf-collision，d2a8802）。
6. audit 已知局限（审查在案，非阻断）：INHERITED 桶未做 vanilla jar 在场验证；字段引用 desc 丢弃按名匹配有假 OK 风险（见 tasks prod-fix 审查记录）。
