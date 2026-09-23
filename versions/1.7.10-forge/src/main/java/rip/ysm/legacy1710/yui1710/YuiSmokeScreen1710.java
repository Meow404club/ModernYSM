package rip.ysm.legacy1710.yui1710;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import rip.ysm.yui.YuiColors;
import rip.ysm.yui.YuiFlatButton;
import rip.ysm.yui.YuiLabel;
import rip.ysm.yui.YuiModelCard;
import rip.ysm.yui.YuiPanel;
import rip.ysm.yui.YuiScreen;
import rip.ysm.yui.YuiCardGrid;

/**
 * 【实验性质·验收冒烟屏】M-U3 GATE_1710UI 证据用：经 yui 中性组件 +
 * YuiBackendGL1710 后端渲染 面板+扁平钮(hover/选中态)+真模型卡网格
 * （L3b 起：default 前置+listBuiltinModels 枚举全部 builtin，卡内真实模型
 * 预览，逐 tick 惰性装载）+标签+可滚 ScrollView（▲▼ 回退）。
 * 形态与 YuiSmokeScreen1122（M-U1）逐项同构——1.7.10 面是渲染/输入桥的第二取证点。
 * 选择链/包导航/键盘导航不在此屏（L3a 模型选择屏边界）。
 *
 * <p>EXPERIMENTAL：本类与 OpenYSMStub.init 的挂点行随 1710 L3（模型枚举/网络同步
 * 落地）收编或整类删除，不进长期 API 面。
 *
 * <p>入口：独立 keybinding J（默认，Keyboard.KEY_J），ClientTickEvent 轮询 isPressed，
 * 仅世界内可开。1.7.10 FML 差异：TickEvent 挂 FMLCommonHandler.instance().bus()
 * （FMLCommonHandler.java:330 bus().post(new TickEvent.ClientTickEvent(...))，
 * 非 MinecraftForge.EVENT_BUS——1.7.10 双总线代际面）；键位注册
 * cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding（ClientRegistry.java:47）。
 * 交互证据：点击/hover/滚动均 System.out 打 [ysm-yui-smoke] 前缀行（日志佐证）。
 */
@SideOnly(Side.CLIENT)
public final class YuiSmokeScreen1710 {

    private static final KeyBinding SMOKE_KEY =
            new KeyBinding("key.openysm.yui_smoke", Keyboard.KEY_J, "key.categories.openysm");

    /** init（FMLInitializationEvent 调用链，客户端分支）调：注册键位+tick 轮询。 */
    public static void initExperimental() {
        ClientRegistry.registerKeyBinding(SMOKE_KEY);
        // 1.7.10 tick 事件在 FML 总线（见类注），MinecraftForge.EVENT_BUS 不发 ClientTickEvent。
        // 须传监听器实例：1.7.10 cpw EventBus 只有 register(Object)（EventBus.java:45，
        // 无 1.12.2 代的 register(Class) 重载）——传 Class 字面量会被当作监听器对象扫
        // java.lang.Class 的方法，零命中零报错静默失效（本卡 runClient 实测踩坑）。
        FMLCommonHandler.instance().bus().register(new TickListener());
    }

