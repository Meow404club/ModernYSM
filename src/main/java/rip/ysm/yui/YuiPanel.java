package rip.ysm.yui;

/**
 * 面板：暗实底 + 可选奶油 1px 强调线（标题下）。
 * 形态真相源：PlayerModelScreen.render:567-569（#FF222222 实底 + -790560 强调线），
 * 强调线几何对位 1.12.2 L3-2（LegacyModelSelectScreen:149-151：面板 y+19，左右内缩 6px）。
 */
public class YuiPanel extends YuiWidget {

    /** true=顶部画奶油 1px 强调线（y+19，左右内缩 6）。 */
    public boolean accentLine = true;

    public YuiPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        backend.fillRect(this.x, this.y, this.x + this.width, this.y + this.height, YuiColors.PANEL);
        if (this.accentLine) {
            backend.fillRect(this.x + 6, this.y + 19, this.x + this.width - 6, this.y + 20, YuiColors.ACCENT);
        }
    }
}
