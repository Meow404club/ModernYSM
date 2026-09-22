package rip.ysm.yui;

/**
 * 文本标签。真相源：PlayerModelScreen:586-591（页码居中 15986656）/
 * :696-709（面包屑：常态 15986656、hover 16777120、灰 7829367）。
 * width/height 仅用于容器布局与命中参考，渲染本身不裁剪。
 */
public class YuiLabel extends YuiWidget {

    public int color = YuiColors.TEXT;
    public boolean shadow = true;
    public YuiBackend.Align align = YuiBackend.Align.LEFT;
    public String text;

    public YuiLabel(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        backend.drawText(this.text, this.x, this.y, this.color, this.shadow, this.align);
    }
}
