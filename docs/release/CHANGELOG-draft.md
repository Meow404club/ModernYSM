# CHANGELOG 草稿 — OpenYSM 2.6.6.6（全线平铺版）

> 状态：草稿（供用户审阅后自行发布）。版本号沿用现状 **2.6.6.6**，本次未擅自升级——是否升版列为待用户决定项。
> 构建基线：dev 82348d5（prod-release-202609 谱系 + nfrt 三线平铺等本批合并，2026-09-19 全量重建）。38 条产线全部构建 + audit 0 BROKEN，清单见 [RELEASE-MANIFEST.md](RELEASE-MANIFEST.md)。

## 新增

- **全版本平铺**：单仓 Stonecutter 构建 35 条产线——Forge 1.16.1 ~ 1.20.1（含 1.16.1~1.16.5、1.17.1、1.18.x、1.19.x、1.20）与 NeoForge 1.20.4 ~ 1.26.3（1.20.4/1.20.6/1.21/1.21.1/21.2~21.11/26.1/26.1.1/26.1.2/26.2/26.3）。
- **NeoForge 新线**：1.20.4 起 NeoForge 全谱接入（mods.toml [[mixins]] 声明、mojmap 直配无 refmap 口径）。
- **NeoForge 1.20.2/1.20.3/1.20.5 三线补齐（NFRT 直驱）**：以 NeoForm Runtime 2.0.31 直驱构建接入 MDG capability 解析判负的三线（named 直发无 reobf；1.20.2/1.20.3=Java 17、1.20.5=Java 21），全线扩至 **38 条产线**，清单见 RELEASE-MANIFEST.md。
- **新代适配**：26.1 / 26.2 / 26.3 渲染代移植（submit DAG 几何提交、GuiGraphics 抽取、SDL 键值域/事件面、swing 状态 API 换代）。
- **FirstPersonModel 真 compat**：NeoForge 21.2 ~ 21.8 七线恢复第一人称模型兼容（vendor jar 按 Modrinth 校验和锁定；21.9+ 见已知限制）。
- **GPU 渲染路径复活（21.8/21.11）**：投影/雾参数捕获面 + vanilla FBO 缓存命中自绑，世界内模型恢复 GL43 compute GPU 蒙皮路径（捕获得放行的门控）。
- **模型/动画能力面**：26.x submitCustomGeometry 逃生口、GUI 预览抽取新通道、taxonomy 与动画测试屏各代对齐。

## 修复

- **音频崩溃（1.16.5/1.20.1）**：`IAudioPlayer.isStopped` 与 vanilla `AbstractSoundInstance.isStopped` SRG 撞名导致进世界即 `NoSuchMethodError`——自有方法改名 `hasStopped`（1.16.5 覆写保名口径保留），全调用点同步。
- **1.19.4 适配（本次发布卡唯一源码变更）**：
  - 背包/创造 inventory 纸娃娃：注入点跟进 1.19.4 更名的 `renderEntityInInventoryFollowsMouse(PoseStack,...)`（原 1.19.3 六参签名误活）；
  - 盔甲槽位判定：1.19.4 起 `ArmorItem.getSlot()` → `getEquipmentSlot()`（原行对 1.19.4 误挂 `getSlot()`）。
  - 修复对其他 34 线经逐条目 CRC + javap 归一化对拍证明零行为变化。
- **六线修绿**：1.16.1（enqueueWork 分支撞车）、1.19/1.19.1/1.19.2（sendCommand 三档签名分档）、1.18.2/1.19（RenderCompat invalidate 下界）、1.16.1（camera 走查降级）——上述各线首次具备完整可构建产物。
- **dedicated server 启动崩（NeoForge 21.7+）**：`sendUnavailableMessage` 的 client 类引用在 dedicated server 形态被 dist 剥离后 CNFE——结构性隔离至 client 专用类。
- **光影包世界黑屏止血（1.20.1）**：装 Oculus/Iris + 光影包时原 GPU 直绘在真实驱动画空且吞回退、回退链又损坏——现光影包在场回退 CPU 原版管线（可用性优先；根治重推导在池，见已知限制）。
- **1.16.5 HUD 纸娃娃**：ExtraPlayerOverlay/MPR 拖拽预览缺口补齐（含 LazyModelAssembly SOE 守卫）。

