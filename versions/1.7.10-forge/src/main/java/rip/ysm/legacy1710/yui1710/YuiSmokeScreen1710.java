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
import rip.ysm.yui.YuiScrollView;

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

        private YuiScrollView scroll;
        /** 非 default 可用模型逐 tick 惰性装载队列（122 屏同式；loadModel 幂等+负缓存）。 */
        private final java.util.List<String> loadQueue =
                new java.util.ArrayList<String>(rip.ysm.legacy1710.LegacyModelLoader.listBuiltinModels());

        @Override
        public void tick() {
            if (!this.loadQueue.isEmpty()) {
                rip.ysm.legacy1710.LegacyModelLoader.loadModel(this.loadQueue.remove(0));
            }
        }

        @Override
        public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
            boolean r = super.mouseScrolled(mouseX, mouseY, delta);
            if (r && this.scroll != null) {
                System.out.println("[ysm-yui-smoke] scroll -> scrollY=" + this.scroll.scrollY);
            }
            return r;
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

            // 三枚扁平钮：toggle 自身选中态 / 恒选中演示 / 关屏（Done 语言）
            final YuiFlatButton[] holder = new YuiFlatButton[1];
            holder[0] = new YuiFlatButton(px + 8, py + 28, 70, 14, "Toggle", new Runnable() {
                @Override
                public void run() {
                    holder[0].selected = !holder[0].selected;
                    System.out.println("[ysm-yui-smoke] toggle -> selected=" + holder[0].selected);
                }
            });
            add(holder[0]);
            YuiFlatButton selected = new YuiFlatButton(px + 86, py + 28, 70, 14, "Selected", null);
            selected.selected = true;
            add(selected);
            add(new YuiFlatButton(px + 164, py + 28, 70, 14, "Close", new Runnable() {
                @Override
                public void run() {
                    System.out.println("[ysm-yui-smoke] close");
                    Minecraft.getMinecraft().displayGuiScreen((GuiScreen) null);
                }
            }));

            // 可滚内容：真模型卡网格（L3b：Registry 枚举接真数据——default 主面
            // 前置+LegacyModelLoader.listBuiltinModels 两级 id compareTo 序；
            // 每卡=LegacyPreview1710.card 真实模型预览，preview_animation 采样驱动、
            // 空串=绑定位静像、12_little=disable false+hold_on_last_frame+hover
            // 三态样本）+ 3 行文本居尾，验证 scissor 裁剪+偏移+拇指+槽外零污染。
            // 选择链无（L3a 功能化边界），点卡只打日志。
            int cols = 5;
            int pitchX = 55;
            int pitchY = 95; // 卡 90 + 行距 5
            int cardTop = py + 48;
            int gridW = cols * pitchX - 3; // 末列按卡宽 52 收口
            java.util.List<String> ids = new java.util.ArrayList<String>();
            ids.add(null); // default 主面
            ids.addAll(rip.ysm.legacy1710.LegacyModelLoader.listBuiltinModels());
            int gridRows = (ids.size() + cols - 1) / cols;
            int cardsH = gridRows * pitchY - 5;
            this.scroll = new YuiScrollView(px + 8, cardTop, pw - 16 - 36, ph - 48 - 26,
                    cardsH + 3 * 14);
            int gridX = px + 8 + (this.scroll.width - gridW) / 2;
            for (int i = 0; i < ids.size(); i++) {
                final String id = ids.get(i);
                String label = id == null ? "default"
                        : id.substring(id.lastIndexOf('/') + 1);
                this.scroll.add(new YuiModelCard(gridX + (i % cols) * pitchX,
                        cardTop + (i / cols) * pitchY, label, LegacyPreview1710.card(id),
                        new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("[ysm-yui-smoke] card " + id + " picked");
                            }
                        }));
            }
            int rows = 3;
            for (int i = 0; i < rows; i++) {
                this.scroll.add(new YuiLabel(px + 14, cardTop + cardsH + i * 14 + 3, pw - 60, 10,
                        "Row " + (i + 1) + " / " + rows));
            }
            add(this.scroll);

            // 滚轮回退（lwjgl3ify 桥下滚轮事件不达 GuiScreen 的预期，YuiScreenHost1710 类注）：
            // ▲▼ 走与滚轮完全相同的 mouseScrolled 分发路径（坐标取视口内一点），证明滚动机制本体
            final YuiScrollView scrollRef = this.scroll;
            int bx = px + pw - 40;
            add(new YuiFlatButton(bx, cardTop, 32, 14, "^", new Runnable() {
                @Override
                public void run() {
                    SmokeScreen.this.mouseScrolled(scrollRef.x + 10, scrollRef.y + 10, 1.0);
                }
            }));
            add(new YuiFlatButton(bx, py + ph - 40, 32, 14, "v", new Runnable() {
                @Override
                public void run() {
                    SmokeScreen.this.mouseScrolled(scrollRef.x + 10, scrollRef.y + 10, -1.0);
                }
            }));

            YuiLabel hint = new YuiLabel(this.width / 2, py + ph - 16, 0, 10,
                    "Esc=close  wheel/uv=scroll");
            hint.color = YuiColors.TEXT_DIM;
            hint.align = rip.ysm.yui.YuiBackend.Align.CENTER;
            add(hint);
        }
    }

    private YuiSmokeScreen1710() {
    }
}
