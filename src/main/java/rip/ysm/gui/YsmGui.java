package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * GUI 绘制门面：对位 GuiGraphics（1.19.4+）与 PoseStack 直绘（1.16.5）双轴。
 * 共享源恒为 1.20.1 展开态（活跃分支裸写），1.16.5 分支预包 <code>/* *&#47;</code>。
 * <p>映射对照（GuiGraphics 1.20.1 → 1.16.5 等价，实据 vanilla-mc 双版本源）：
 * <ul>
 *   <li>fill(x1,y1,x2,y2,color) → GuiComponent.fill(PoseStack,...)（GuiComponent.java:43，静态）</li>
 *   <li>drawString(font,c,x,y,color,false) → Font.draw(PoseStack,...)（Font.java:60/75/80）；
 *       shadow=true → GuiComponent.drawString / Font.drawShadow（GuiComponent.java:120-127 恒 shadow）</li>
 *   <li>drawCenteredString(font,c,x,y,color) → GuiComponent.drawCenteredString（GuiComponent.java:111-118）；
 *       FormattedCharSequence 重载 1.16.5 无 → 按 1.20.1 GuiGraphics 实现等价展开（x-width/2 + drawShadow）</li>
 *   <li>blit(rl,x,y,w,h,u,v,uW,vH,texW,texH) → TextureManager.bind(rl)（TextureManager.java:41）
 *       + GuiComponent.blit(PoseStack,...)（GuiComponent.java:155，静态，参数序一致）</li>
 *   <li>renderOutline(x,y,w,h,color) → 4 次 fill（按 1.20.1 GuiGraphics.renderOutline 几何）</li>
 *   <li>setColor(r,g,b,a) → RenderSystem.color4f（RenderSystem.java:528）</li>
 *   <li>enableScissor/disableScissor → GlStateManager._enableScissorTest/_scissorBox/
 *       _disableScissorTest（GlStateManager.java:152-164），Y 翻转照抄 1.20.1 applyScissor 数学</li>
 * </ul>
 */
public final class YsmGui {
    /**
     * 版本中性文本工厂：1.20.1 Component.literal（1.19.4+，mojmap 无 TextComponent 类）↔
     * 1.16.5 new TextComponent（Component.java 无 literal/translatable 工厂）。
     */
    public static net.minecraft.network.chat.MutableComponent text(String s) {
        //? if <1.17 {
        /*return new net.minecraft.network.chat.TextComponent(s);
         *///?} else {
        return Component.literal(s);
        //?}
    }

    public static net.minecraft.network.chat.MutableComponent trans(String key, Object... args) {
        //? if <1.17 {
        /*return new net.minecraft.network.chat.TranslatableComponent(key, args);
         *///?} else {
        return Component.translatable(key, args);
        //?}
    }

    /** AbstractWidget.render(GuiGraphics,PoseStack 双签名) 的单点分发。 */
    public void renderWidget(YsmWidget widget, int mouseX, int mouseY, float partialTick) {
        //? if <1.17 {
        /*widget.render(this.pose, mouseX, mouseY, partialTick);
         *///?} else {
        widget.render(this.graphics, mouseX, mouseY, partialTick);
        //?}
    }

    //? if >1.17 {
    private final net.minecraft.client.gui.GuiGraphics graphics;

    public YsmGui(net.minecraft.client.gui.GuiGraphics graphics) {
        this.graphics = graphics;
    }

    /** 1.20.1 侧取出被包装的 GuiGraphics（super.render/renderBackground 需要）。 */
    public net.minecraft.client.gui.GuiGraphics graphics() {
        return this.graphics;
    }

    public PoseStack pose() {
        return this.graphics.pose();
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        this.graphics.drawString(font, text, x, y, color, shadow);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.graphics.drawCenteredString(font, text, x, y, color);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        this.graphics.renderOutline(x, y, width, height, color);
    }

