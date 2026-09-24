package rip.ysm.yui;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本中性屏根：生命周期 open(layout)/render/close/tick + 事件面
 * click/release/scroll/key（ADR-1 原语清单的事件/生命周期半区）。
 * 版本宿主壳（YuiScreenHost1122 等，版本树内哑转发）把这些面映射到
 * GuiScreen/Screen 生命周期；命中测试/焦点/hover/滚动等交互逻辑全在本层。
 *
 * <p>resize/重入语义：host 每次生命周期初始化都 new 屏 + open()，open 清空组件表
 * 再调 {@link #layout()} 全量重建——与主线 Screen initGui 重入同构，无增量状态。
 */
public abstract class YuiScreen {

    public YuiBackend backend;
    public int width;
    public int height;

    private final List<YuiWidget> widgets = new ArrayList<YuiWidget>();

    /** host 生命周期初始化点（initGui/init 同形）。 */
    public final void open(YuiBackend backend, int width, int height) {
        this.backend = backend;
        this.width = width;
        this.height = height;
        this.widgets.clear();
        this.layout();
    }

    /** 布局钩子：消费侧在此摆放并 {@link #add} 组件（坐标全 GUI 逻辑坐标）。 */
    protected abstract void layout();

    protected final void add(YuiWidget widget) {
        this.widgets.add(widget);
    }

    /** 每游戏 tick（host updateScreen 同形）。 */
    public void tick() {
    }

    /** host 关屏点（onGuiClosed 同形）。 */
    public void close() {
    }

    /** 每帧渲染（host drawScreen 剔除背景后调用，背景由 host 画）。 */
    public void render(int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < this.widgets.size(); i++) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible) {
                w.render(this.backend, mouseX, mouseY, partialTick);
            }
        }
        renderTooltip(mouseX, mouseY);
    }

    /**
     * hover tooltip 后置 pass（wave-d-b4 包描述面）：取最上层可见且带 tooltip 的
     * 悬停组件，在鼠标旁画描述框。原语全走 backend（版本中性），配色/几何=
     * vanilla 1.12 GuiScreen.drawHoveringText 同款（bg 0xF0100010 / 紫描边 /
     * 白字带影，行高 10，鼠标偏移 +12/-12 屏缘钳制）。
     */
    private void renderTooltip(int mouseX, int mouseY) {
        YuiWidget hovered = null;
        for (int i = this.widgets.size() - 1; i >= 0; i--) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible && w.tooltip != null && w.isHovered(mouseX, mouseY)) {
                hovered = w;
                break;
            }
        }
        if (hovered == null) {
            return;
        }
        List<String> lines = wrapTooltipText(this.backend, hovered.tooltip);
        int textWidth = 0;
        for (int i = 0; i < lines.size(); i++) {
            textWidth = Math.max(textWidth, this.backend.textWidth(lines.get(i)));
        }
        int x = mouseX + 12;
        if (x + textWidth + 8 > this.width) {
            x = Math.max(0, this.width - textWidth - 8);
        }
        int y = Math.max(0, mouseY - 12);
        this.backend.fillRect(x - 4, y - 4, x + textWidth + 4, y + lines.size() * 10 + 3,
                TOOLTIP_BG);
        this.backend.outlineRect1px(x - 4, y - 4, textWidth + 8, lines.size() * 10 + 7,
                TOOLTIP_BORDER);
        for (int i = 0; i < lines.size(); i++) {
            this.backend.drawText(lines.get(i), x, y + i * 10, TOOLTIP_TEXT, true,
                    YuiBackend.Align.LEFT);
        }
    }

    /** tooltip 文本按词换行（无空格退化为逐字断行），限宽 200（vanilla 1.12 默认域）。 */
    private static List<String> wrapTooltipText(YuiBackend backend, String text) {
        List<String> out = new ArrayList<String>();
        for (String rawLine : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : rawLine.split(" ", -1)) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (backend.textWidth(candidate) <= TOOLTIP_MAX_WIDTH) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (current.length() > 0) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                // 单词超宽：逐字断行（位图字体无空格文本如 URL 的兜底）
                while (backend.textWidth(word) > TOOLTIP_MAX_WIDTH && word.length() > 1) {
                    int cut = word.length() - 1;
                    while (cut > 1 && backend.textWidth(word.substring(0, cut)) > TOOLTIP_MAX_WIDTH) {
                        cut--;
                    }
                    out.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                current.append(word);
            }
            out.add(current.toString());
        }
        return out;
    }

    private static final int TOOLTIP_BG = 0xF0100010;
    private static final int TOOLTIP_BORDER = 0x505000FF;
    private static final int TOOLTIP_TEXT = 0xFFFFFF;
    private static final int TOOLTIP_MAX_WIDTH = 200;

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        for (int i = this.widgets.size() - 1; i >= 0; i--) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible && w.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /** 释放广播（不阻断——按下与释放在不同组件上也属正常交互）。 */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        for (int i = this.widgets.size() - 1; i >= 0; i--) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible) {
                w.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        for (int i = this.widgets.size() - 1; i >= 0; i--) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible && w.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, char typedChar) {
        for (int i = this.widgets.size() - 1; i >= 0; i--) {
            YuiWidget w = this.widgets.get(i);
            if (w.visible && w.keyPressed(keyCode, typedChar)) {
                return true;
            }
        }
        return false;
    }
}
