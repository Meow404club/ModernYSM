package rip.ysm.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import rip.ysm.gui.YsmGui;
import net.minecraft.network.chat.Component;
//? if >=1.16.2
import net.minecraft.util.FormattedCharSequence;
import rip.ysm.gui.OptionRow;

import java.util.List;

// FormattedCharSequence 1.16.2 才有（1.16.1 vanilla 无此类）：<1.16.2 段以 FormattedText
// 等价形态承接（1.16.1 Font.split(FormattedText,int)→List<FormattedText>，官方 1161 实证）。
public final class TipsRow extends OptionRow<Object> {
    private final String text;
    //? if <1.16.2 {
    /*private List<net.minecraft.network.chat.FormattedText> cachedLines;*/
    //?}
    //? if >=1.16.2 {
    private List<FormattedCharSequence> cachedLines;
    //?}
    private int cachedWidth = -1;

    public TipsRow(String text) {
        super(0, 0, 0, 0, null);
        this.text = text;
    }

    private void recomputeLines() {
        if (cachedWidth == width && cachedLines != null) return;
        Font font = Minecraft.getInstance().font;
        cachedLines = font.split(YsmGui.text(text), Math.max(20, width - 16));
        cachedWidth = width;
        this.height = Math.max(18, cachedLines.size() * 10 + 8);
    }

    @Override
    public void setWidth(int w) {
        super.setWidth(w);
        cachedLines = null;
        cachedWidth = -1;
        recomputeLines();
    }

    @Override
    protected void renderWidget(YsmGui g, int mouseX, int mouseY, float partialTick) {
        recomputeLines();
        g.fill(getX(), getY(), getX() + width, getY() + height, 0x90000000);
        Font font = Minecraft.getInstance().font;
        int y = getY() + 4;
        //? if <1.16.2 {
        /*for (net.minecraft.network.chat.FormattedText line : cachedLines) {
            g.drawString(font, line, getX() + 8, y, 0xFFEEEEEE, false);
            y += 10;
        }*/
        //?}
        //? if >=1.16.2 {
        for (FormattedCharSequence line : cachedLines) {
            g.drawString(font, line, getX() + 8, y, 0xFFEEEEEE, false);
            y += 10;
        }
        //?}
    }

    @Override
    protected void renderControl(YsmGui g, int mouseX, int mouseY, float partialTick) {
    }
}
