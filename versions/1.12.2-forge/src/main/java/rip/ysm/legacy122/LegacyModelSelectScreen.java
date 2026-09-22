package rip.ysm.legacy122;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * 1.12.2 模型选择 GUI（L3-2；观感翻新对齐主线设计语言）。
 *
 * 功能链不变：keybinding（默认 Y，ClientRegistry.registerKeyBinding，forge-1.12.x
 * ClientRegistry.java:60）+ ClientTickEvent 轮询 isPressed（TickEvent.java:53，1.12.2
 * 无 KeyInputEvent）开屏；列表首项 default + 服务端登录下发的可用模型（两级 id），
 * 点击→LegacySyncChannel.requestSelect→服务端校验/持久化/广播。
 *
 * 视觉（只读参考主线配色常量复刻，不 import >=1.17 类，全部 1.12.2 Gui.drawRect 面）：
 * - 面板实底 #222222（PlayerModelScreen.render fillGradient -14540254 同值）+
 *   标题下奶油 1px 强调线（PlayerModelScreen :569 竖版强调线同语言）；
 * - 列表行=FlatColorButton 语言：常态 #434242（-12369342）/选中 #1E90FF（-14774017）/
 *   hover 奶油 1px 描边 #F3EFE0（-790560）/行文字奶油 0xF3EFE0（15986656），左对齐 x+4 垂直居中；
 * - Done 按钮=FlatIconButton 语言（115x15 扁平，同上 hover 描边）；
 * - 滚动=手写 wheel（GuiSlot 弃用：其 dirt 底/立体选区与扁平面板语言冲突），
 *   行区 GL11 scissor 裁剪（glScissor 坐标=gui*scale，Y 翻转 displayHeight-）。
 */
@SideOnly(Side.CLIENT)
public final class LegacyModelSelectScreen extends GuiScreen {

    // 主线配色常量（值取自 FlatColorButton/FlatIconButton/PlayerModelScreen 源码，见类注）
    private static final int PANEL = 0xFF222222;
    private static final int ROW = 0xFF434242;
    private static final int ROW_SELECTED = 0xFF1E90FF;
    private static final int ACCENT = 0xFFF3EFE0;
    private static final int ROW_TEXT = 0xF3EFE0;
    private static final int SCROLL_THUMB = 0x66F3EFE0;
    private static final int BUTTON_W = 115;
    private static final int BUTTON_H = 15;
    private static final int SLOT_HEIGHT = 14;

    private static final KeyBinding OPEN_GUI =
            new KeyBinding("key.openysm.select_model", Keyboard.KEY_Y, "key.categories.openysm");

    // 服务端登录时 LegacyModelListPacket 下发（volatile 引用换入，读侧无锁）
    private static volatile List<String> available = Collections.emptyList();

    private int scrollAmount;
    private int hoveredRow = -1;
    private String selected = LegacyModelRegistry.DEFAULT_MODEL_ID;

