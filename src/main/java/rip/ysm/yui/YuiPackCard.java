package rip.ysm.yui;

/**
 * 模型包卡（52x90）：实底 + 包图标铺满 + 底部居中标签（≤2 行，split 宽 45）
 * + hover 1px 描边。形态真相源：主线 PackIconButton.renderWidget
 * （PackIconButton.java:72-114）——bg -6598176（:75）/图标铺满卡（:88）/
 * 标签 ≤2 行 45 宽 色 5592405（:95-107）/hover -1982745 描边（:108-113）。
 * 点击→进入该包（currentPath 导航，主线 PlayerModelScreen:513-518）。
 */
public class YuiPackCard extends YuiWidget {

    /** 包卡实底（PackIconButton.java:75 -6598176）。 */
    public static final int BG = 0xFF9B51E0;
    /** 包名文字色（:102 5592405）。 */
    public static final int LABEL = 0x555555;
    /** hover 描边（:109 -1982745）。 */
    public static final int OUTLINE = 0xFFE1BEE7;

    private final String label;
    private final YuiBackend.Texture icon;
    private final Runnable onPress;

    public YuiPackCard(int x, int y, String label, YuiBackend.Texture icon, Runnable onPress) {
        super(x, y, YuiModelCard.CARD_WIDTH, YuiModelCard.CARD_HEIGHT);
        this.label = label;
        this.icon = icon;
        this.onPress = onPress;
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        backend.fillRect(this.x, this.y, this.x + this.width, this.y + this.height, BG);
        if (this.icon != null) {
            backend.blit(this.icon, this.x, this.y, 0, 0, this.width, this.height);
        }
        // 标签 ≤2 行：两行 y+h-19/-10，单行 y+h-15（PackIconButton :95-107 同几何）
        java.util.List<String> lines = YuiLabel.split(backend, this.label, 45, 2);
        if (lines.size() > 1) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 19, LABEL);
            backend.drawTextCentered(lines.get(1), this.x + this.width / 2,
                    this.y + this.height - 10, LABEL);
        } else if (!lines.isEmpty()) {
            backend.drawTextCentered(lines.get(0), this.x + this.width / 2,
                    this.y + this.height - 15, LABEL);
        }
        if (isHovered(mouseX, mouseY)) {
            backend.outlineRect1px(this.x, this.y, this.width, this.height, OUTLINE);
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