## 已知限制（如实列出，不美化）

1. **26.2：GUI 预览面板模型区空白**——PiP 渲染重构（renderBuffers/MultiBufferSource 删除）后预览实体无自定义几何钩子，暂 no-op；面板 UI 本身正常。（debt-262-preview-pip）
2. **26.2：SIMD 快速顶点构建回退 CPU**——BufferBuilder 字段面换代致 native 动态映射初始化失败，自动回退纯 Java 路径（功能正常，性能回退）。（debt-262-simd-bufferbuilder）
3. **26.2：鞘翅/披风丢失附魔光膜层**——`getFoilBuffer` 26.2 改私有实例方法不可达，本体渲染正常。（debt-262-elytra-foil）
4. **26.2：GPU 渲染路径维持关闭**——FBO 获取口内化（GlDevice 包私有），门控暂闭、无功能回归。（debt-262-gpu-fbo）
5. **FirstPersonModel compat 缺席（NeoForge 21.9 ~ 21.11、26.3）**——vendor 尚未发布对应版本，自动走恒 false shim 档（FPM 不生效，其余功能正常）；26.1.x 真兼容不受影响。（debt-263-fpm-shim）
6. **装 Oculus/Iris + 光影包 = 世界内模型走 CPU 路径**——可用但非最优；GPU 直绘 + 回退链双失败的根治重推导（保性能接真 Iris compat）在池。（debt-iris-shader-compat）
7. **GUI 屏幕后景模糊降级（NeoForge 21.6 ~ 21.11、26.x）**——vanilla 每帧一次的背景模糊在这些代触发崩溃（21.6/21.10 实证、21.11/26.x 同机制预防性降级），统一降级为半透明暗化遮罩（失去模糊、保留暗化）。（debt-216-blur-degraded）
8. **1.16.5：无 GPU/Iris 渲染路径**——GL43 compute 管线在 1.16.5 不存在，恒走 SIMD/CPU 路径（SIMD 默认开启）。（debt-gui-iris-1165）同线 GUI 毛玻璃模糊缺席（debt-gui-blur-1165）、暂停页按钮 tooltip 未接链（debt-gui-tooltip-1165）。
9. **1.16.5：第三方 compat 矩阵缺席**——tacz/TLM/SBackpack 等兼容桥仅 1.20.1 口径，1.16.5 侧 `isModLoaded` 恒 false 走 shim。（debt-compat-matrix）
10. **1.16.5：biome molang 查询恒 false**——1.18+ 数据包标签体系不存在，按不匹配语义降级。（debt-biome-molang）
11. **26.3：`swing_time` molang 变量语义近似**——旧版=挥动起始 tick 计数，新版=动画进度取整，模型脚本存在半 tick 内偏移；swinging/swinging_arm/attack_time 均语义等价映射。（debt-263-swing-time-molang）
12. **1.16.x 产物为 Java 8 字节码**（unimined `--release 8`），功能面与其余线一致，但独立于各代 Java 工具链口径（详见 RELEASE-MANIFEST.md）。

## 验证口径

- 全线 `compileJava` + `buildAndCollect` 绿（串行 `--no-daemon --no-configuration-cache`）。
- 全线 `tools/audit_reobf_collision.py` **0 BROKEN**（38/38，exit 0）。
- tour 在案线（1.20.1/1.16.5/21.8/21.11/26.1/26.2/26.3/1.21.1，及 NFRT 三新线 1.20.2/1.20.3/1.20.5——nfrt-flatline 卡走查）直接采信 16 屏走查证据；其余 27 线未跑 tour（名单已标注）。
- 1.20.1 抽查：jar CRC 全过、natives 五平台齐、零 harness 条目、linux-x64 native 可 dlopen（真机 soak ~24min 0 NSME 先例在案）。
- 26.2/26.3 真机终验、Windows 真机（光影/真 GPU）终验归用户。
