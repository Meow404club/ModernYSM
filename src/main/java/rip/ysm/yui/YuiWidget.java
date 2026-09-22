package rip.ysm.yui;

/**
 * YUI 组件基类。命中测试/hover/事件分发语义在此层（版本中性），
 * 渲染只经 {@link YuiBackend} 原语。
 *
 * <p>M-U2 扩展点：模型卡片（YuiModelCard）/卡网格（YuiCardGrid）按同形继承本类挂入
 * YuiScreen，无需改接缝。
 */
public abstract class YuiWidget {

    public int x;
    public int y;
    public int width;
    public int height;
    public boolean visible = true;

    protected YuiWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** 鼠标是否悬停于本组件矩形内（GUI 逻辑坐标）。 */
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public abstract void render(YuiBackend backend, int mouseX, int mouseY, float partialTick);

    /** 返回 true=消费（阻止向兄弟/下层传递）。 */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return false;
    }

    /** 释放广播：所有收到 pressed 的组件都会收到（无 focus 追踪，M-U1 语义）。 */
    public void mouseReleased(int mouseX, int mouseY, int button) {
    }

    /** delta>0=滚轮向上（内容上移）。返回 true=消费。 */
    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        return false;
    }

    /** 返回 true=消费。Escape 关屏由 host 层处理，不到这里。 */
    public boolean keyPressed(int keyCode, char typedChar) {
        return false;
    }
}
