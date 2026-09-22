package rip.ysm.yui;

/**
 * 扁平按钮：常态实底/选中蓝底/hover 奶油 1px 四边描边/居中文字。
 * 形态真相源：FlatColorButton.java:72-94（常态 -12369342 / 选中 -14774017 /
 * hover -790560 四边 / 文字 15986656）；Done 语言 115x15（FlatIconButton.java:36）。
 */
public class YuiFlatButton extends YuiWidget {

    public boolean selected;

    private final String label;
    private final Runnable onPress;

    public YuiFlatButton(int x, int y, int width, int height, String label, Runnable onPress) {
        super(x, y, width, height);
        this.label = label;
        this.onPress = onPress;
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        backend.fillRect(this.x, this.y, this.x + this.width, this.y + this.height,
                this.selected ? YuiColors.SELECTED : YuiColors.ROW);
        if (isHovered(mouseX, mouseY)) {
            backend.outlineRect1px(this.x, this.y, this.width, this.height, YuiColors.ACCENT);
        }
        backend.drawTextCentered(this.label, this.x + this.width / 2,
                this.y + (this.height - 8) / 2, YuiColors.TEXT);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            if (this.onPress != null) {
                this.onPress.run();
            }
            return true;
        }
        return false;
    }
}
