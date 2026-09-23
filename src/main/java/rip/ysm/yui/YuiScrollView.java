package rip.ysm.yui;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动容器：视口 scissor + 内容偏移 + 滚轮 + 半透明奶油拇指（无轨道）。
 * 形态真相源：LegacyModelSelectScreen:156-187（1.12.2 GL11 scissor+手写 wheel 在产实证）
 * + YsmGui.enableScissor 现代语义（YsmGui.java:745）。
 *
 * <p>内容坐标约定：子组件按 scrollY=0 时的绝对 GUI 坐标摆放（消费侧 layout 定位）；
 * 滚动只影响渲染与命中测试的 y 偏移，不改子组件坐标。内容总高由消费侧声明
 * {@link #contentHeight}。
 */
public class YuiScrollView extends YuiWidget {

    /** 滚轮一格的滚动量（px）。 */
    public int wheelStep = 14;

    /** 内容总高（px，scrollY=0 形态下最末子组件的底缘）。 */
    public int contentHeight;

    /** 滚动偏移（0..maxScroll）。 */
    public int scrollY;

    private final List<YuiWidget> content = new ArrayList<YuiWidget>();

    public YuiScrollView(int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height);
        this.contentHeight = contentHeight;
    }

    public void add(YuiWidget widget) {
        this.content.add(widget);
    }

    public int maxScroll() {
        return Math.max(0, this.contentHeight - this.height);
    }

    private boolean inContent(int mouseX, int mouseY) {
        // 命中区含拇指列（右侧 2px），与视觉视口一致
        return isHovered(mouseX, mouseY);
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScroll()));
        backend.scissorPush(this.x, this.y, this.x + this.width, this.y + this.height);
        for (int i = 0; i < this.content.size(); i++) {
            YuiWidget w = this.content.get(i);
            if (!w.visible) {
                continue;
            }
            int shifted = w.y - this.scrollY;
            if (shifted + w.height < this.y || shifted > this.y + this.height) {
                continue; // 视口外剔除
            }
            w.y = shifted;
            // 子件已在视口系（y 临时移位），鼠标同样传视口系原值——滚动态 hover 才配对
            //（曾错传内容系 +scrollY，错位 2×scrollY；debt-yui-scrollview-hover-mismatch）
            w.render(backend, mouseX, mouseY, partialTick);
            w.y = shifted + this.scrollY;
        }
        backend.scissorPop();
        if (maxScroll() > 0) {
            // 拇指在裁剪外画（右缘贴边），LegacyModelSelectScreen:182-187 同语言
            int thumbH = Math.max(8, this.height * this.height / Math.max(this.contentHeight, 1));
            int thumbY = this.y + this.scrollY * (this.height - thumbH) / maxScroll();
            backend.fillRect(this.x + this.width - 2, thumbY, this.x + this.width,
                    thumbY + thumbH, YuiColors.SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!inContent(mouseX, mouseY)) {
            return false;
        }
        for (int i = this.content.size() - 1; i >= 0; i--) {
            YuiWidget w = this.content.get(i);
            if (w.visible && w.mouseClicked(mouseX, mouseY + this.scrollY, button)) {
                return true;
            }
        }
        return true; // 空白区也吞掉点击（面板语言：点击不透传）
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        for (int i = this.content.size() - 1; i >= 0; i--) {
            this.content.get(i).mouseReleased(mouseX, mouseY + this.scrollY, button);
        }
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        if (!inContent(mouseX, mouseY) || maxScroll() == 0) {
            return false;
        }
        this.scrollY = Math.max(0, Math.min(this.scrollY - (int) Math.signum(delta) * this.wheelStep,
                maxScroll()));
        return true;
    }
}
