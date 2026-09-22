package rip.ysm.yui;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 折行：贪心逐字符、按 {@code textWidth} 量宽、截断至 maxLines。
     * 主线 font.split(msg, width) 的中性近似（yui 树无 Font 面）；
     * 超宽截断与主线同语义（超出 maxLines 静默丢弃）。
     * YuiModelCard 标签折行同式（r2 起共用本实现，不再各写一份）。
     */
    public static List<String> split(YuiBackend backend, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            cur.append(text.charAt(i));
            if (backend.textWidth(cur.toString()) > maxWidth && cur.length() > 1) {
                cur.deleteCharAt(cur.length() - 1);
                lines.add(cur.toString());
                if (lines.size() == maxLines) {
                    return lines;
                }
                cur.setLength(0);
            }
        }
        if (cur.length() > 0 || lines.isEmpty()) {
            lines.add(cur.toString());
        }
        return lines;
    }
}
