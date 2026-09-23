package rip.ysm.legacy1710.yui1710;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;

import rip.ysm.yui.YuiScreen;

/**
 * YUI 1.7.10 版本宿主壳：GuiScreen 生命周期 ↔ 中性 YuiScreen 生命周期哑转发
 * （ADR-YSM-UI-UNIFIED §5；交互逻辑全在中性 YuiScreen，本壳只做代际差异映射）。
 *
 * <p>1.7.10 代际差异面（vanilla-mc-1710 反编译 + stable_12 methods.csv 亲证 2026-09-23）：
 * mouseClicked 不抛 IOException（GuiScreen.java:153，1122 起 :311 才 throws）；
 * keyTyped 同样不抛（:44）；鼠标释放方法 stable_12 定名 mouseReleased
 * （func_146286_b，methods.csv 实证——任务卡"mouseMovedOrUp"系旧 MCP 口径，
 * 语义同槽：move/up 合一分发，GuiScreen.java:158）；ScaledResolution 三参构造
 * （ScaledResolution.java:13）；initGui/drawScreen/handleMouseInput/updateScreen/
 * doesGuiPauseGame/onGuiClosed 两代同形。
 *
 * <p>滚轮：M-U1 1122 面运行时实证（lwjgl3ify d6af8e7）GLFW 滚轮回调有 firing 但
 * Mouse.next() 不向 GuiScreen 弹出滚轮事件、getDWheel 累积器恒 0——1.7.10 桥同款
 * 风险（M-U3 runClient 如实取证，预期阴性）。host 沿用 YuiScreenHost1122 的
 * getDWheel 逐 tick 轮询（健康 LWJGL2 面兼容），滚轮输入由消费侧 ▲▼ 键回退兜底。
 *
 * <p>背景：drawScreen 先 drawDefaultBackground（尘土底/暗化）再渲染中性组件。
 */
public final class YuiScreenHost1710 extends GuiScreen {

    private final Supplier<? extends YuiScreen> factory;
    private YuiScreen screen;

    public YuiScreenHost1710(Supplier<? extends YuiScreen> factory) {
        this.factory = factory;
    }

    @Override
    public void initGui() {
        // resize/重入：全量重建（YuiScreen.open 清空组件表再 layout，无增量状态）
        this.screen = this.factory.get();
        this.screen.open(new YuiBackendGL1710(this.mc), this.width, this.height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.screen.render(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.screen.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.screen.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
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
        // 滚轮：逐 tick 轮询 getDWheel 累积器（YuiScreenHost1122.updateScreen 同形；
        // lwjgl3ify 桥下预期恒 0=阴性证据，1.12.2 面 2026-09-22 实证口径一致）
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
