package rip.ysm.legacy122.yui;

import java.io.IOException;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import rip.ysm.yui.YuiScreen;

/**
 * YUI 1.12.2 版本宿主壳：GuiScreen 生命周期 ↔ 中性 YuiScreen 生命周期哑转发
 * （ADR-YSM-UI-UNIFIED §5；交互逻辑全在中性 YuiScreen，本壳只做代际差异映射）。
 *
 * <p>1.12.2 代际差异面（vanilla-mc-1122 反编译实证）：mouseClicked 抛 IOException
 * （GuiScreen.java:311，L3-2 在产 LegacyModelSelectScreen.java:216 同款）；
 * mouseReleased protected（:324）；ScaledResolution 单参构造（:13）。
 *
 * <p>滚轮特例（lwjgl3ify d6af8e7 运行时实证 2026-09-22，RFB client 日志）：
 * GLFW 滚轮回调有 firing（lwjgl3ify DEBUG-MOUSE wheel 日志），addWheelEvent 也入队，
 * 但 {@code Mouse.next()} 从不向 GuiScreen.handleInput 弹出滚轮事件
 * （event_dwheel 只在 addWheelEvent/addButton/addMove 写入点间存留）——
 * 事件驱动的 handleMouseInput 面收不到滚轮。故本壳改用累积器
 * {@code Mouse.getDWheel()}（读即清零，经典 LWJGL2 API）在 updateScreen 逐 tick
 * 轮询喂给中性 YuiScreen。经典 LWJGL2 面同 API 兼容（M-U3 1.7.10 可复用同形）。
 *
 * <p>背景：drawScreen 先 drawDefaultBackground（尘土底/暗化）再渲染中性组件，
 * GuiIngameMenu 同序。
 */
public final class YuiScreenHost1122 extends GuiScreen {

    private final Supplier<? extends YuiScreen> factory;
    private YuiScreen screen;

    public YuiScreenHost1122(Supplier<? extends YuiScreen> factory) {
        this.factory = factory;
    }

    @Override
    public void initGui() {
        // resize/重入：全量重建（YuiScreen.open 清空组件表再 layout，无增量状态）
        this.screen = this.factory.get();
        this.screen.open(new YuiBackendGL1122(this.mc), this.width, this.height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.screen.render(mouseX, mouseY, partialTicks);
        // preview 契约收口（YuiBackendGL1122.preview 返回后深度关）：帧末恢复，
        // 关屏后世界渲染 GL 态干净（GuiContainer:73 关深度先例的对称收尾）
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.screen.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.screen.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == 1) {
            // Escape 关屏（mc.displayGuiScreen(null) 触发 onGuiClosed→close）
            this.mc.displayGuiScreen(null);
            return;
        }
        this.screen.keyPressed(keyCode, typedChar);
    }

    @Override
    public void updateScreen() {
        this.screen.tick();
        // 滚轮：lwjgl3ify(d6af8e7) 下 GLFW 滚轮回调有 firing（DEBUG-MOUSE 日志），但
        // Mouse.next() 不弹出滚轮事件、累积器 getDWheel() 恒 0（2026-09-22 运行时实证，
        // 见类注）；而同帧按钮事件可达。故逐 tick 轮询 getDWheel（健康 LWJGL2 面=经典
        // API；本环境实测恒 0，滚轮输入由消费侧回退路径兜底（Pager 按钮/▲▼ 键翻页）。
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            this.screen.mouseScrolled(mouseX, mouseY, wheel > 0 ? 1.0 : -1.0);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.screen.close();
    }
}
