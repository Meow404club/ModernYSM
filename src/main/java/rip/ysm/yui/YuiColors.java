package rip.ysm.yui;

/**
 * YUI 设计常量——真相源为主线源码值（docs/ADR-YSM-UI-UNIFIED §5）：
 * PlayerModelScreen.render:567-569 / ModelButton.java:242-331 / FlatColorButton.java:72-94。
 * 1.12.2 L3-2 复刻（c96ca9e）已逐值核对相符。
 */
public final class YuiColors {

    /** 面板实底 #FF222222（PlayerModelScreen:567 fillGradient -14540254）。 */
    public static final int PANEL = 0xFF222222;

    /** 奶油强调线/hover 描边 #FFF3EFE0（PlayerModelScreen:569 -790560）。 */
    public static final int ACCENT = 0xFFF3EFE0;

    /** 扁平钮/列表行常态 #FF434242（FlatColorButton:78 -12369342）。 */
    public static final int ROW = 0xFF434242;

    /** 选中态 #FF1E90FF（FlatColorButton:76 -14774017）。 */
    public static final int SELECTED = 0xFF1E90FF;

    /** 主文字色 0xF3EFE0（15986656，FlatColorButton 文字）。 */
    public static final int TEXT = 0xF3EFE0;

    /** 弱化文字灰 0x777777（7829367，PlayerModelScreen 搜索占位/面包屑灰）。 */
    public static final int TEXT_DIM = 0x777777;

    /** 滚动拇指（半透明奶油，无轨道；LegacyModelSelectScreen:47 0x66F3EFE0）。 */
    public static final int SCROLL_THUMB = 0x66F3EFE0;

    private YuiColors() {
    }
}
