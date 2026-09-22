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
    }

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
