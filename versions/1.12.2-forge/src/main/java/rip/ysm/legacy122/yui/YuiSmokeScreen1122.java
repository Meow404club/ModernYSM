package rip.ysm.legacy122.yui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import rip.ysm.yui.YuiColors;
import rip.ysm.yui.YuiFlatButton;
import rip.ysm.yui.YuiLabel;
import rip.ysm.yui.YuiPanel;
import rip.ysm.yui.YuiScreen;
import rip.ysm.yui.YuiScrollView;

/**
 * 【实验性质·验收冒烟屏】M-U1 GATE_YUI 证据用：经 yui 中性组件 +
 * YuiBackendGL1122 后端渲染 面板+扁平钮(hover/选中态)+标签+可滚 ScrollView。
 *
 * <p>EXPERIMENTAL：本类与 LegacyModelSelectScreen.init() 尾部的挂点行随 M-U2
 * （1.12.2 模型选择屏重做）收编或整类删除，不进长期 API 面。
 *
 * <p>入口：独立 keybinding J（默认，Keyboard.KEY_J，避免与既有 Y 键行为面混淆），
 * ClientTickEvent 轮询 isPressed（1.12.2 无 KeyInputEvent，L3-2 同面），仅世界内可开。
 * 交互证据：点击/hover/滚动均 System.out 打 [ysm-yui-smoke] 前缀行（日志佐证）。
 */
@SideOnly(Side.CLIENT)
public final class YuiSmokeScreen1122 {

    private static final KeyBinding SMOKE_KEY =
            new KeyBinding("key.openysm.yui_smoke", Keyboard.KEY_J, "key.categories.openysm");

    /** init（FMLInitializationEvent 调用链，客户端侧）调：注册键位+tick 轮询。 */
    public static void initExperimental() {
        ClientRegistry.registerKeyBinding(SMOKE_KEY);
        MinecraftForge.EVENT_BUS.register(YuiSmokeScreen1122.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SMOKE_KEY.isPressed()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new YuiScreenHost1122(SmokeScreen::new));
        }
    }

    /** 冒烟屏内容（中性组件布局；坐标全 GUI 逻辑坐标）。 */
    private static final class SmokeScreen extends YuiScreen {

        @Override
        protected void layout() {
            int pw = 250;
            int ph = 190;
            int px = (this.width - pw) / 2;
            int py = (this.height - ph) / 2;

            add(new YuiPanel(px, py, pw, ph));
            YuiLabel title = new YuiLabel(this.width / 2, py + 8, 0, 10, "YUI Smoke (experimental)");
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

            // 可滚列表：20 行溢出视口，验证 scissor 裁剪+偏移+拇指
            int rows = 20;
            YuiScrollView scroll = new YuiScrollView(px + 8, py + 48, pw - 16, ph - 48 - 26,
                    rows * 14);
            for (int i = 0; i < rows; i++) {
                scroll.add(new YuiLabel(px + 14, py + 48 + i * 14 + 3, pw - 32, 10,
                        "Row " + (i + 1) + " / " + rows));
            }
            add(scroll);

            YuiLabel hint = new YuiLabel(this.width / 2, py + ph - 16, 0, 10,
                    "Esc=close  wheel=scroll");
            hint.color = YuiColors.TEXT_DIM;
            hint.align = rip.ysm.yui.YuiBackend.Align.CENTER;
            add(hint);
        }
    }

    private YuiSmokeScreen1122() {
    }
}
