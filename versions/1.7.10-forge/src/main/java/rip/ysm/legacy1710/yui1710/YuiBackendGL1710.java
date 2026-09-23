package rip.ysm.legacy1710.yui1710;

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
 * YUI 1.7.10 固定管线后端（MCP stable_12 面）。ADR-YSM-UI-UNIFIED ADR-1 实现 3/3。
 * 逐方法对照 YuiBackendGL1122 适形，代际差异（vanilla-mc-1710 反编译+stable_12
 * methods.csv 亲证 2026-09-23）：
 * - fillRect → Gui.drawRect public static（Gui.java:35，与 1122 同形）
 * - fillGradientV → drawGradientRect protected 实例 6 参（Gui.java:67），包内桥子类直调
 * - blit → TextureManager.bindTexture(ResourceLocation)（TextureManager.java:30）+
 *   Gui.drawModalRectWithCustomSizedTexture public static（Gui.java:132）
 * - drawText → FontRenderer.drawString(String,int,int,int,boolean)（FontRenderer.java:221，
 *   1710 为 int 坐标，非 1122 RFB 面的 float 重载）
 * - textWidth → getStringWidth（FontRenderer.java:404）
 * - scissor → GL11 直调 + Y 翻转（glY = displayHeight - y2*scale）
 * - guiScale/windowHeight → ScaledResolution(Minecraft,int,int) 三参构造
 *   （ScaledResolution.java:13）/ Minecraft.displayHeight（Minecraft.java:189）
 * - 字体字段 mc.fontRendererObj（Minecraft.java:200，1122 RFB 面为 fontRenderer）
 */
public final class YuiBackendGL1710 implements YuiBackend {

    /** drawGradientRect 是 Gui protected 实例方法——包内桥子类暴露（vanilla GuiScreen 同形继承）。 */
    private static final class GradientBridge extends Gui {
        void draw(int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
            drawGradientRect(x1, y1, x2, y2, colorFrom, colorTo);
        }
    }

    private final Minecraft mc;
    private final GradientBridge gradientBridge = new GradientBridge();
    private final Deque<int[]> scissorStack = new ArrayDeque<int[]>();

    public YuiBackendGL1710(Minecraft mc) {
        this.mc = mc;
    }

    private TextureManager textures() {
        return this.mc.getTextureManager();
    }

    private FontRenderer font() {
        return this.mc.fontRendererObj;
    }

    private ScaledResolution scaled() {
        return new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
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
        // GuiGraphics.renderOutline 几何：上下整行 + 左右中间列（与 1122 后端同一份展开）
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
        int s = scaled().getScaleFactor();
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
        font().drawString(text, ax, y, color, shadow);
    }

    // drawTextCentered：接口 default（同 vanilla drawCenteredString=drawShadow 语义）

    @Override
    public int textWidth(String text) {
        return font().getStringWidth(text);
    }

    @Override
    public double guiScale() {
        return scaled().getScaleFactor();
    }

    @Override
    public int windowHeight() {
        return this.mc.displayHeight;
    }

    // preview：接口默认空实现（本卡范围）。1.7.10 预览配方随 1710 L2 真模型装载另卡
    //（vanilla 参照 GuiInventory.drawEntityOnScreen GuiInventory.java:63-97：GL11 固定管线
    // + RenderHelper 标准光照 + RenderManager.instance.renderEntityWithPosYaw，无 stencil；
    // legacy 渲染走 RendererLivingEntity mixin 接管，GUI 内须直调版本树翻译层）。
}