    public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        this.graphics.blit(atlas, x, y, u, v, width, height);
    }

    public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    public void setColor(float r, float g, float b, float a) {
        this.graphics.setColor(r, g, b, a);
    }

    /** 垂直渐变填充（GuiGraphics.fillGradient 对位）。 */
    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    /** GuiGraphics.blit(rl,x,y,u,v,w,h,texW,texH)（float u/v + 显式纹理尺寸）对位。 */
    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    /** TextureManager.getTexture(rl, missing) 双参语义：未注册时返回缺失纹理占位（不注册占位条目）。 */
    public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().getTexture(location, net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture());
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        this.graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics.disableScissor();
    }

    /** Screen.renderBackground(GuiGraphics) 的版本中性入口。 */
    public void renderScreenBackground(net.minecraft.client.gui.screens.Screen screen) {
        screen.renderBackground(this.graphics);
    }

    /** GuiGraphics.renderComponentTooltip 的版本中性入口（Screen 渲染 tooltip 用）。 */
    public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    /** 已处于窗口像素坐标的裸 scissor（1.20.1 = RenderSystem.enableScissor）。 */
    public static void enableScissorBox(int x, int y, int width, int height) {
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
    //?} else {
    /*private final PoseStack pose;

    public YsmGui(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        net.minecraft.client.gui.GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        // 1.16.5 GuiComponent.drawString 仅 String/Component 重载（GuiComponent.java:120-127）
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawCenteredString(this.pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawCenteredString(this.pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        // 1.16.5 无 FormattedCharSequence 重载，按 1.20.1 GuiGraphics 实现等价展开
        font.drawShadow(this.pose, text, (float) (x - font.width(text) / 2), (float) y, color);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        // 按 1.20.1 GuiGraphics.renderOutline 几何：上下整行 + 左右中间列
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);
        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        // GuiComponent.blit(PoseStack,x,y,destW,destH,u,v,uW,vH,texW,texH)（GuiComponent.java:155）
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, width, height, (float) u, (float) v, width, height, 256, 256);
    }

    public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    public void setColor(float r, float g, float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.color4f(r, g, b, a);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        // 1.16.5 GuiComponent.fillGradient(PoseStack,...) 是 protected 实例方法（GuiComponent.java:43 区段），
        // 经包内子类桥直调 vanilla 原路径，不自绘 BufferBuilder 降风险
        GradientFiller.INSTANCE.fillGradient(this.pose, minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        // GuiComponent.blit(PoseStack,x,y,destW,destH,u,v,uW,vH,texW,texH)（GuiComponent.java:155）
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        // 1.16.5 TextureManager.getTexture 只有单参重载：未注册时 computeIfAbsent 落缺失纹理占位
        //（与 1.20.1 双参版差异=占位条目可能入 byPath，后续 register(location, texture) 会覆盖，语义等价）
        return Minecraft.getInstance().getTextureManager().getTexture(location);
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        // 照抄 1.20.1 GuiGraphics.applyScissor 数学（GUI 坐标 → GL 窗口坐标 Y 翻转）
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        int windowHeight = window.getHeight();
        double scale = window.getGuiScale();
        int x = (int) ((double) minX * scale);
        int y = (int) ((double) windowHeight - (double) maxY * scale);
        int w = Math.max(0, (int) ((double) (maxX - minX) * scale));
        int h = Math.max(0, (int) ((double) (maxY - minY) * scale));
        com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
        com.mojang.blaze3d.platform.GlStateManager._scissorBox(x, y, w, h);
    }

    public void disableScissor() {
        com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
    }

    public void renderScreenBackground(net.minecraft.client.gui.screens.Screen screen) {
        screen.renderBackground(this.pose);
    }

    public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
    }

    public static void enableScissorBox(int x, int y, int width, int height) {
        com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
        com.mojang.blaze3d.platform.GlStateManager._scissorBox(x, y, width, height);
    }

    public static void disableScissorBox() {
        com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
    }

    // 1.16.5 GuiComponent.fillGradient(PoseStack,...) 为 protected 实例方法 → 包内子类桥
    private static final class GradientFiller extends net.minecraft.client.gui.GuiComponent {
        private static final GradientFiller INSTANCE = new GradientFiller();
    }
     *///?}
}