    public LegacyModelSelectScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            selected = LegacyModelRegistry.modelIdOf(mc.player.getUniqueID());
        }
    }

    /** init（FMLInitializationEvent，客户端侧）调：注册键位+tick 轮询。 */
    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUI);
        MinecraftForge.EVENT_BUS.register(LegacyModelSelectScreen.class);
        // EXPERIMENTAL（M-U1 冒烟屏，独立 J 键）：M-U2 收编冒烟屏时一并撤
        rip.ysm.legacy122.yui.YuiSmokeScreen1122.initExperimental();
    }

    /** S2C 可用模型列表落点（LegacyModelListPacket.Handler）。 */
    public static void setAvailable(List<String> ids) {
        available = ids == null ? Collections.<String>emptyList() : ids;
    }

    /** 键位轮询（1.12.2 无 InputEvent.KeyInputEvent，ClientTickEvent+isPressed 标准面）。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !OPEN_GUI.isPressed()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new LegacyModelSelectScreen());
        }
    }

    // ==================== 几何 ====================

    private int panelLeft() {
        return this.width / 2 - 130;
    }

    private int panelTop() {
        return 28;
    }

    private int panelBottom() {
        return this.height - 48;
    }

    private int listLeft() {
        return panelLeft() + 6;
    }

    private int listRight() {
        return panelLeft() + 260 - 16; // 右侧留 8px 滚动条区
    }

    private int listTop() {
        return panelTop() + 26;
    }

    private int listBottom() {
        return panelBottom() - 6;
    }

    private int rowCount() {
        return 1 + available.size();
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * SLOT_HEIGHT - (listBottom() - listTop()));
    }

    private String idAt(int index) {
        return index <= 0 ? LegacyModelRegistry.DEFAULT_MODEL_ID : available.get(index - 1);
    }

    private int rowAt(int mouseX, int mouseY) {
        if (mouseX < listLeft() || mouseX >= listRight()
                || mouseY < listTop() || mouseY >= listBottom()) {
            return -1;
        }
        int row = (mouseY - listTop() + this.scrollAmount) / SLOT_HEIGHT;
        return row >= 0 && row < rowCount() ? row : -1;
    }

    // ==================== 绘制 ====================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        // 面板实底 + 标题 + 奶油强调线（主线面板语言）
        drawRect(panelLeft(), panelTop(), panelLeft() + 260, panelBottom(), PANEL);
        this.drawCenteredString(this.fontRenderer, "YSM Models", this.width / 2, panelTop() + 8, ROW_TEXT);
        drawRect(panelLeft() + 6, panelTop() + 19, panelLeft() + 254, panelTop() + 20, ACCENT);

        this.hoveredRow = rowAt(mouseX, mouseY);
        int scroll = this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, maxScroll()));

        // 行区 scissor 裁剪（glScissor 原点左下：glY = displayHeight - guiBottom*scale）
        ScaledResolution sr = new ScaledResolution(this.mc);
        int s = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(listLeft() * s, this.mc.displayHeight - listBottom() * s,
                (listRight() - listLeft()) * s, (listBottom() - listTop()) * s);
        for (int i = 0; i < rowCount(); i++) {
            int ry = listTop() + i * SLOT_HEIGHT - scroll;
            int rh = SLOT_HEIGHT - 2;
            if (ry + rh < listTop() || ry > listBottom()) {
                continue;
            }
            boolean sel = idAt(i).equals(this.selected);
            drawRect(listLeft(), ry, listRight(), ry + rh, sel ? ROW_SELECTED : ROW);
            if (i == this.hoveredRow) {
                // hover 奶油 1px 描边（FlatColorButton hoveredOrFocused 四边同语言）
                drawRect(listLeft(), ry, listRight(), ry + 1, ACCENT);
                drawRect(listLeft(), ry + rh - 1, listRight(), ry + rh, ACCENT);
                drawRect(listLeft(), ry + 1, listLeft() + 1, ry + rh - 1, ACCENT);
                drawRect(listRight() - 1, ry + 1, listRight(), ry + rh - 1, ACCENT);
            }
            this.fontRenderer.drawStringWithShadow(idAt(i), listLeft() + 4, ry + (rh - 8) / 2, ROW_TEXT);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 扁平滚动条（半透明奶油拇指，无轨道）
        if (maxScroll() > 0) {
            int trackH = listBottom() - listTop();
            int thumbH = Math.max(8, trackH * trackH / (rowCount() * SLOT_HEIGHT));
            int thumbY = listTop() + scroll * (trackH - thumbH) / maxScroll();
            drawRect(listRight() + 3, thumbY, listRight() + 5, thumbY + thumbH, SCROLL_THUMB);
        }

        // Done 扁平按钮（FlatIconButton 115x15 语言）
        drawFlatButton(buttonX(), buttonY(), I18n.format("gui.done"), mouseX, mouseY);
    }

    private int buttonX() {
        return this.width / 2 - BUTTON_W / 2;
    }

    private int buttonY() {
        return panelBottom() + 8;
    }

    private void drawFlatButton(int x, int y, String label, int mouseX, int mouseY) {
        drawRect(x, y, x + BUTTON_W, y + BUTTON_H, ROW);
        boolean hover = mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
        if (hover) {
            drawRect(x, y, x + BUTTON_W, y + 1, ACCENT);
            drawRect(x, y + BUTTON_H - 1, x + BUTTON_W, y + BUTTON_H, ACCENT);
            drawRect(x, y + 1, x + 1, y + BUTTON_H - 1, ACCENT);
            drawRect(x + BUTTON_W - 1, y + 1, x + BUTTON_W, y + BUTTON_H - 1, ACCENT);
        }
        this.drawCenteredString(this.fontRenderer, label, x + BUTTON_W / 2, y + (BUTTON_H - 8) / 2, ROW_TEXT);
    }

    // ==================== 输入 ====================

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            // 与翻新前 elementClicked 语义一致：点击即选即发（服务端幂等）
            this.selected = idAt(row);
            LegacySyncChannel.requestSelect(this.selected);
            System.out.println("[ysm-legacy122] gui selected: " + this.selected);
            return;
        }
        int bx = buttonX();
        int by = buttonY();
        if (mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= by && mouseY < by + BUTTON_H) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            // GuiSlot 滚轮手感同款：每格 slotHeight/2
            this.scrollAmount += (wheel > 0 ? -1 : 1) * (SLOT_HEIGHT / 2);
            this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, maxScroll()));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // 选择期间世界不停（GuiIngameMenu 同语义）
    }
}
