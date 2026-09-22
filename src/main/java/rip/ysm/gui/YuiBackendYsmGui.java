package rip.ysm.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import rip.ysm.yui.YuiBackend;

/**
 * YUI 现代线后端（>=1.16.5 全部现代线）：薄适配器，委托既有 {@link YsmGui}。
 * docs/ADR-YSM-UI-UNIFIED ADR-1 裁定——PoseStack 代（1.16.5~1.19.4）/GuiGraphics 代
 * （1.20~21.5）/Matrix3x2fStack 代（21.6+）/26.x extractor 代的差异全由 YsmGui 内部
 * 吸收，不拆多后端；本文件是 rip/ysm/gui 包内唯一允许条件轴的新文件（现实现零条件：
 * 只调 YsmGui 全线签名稳定的重载面，int 实参在 <1.20 线自动加宽命中 float u/v blit）。
 *
 * <p>不在 1.12.2/1.7.10 共享源白名单（legacy122Include 无 rip/ysm/gui/**）=legacy 自动排除。
 *
 * <p>26.x PiP 相位契约（YsmGui 构造器 ysmSetExtractor262 是 extract 相位唯一入口）：
 * 消费方必须在屏渲染同相位以 {@code new YsmGui(...)} 构造后即时包装/调用——
 * 与 PlayerModelScreen.render(YsmGui,...) 现行形态同构。preview 原语 M-U1 为
 * 接口默认空实现，M-U2 实现时直调 ModelPreviewRenderer.renderLivingEntityPreview
 * （ModelButton:287 同形），不得加间接层破坏相位契约。
 */
public final class YuiBackendYsmGui implements YuiBackend {

    private final YsmGui gui;

    public YuiBackendYsmGui(YsmGui gui) {
        this.gui = gui;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void fillRect(int x1, int y1, int x2, int y2, int color) {
        this.gui.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void fillGradientV(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        this.gui.fillGradient(x1, y1, x2, y2, colorFrom, colorTo);
    }

    @Override
    public void outlineRect1px(int x, int y, int width, int height, int color) {
        this.gui.renderOutline(x, y, width, height, color);
    }

    @Override
    public void blit(Texture texture, int x, int y, int u, int v, int width, int height) {
        // u/v int 实参：<1.20 线命中 YsmGui float u/v 重载自动加宽，>=1.20 命中 int 重载
        this.gui.blit(textureLocation(texture), x, y, u, v, width, height,
                texture.width, texture.height);
    }

    /**
     * 中性路径字符串 → 本线 RL。不走 rip.ysm.util.Rl（forge 中段六线 sourceSet 级排除，
     * build.forge.gradle.kts:243，Rl 仅 neoforge-moddev 线编译）。构造形分界：
     * 1.21 起单 String/双参构造收紧为私有（vanilla-1.21.1 ResourceLocation.java:38 实证），
     * 1.16.5~1.20.6 单 String public；21.11+ Identifier 为生成树后处理全树改名
     * （build.moddev.gradle.kts:464，同包同 API，此处按 ResourceLocation 原名书写即可）。
     */
    private static ResourceLocation textureLocation(Texture texture) {
        String path = texture.path;
        int sep = path.indexOf(':');
        String namespace = sep < 0 ? "minecraft" : path.substring(0, sep);
        String subPath = sep < 0 ? path : path.substring(sep + 1);
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, subPath);
         *///?} else {
        return new ResourceLocation(path);
        //?}
    }

    @Override
    public void scissorPush(int x1, int y1, int x2, int y2) {
        this.gui.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void scissorPop() {
        this.gui.disableScissor();
    }

    @Override
    public void drawText(String text, int x, int y, int color, boolean shadow, Align align) {
        int ax = x;
        if (align == Align.CENTER) {
            ax = x - this.textWidth(text) / 2;
        } else if (align == Align.RIGHT) {
            ax = x - this.textWidth(text);
        }
        this.gui.drawString(font(), text, ax, y, color, shadow);
    }

    // drawTextCentered：接口 default（drawText CENTER+阴影），与 vanilla
    // drawCenteredString（x-width/2 + drawShadow，YsmGui.java:864）语义一致。

    @Override
    public int textWidth(String text) {
        return font().width(text);
    }

    @Override
    public double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public int windowHeight() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    // preview：接口默认空实现（M-U1），真预览 M-U2 按 YsmGui 26.x 相位契约直调
    // ModelPreviewRenderer.renderLivingEntityPreview（本类不拦截不包装）。
}