    // public：1.7.10 ASMEventHandler 生成的调用类在 cpw 包内，嵌套监听器类须 public
    // 可达（private 嵌套类 → IllegalAccessError，本卡 runClient 实测踩坑）
    public static final class TickListener {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !SMOKE_KEY.isPressed()) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                mc.displayGuiScreen(new YuiScreenHost1710(SmokeScreen::new));
            }
        }
    }

    /** 冒烟屏内容（中性组件布局；坐标全 GUI 逻辑坐标）。 */
    private static final class SmokeScreen extends YuiScreen {

        private YuiCardGrid grid;
        /** 非 default 可用模型逐 tick 惰性装载队列（122 屏同式；loadModel 幂等+负缓存）。 */
        private final java.util.List<String> loadQueue =
                new java.util.ArrayList<String>(rip.ysm.legacy1710.LegacyModelLoader.listBuiltinModels());

        @Override
        public void tick() {
            if (!this.loadQueue.isEmpty()) {
                rip.ysm.legacy1710.LegacyModelLoader.loadModel(this.loadQueue.remove(0));
            }
        }

        /** 键控翻页（</> ；主路径=pager 钮，同 122 屏语言）。 */
        @Override
        public boolean keyPressed(int keyCode, char typedChar) {
            if (keyCode == Keyboard.KEY_COMMA && this.grid != null) {
                this.grid.flipPage(-1);
                return true;
            }
            if (keyCode == Keyboard.KEY_PERIOD && this.grid != null) {
                this.grid.flipPage(1);
                return true;
            }
            return super.keyPressed(keyCode, typedChar);
        }

        @Override
        protected void layout() {
            int pw = 420;
            int ph = 235;
            int px = (this.width - pw) / 2;
            int py = (this.height - ph) / 2;

            add(new YuiPanel(px, py, pw, ph));
            YuiLabel title = new YuiLabel(this.width / 2, py + 8, 0, 10, "YUI Smoke 1710 (experimental)");
            title.align = rip.ysm.yui.YuiBackend.Align.CENTER;
            add(title);

            // 四枚扁平钮：toggle 自身选中态 / 恒选中演示 / 关屏（Done 语言）/ Select。
            // 放底部翻页行（左段 0..146+右段 360..416）：顶行曾与卡网格相撞（Close 被卡盖住）。
            // L2b 第四枚 Select：requestSelect 链冒烟（availableModels 首项→C2S→
            // 服务端 netty 线程校验/持久化/指派→syncTo 回环）；L3a 模型选择屏收编。
            // rebase 冲突解决（review-merge）：L3b 150b904 把按钮行挪底行后，Select 原
            // 顶行位落在卡网格上——移底行右段（pager next 右侧空档），语义不变。
            final YuiFlatButton[] holder = new YuiFlatButton[1];
            holder[0] = new YuiFlatButton(px + 8, py + 215, 42, 14, "Tog", new Runnable() {
                @Override
                public void run() {
                    holder[0].selected = !holder[0].selected;
                    System.out.println("[ysm-yui-smoke] toggle -> selected=" + holder[0].selected);
                }
            });
            add(holder[0]);
            YuiFlatButton selected = new YuiFlatButton(px + 54, py + 215, 42, 14, "Sel", null);
            selected.selected = true;
            add(selected);
            add(new YuiFlatButton(px + 100, py + 215, 42, 14, "X", new Runnable() {
                @Override
                public void run() {
                    System.out.println("[ysm-yui-smoke] close");
                    Minecraft.getMinecraft().displayGuiScreen((GuiScreen) null);
                }
            }));
            add(new YuiFlatButton(px + 360, py + 215, 52, 14, "Select", new Runnable() {
                @Override
                public void run() {
                    java.util.List<String> ids = rip.ysm.legacy1710.LegacySyncChannel.availableModels();
                    String pick = ids.isEmpty() ? null : ids.get(0);
                    System.out.println("[ysm-yui-smoke] select -> available=" + ids.size()
                            + " pick=" + pick);
                    if (pick != null) {
                        rip.ysm.legacy1710.LegacySyncChannel.requestSelect(pick);
                    }
                }
            }));

            // 卡网格（L3b：Registry 枚举接真数据——default 主面前置+
            // LegacyModelLoader.listBuiltinModels 两级 id compareTo 序；每卡=
            // LegacyPreview1710.card 真实模型预览，preview_animation 采样驱动、
            // 空串=绑定位静像、12_little=disable false+hold_on_last_frame+hover
            // 三态样本）。用 YuiCardGrid 5x2+翻页（122 真屏同款几何：
            // PlayerModelScreen.init :506-538 slotX/slotY+FlatColorButton :488-501）
            // 而非 ScrollView——共享 YuiScrollView.render 子件视口系坐标×内容系
            // 鼠标配对错位（滚动后卡内 hover 永错位，缺陷已报主会话），且翻页
            // 正是 L3a 真屏形态。选择链无（L3a 功能化边界），点卡只打日志。
            java.util.List<String> ids = new java.util.ArrayList<String>();
            ids.add(null); // default 主面
            ids.addAll(rip.ysm.legacy1710.LegacyModelLoader.listBuiltinModels());
            this.grid = new YuiCardGrid(px + 143, py + 28, py + 215);
            for (int i = 0; i < ids.size(); i++) {
                final String id = ids.get(i);
                String label = id == null ? "default"
                        : id.substring(id.lastIndexOf('/') + 1);
                this.grid.addCard(new YuiModelCard(0, 0, label, LegacyPreview1710.card(id),
                        new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("[ysm-yui-smoke] card " + id + " picked");
                            }
                        }));
            }
            this.grid.setPage(ids.size() > 10 ? 1 : 0); // 默认露出 wine_fox 页（12_little 可见）
            add(this.grid);

        }
    }

    private YuiSmokeScreen1710() {
    }
}
