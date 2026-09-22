package rip.ysm.yui;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型卡（52x90）：实底 + 卡内预览槽（scissor，槽高 h-20）+ 底部居中标签
 * （≤2 行，split 宽 45）+ hover 奶油 1px 描边。
 * 形态真相源：主线 ModelButton.renderWidget（ModelButton.java:242-331）——
 * bg -12369342(:262)/预览槽 scissor h-20(:277-293)/标签 ≤2 行 45 宽 色 15986656
 * (:302-314)/hover -790560 1px 描边(:315-320)；选中蓝底为消费面选择语义
 * （FlatColorButton:76 -14774017 同值）。锁定遮罩/star 图标不在此期（消费面无此概念）。
 *
 * <p>预览槽经 {@link YuiBackend#preview} 下放版本后端；后端契约：返回后 scissor
 * 已弹出、深度测试已关（模型 z 写深度挡后续 2D，GuiContainer:73 先例），
 * 卡内标签/描边按叠序直接画。
 */
public class YuiModelCard extends YuiWidget {

    public static final int CARD_WIDTH = 52;
    public static final int CARD_HEIGHT = 90;
    /** 预览槽高 = 卡高 - 标签区 20（ModelButton :278-283 同值）。 */
    public static final int SLOT_HEIGHT = CARD_HEIGHT - 20;

    /** 选中态（消费面写回，渲染蓝底）。 */
    public boolean selected;

    private final String label;
    private final YuiPreview preview;
    private final Runnable onPress;

    public YuiModelCard(int x, int y, String label, YuiPreview preview, Runnable onPress) {
        super(x, y, CARD_WIDTH, CARD_HEIGHT);
        this.label = label;
        this.preview = preview;
        this.onPress = onPress;
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        backend.fillRect(this.x, this.y, this.x + this.width, this.y + this.height,
                this.selected ? YuiColors.SELECTED : YuiColors.ROW);
        if (this.preview != null) {
            backend.preview(this.preview, this.x, this.y,
                    this.x + this.width, this.y + SLOT_HEIGHT, mouseX, mouseY, partialTick);
        }
        // 标签 ≤2 行：两行 y+h-19/-10，单行 y+h-15（ModelButton :302-314 同几何）
        List<String> lines = splitLabel(backend, this.label, 45);
        if (lines.size() > 1) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 19, YuiColors.TEXT);
            backend.drawTextCentered(lines.get(1), this.x + this.width / 2,
                    this.y + this.height - 10, YuiColors.TEXT);
        } else if (!lines.isEmpty()) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 15, YuiColors.TEXT);
        }
        if (isHovered(mouseX, mouseY)) {
            backend.outlineRect1px(this.x, this.y, this.width, this.height, YuiColors.ACCENT);
        }
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

    /**
     * 标签折行：贪心逐字符、宽度 45、截断至 2 行。主线 font.split(msg,45) 的
     * 中性近似（yui 树无 Font 面，只有 {@link YuiBackend#textWidth}）；
     * 超宽截断与主线同语义（>2 行静默丢弃）。
     */
    private static List<String> splitLabel(YuiBackend backend, String text, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            cur.append(text.charAt(i));
            if (backend.textWidth(cur.toString()) > maxWidth && cur.length() > 1) {
                cur.deleteCharAt(cur.length() - 1);
                lines.add(cur.toString());
                if (lines.size() == 2) {
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
