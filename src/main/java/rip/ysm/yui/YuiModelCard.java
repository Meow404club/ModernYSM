package rip.ysm.yui;

import java.util.List;

/**
 * 模型卡（52x90）：实底 + 卡内预览槽（scissor，槽高 h-20）+ 底部居中标签
 * （≤2 行，split 宽 45）+ 奶油 1px 描边（hover 或选中常显）。
 * 形态真相源：主线 ModelButton.renderWidget（ModelButton.java:242-331）——
 * bg -12369342(:262)/预览槽 scissor h-20(:277-293)/标签 ≤2 行 45 宽 色 15986656
 * (:302-314)/hover -790560 1px 描边(:315-320)。r2：选中态弃蓝底（主线卡片语言
 * 无选中蓝底，蓝底是 r1 发明）改为描边常显——主线当前模型语义由左栏大预览承载。
 * 锁定遮罩/star 图标不在此期（消费面无此概念）。
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
        backend.fillRect(this.x, this.y, this.x + this.width, this.y + this.height, YuiColors.ROW);
        if (this.preview != null) {
            backend.preview(this.preview, this.x, this.y,
                    this.x + this.width, this.y + SLOT_HEIGHT, mouseX, mouseY, partialTick);
        }
        // 标签 ≤2 行：两行 y+h-19/-10，单行 y+h-15（ModelButton :302-314 同几何）
        List<String> lines = YuiLabel.split(backend, this.label, 45, 2);
        if (lines.size() > 1) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 19, YuiColors.TEXT);
            backend.drawTextCentered(lines.get(1), this.x + this.width / 2,
                    this.y + this.height - 10, YuiColors.TEXT);
        } else if (!lines.isEmpty()) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 15, YuiColors.TEXT);
        }
        // hover 描边（ModelButton :315-320 同色）；选中=描边常显（主线卡片无选中蓝底）
        if (this.selected || isHovered(mouseX, mouseY)) {
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
}
