package rip.ysm.legacy122.yui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import rip.ysm.yui.YuiBackend;
import rip.ysm.yui.YuiPreview;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * YUI 1.12.2 固定管线后端（MCP 面）。ADR-YSM-UI-UNIFIED ADR-1 实现 2/3。
 * 原语映射（vanilla-mc-1122 反编译实证）：
 * - fillRect → Gui.drawRect public static（Gui.java:36）
 * - fillGradientV → drawGradientRect protected 实例（Gui.java:71），包内桥子类直调
 * - blit → TextureManager.bindTexture（TextureManager.java:32）+
 *   Gui.drawModalRectWithCustomSizedTexture public static（Gui.java:162）
 * - drawText → FontRenderer.drawString(String,float,float,int,boolean)（FontRenderer.java:239）
 * - textWidth → getStringWidth（FontRenderer.java:427）
 * - scissor → GL11 直调 + Y 翻转（glY = displayHeight - y2*scale，在产先例
 *   LegacyModelSelectScreen:159-161）
 * - guiScale/windowHeight → ScaledResolution 单参构造（ScaledResolution.java:13）/
 *   Minecraft.displayHeight（Minecraft.java:235）
 */
public final class YuiBackendGL1122 implements YuiBackend {

    /** drawGradientRect 是 Gui protected 实例方法——包内桥子类暴露（vanilla GuiScreen 同形继承）。 */
    private static final class GradientBridge extends Gui {
        void draw(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
            drawGradientRect(x1, y1, x2, y2, colorFrom, colorTo);
        }
    }

    private final Minecraft mc;
    private final GradientBridge gradientBridge = new GradientBridge();
    private final Deque<int[]> scissorStack = new ArrayDeque<int[]>();

    public YuiBackendGL1122(Minecraft mc) {
        this.mc = mc;
    }

    private TextureManager textures() {
        return this.mc.getTextureManager();
    }

    private FontRenderer font() {
        return this.mc.fontRenderer;
    }

    @Override
    public void fillRect(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    @Override
    public void fillGradientV(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        this.gradientBridge.draw(x1, y1, x2, y2, colorFrom, colorTo);
    }

    @Override
    public void outlineRect1px(int x, int y, int width, int height, int color) {
        // GuiGraphics.renderOutline 几何：上下整行 + 左右中间列（1.20.1 GuiGraphics 同语义）
        Gui.drawRect(x, y, x + width, y + 1, color);
        Gui.drawRect(x, y + height - 1, x + width, y + height, color);
        Gui.drawRect(x, y + 1, x + 1, y + height - 1, color);
        Gui.drawRect(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Override
    public void blit(Texture texture, int x, int y, int u, int v, int width, int height) {
        textures().bindTexture(new ResourceLocation(texture.path));
        Gui.drawModalRectWithCustomSizedTexture(x, y, (float) u, (float) v, width, height,
                (float) texture.width, (float) texture.height);
    }

    @Override
    public void scissorPush(int x1, int y1, int x2, int y2) {
        this.scissorStack.push(new int[] {x1, y1, x2, y2});
        applyScissor(x1, y1, x2, y2);
    }

    @Override
    public void scissorPop() {
        this.scissorStack.poll();
        int[] outer = this.scissorStack.peek();
        if (outer == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            applyScissor(outer[0], outer[1], outer[2], outer[3]);
        }
    }

    private void applyScissor(int x1, int y1, int x2, int y2) {
        int s = new ScaledResolution(this.mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x1 * s, this.mc.displayHeight - y2 * s,
                (x2 - x1) * s, (y2 - y1) * s);
    }

    @Override
    public void drawText(String text, int x, int y, int color, boolean shadow, Align align) {
        int ax = x;
        if (align == Align.CENTER) {
            ax = x - textWidth(text) / 2;
        } else if (align == Align.RIGHT) {
            ax = x - textWidth(text);
        }
        font().drawString(text, (float) ax, (float) y, color, shadow);
    }

    // drawTextCentered：接口 default（同 vanilla drawCenteredString=drawShadow 语义）

    @Override
    public int textWidth(String text) {
        return font().getStringWidth(text);
    }

    @Override
    public double guiScale() {
        return new ScaledResolution(this.mc).getScaleFactor();
    }

    @Override
    public int windowHeight() {
        return this.mc.displayHeight;
    }

    // preview：M-U2 落地（收编 POC be0a794 结论）。签名契约（YuiPreview javadoc）：
    // 返回后 scissor 已弹 + 深度测试已关（模型 z 写深度挡后续 2D quad，vanilla 先例
    // GuiContainer.drawScreen:73；scissor 掩深度写入=槽外零泄漏）。RenderManager
    // 路径不可用（legacy 渲染走 RenderPlayerEvent 接管），实体绘制直调版本树翻译层
    // （消费侧 YuiPreview 实现内完成）。
    @Override
    public void preview(YuiPreview preview, int x1, int y1, int x2, int y2,
                        float mouseX, float mouseY, float partialTick) {
        GL11.glEnable(GL11.GL_DEPTH_TEST); // 模型自遮挡需要（GUI 进场态不保证）
        scissorPush(x1, y1, x2, y2);
        preview.render(x1, y1, x2, y2, mouseX, mouseY, partialTick);
        scissorPop();
        GL11.glDisable(GL11.GL_DEPTH_TEST); // 契约收口：后续 2D 叠序可见（host 帧末恢复）
    }
}
