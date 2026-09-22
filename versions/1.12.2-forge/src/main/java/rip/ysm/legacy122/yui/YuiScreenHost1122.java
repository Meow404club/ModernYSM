package rip.ysm.legacy122.yui;

import java.io.IOException;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Mouse;

import rip.ysm.yui.YuiScreen;

/**
 * YUI 1.12.2 版本宿主壳：GuiScreen 生命周期 ↔ 中性 YuiScreen 生命周期哑转发
 * （ADR-YSM-UI-UNIFIED §5；交互逻辑全在中性 YuiScreen，本壳只做代际差异映射）。
 *
 * <p>1.12.2 代际差异面（vanilla-mc-1122 反编译实证）：mouseClicked 抛 IOException
 * （GuiScreen.java:311，L3-2 在产 LegacyModelSelectScreen.java:216 同款）；
 * mouseReleased protected（:324）；ScaledResolution 单参构造（:13）；
 * 滚轮走 handleMouseInput + Mouse.getEventDWheel（GuiSlot 同面）。
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
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            // 坐标换算同 GuiScreen.handleMouseInput 本地数学（GuiScreen.java:369 区段）
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.screen.mouseScrolled(mouseX, mouseY, wheel > 0 ? 1.0 : -1.0);
        }
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
