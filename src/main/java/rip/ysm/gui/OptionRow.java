package rip.ysm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 选项行基类：继承 YsmWidget 消化双版 AbstractWidget 差异
 * （渲染钩子 renderWidget(GuiGraphics)↔renderButton(PoseStack)、getX/getY/setX/setY 桥接、narration 剔除）。
 */
public abstract class OptionRow<T> extends YsmWidget {
    protected final Option<T> option;

    protected OptionRow(int x, int y, int width, int height, Option<T> option) {
        super(x, y, width, height, option == null ? YsmGui.text("") : option.getLabel());
        this.option = option;
    }

    public Option<T> getOption() {
        return option;
    }

    public void refresh() {
    }

    @Override
    protected void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick) {
        boolean dirty = option != null && option.isDirty();
        int bg = /*? if >=1.18.2 && <1.19.4 {*/ /*isHoveredOrFocused()*//*?} else {*/ isHovered() /*?}*/ ? 0x90171717 : (dirty ? 0x90060606 : 0x90000000);
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);

        Component label = getMessage();
        int textColor = dirty ? -1 : 0x90FFFFFF;
        int textY = getY() + (height - 8) / 2;
        g.drawString(Minecraft.getInstance().font, label, getX() + 8, textY, textColor, false);

        renderControl(g, mouseX, mouseY, partialTick);
    }

    protected abstract void renderControl(YsmGui g, int mouseX, int mouseY, float partialTick);

    protected int controlX() {
        return getX() + width - controlWidth() - 6;
    }

    protected int controlY() {
        return getY() + (height - controlHeight()) / 2;
    }

    protected int controlWidth() {
        return 90;
    }

    protected int controlHeight() {
        return Math.min(height - 4, 16);
    }

    protected boolean isMouseOverControl(double mx, double my) {
        int cx = controlX();
        int cy = controlY();
        return mx >= cx && mx < cx + controlWidth() && my >= cy && my < cy + controlHeight();
    }

    public boolean isOverlayOpen() {
        return false;
    }

    public void closeOverlay() {
    }

    public void renderOverlay(YsmGui g, int mouseX, int mouseY, float partialTick, float scrollDisplay) {
    }

    public boolean overlayMouseClicked(double mouseX, double mouseY, int button, float scrollDisplay) {
        return false;
    }

    public boolean overlayMouseScrolled(double mouseX, double mouseY, double delta, float scrollDisplay) {
        return false;
    }

    protected static int blendBg(boolean hover, int base) {
        if (!hover) return base;
        int a = (base >>> 24) & 0xFF;
        int r = Mth.clamp(((base >> 16) & 0xFF) + 40, 0, 255);
        int gn = Mth.clamp(((base >> 8) & 0xFF) + 40, 0, 255);
        int b = Mth.clamp((base & 0xFF) + 40, 0, 255);
        return (a << 24) | (r << 16) | (gn << 8) | b;
    }
}
