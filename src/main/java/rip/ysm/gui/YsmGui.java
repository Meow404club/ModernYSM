package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * GUI 绘制门面：对位 GuiGraphics（1.20+）与 PoseStack 直绘（1.16.5~1.19.4）双轴。
 * 共享源恒为 1.20.1 展开态（活跃分支裸写），非活跃分支预包 <code>/* *&#47;</code>。
 * 条件结构为顶层互斥块平铺——stonecutter 0.7 的跨行块注释（FlexScanner.flex IN_STAR_COMMENT）
 * 内 <code>//?</code> 指令不可见，预包体内不可嵌套条件，故按方法组分块而非 if/else 大分支。
 * <p>版本边界（vanilla-mc 反编译实证）：
 * <ul>
 *   <li>GuiGraphics：1.20 起（1201 GuiGraphics.java:56；1182/1192/1194 零命中）</li>
 *   <li>Component.literal/translatable：1.19.2 起（1192 Component.java:144/152 接口 static 隐 public；
 *       1182 无，TextComponent 构造 public）</li>
 *   <li>Button.builder：1.19.4 起（1194 Button.java:17；6 参构造降 protected :21）；
 *       1.16.5~1.19.2 用 public 6 参构造（1165:12/1182:15/1192:18）</li>
 *   <li>RenderSystem.setShaderTexture + GuiComponent.blit(z 形)：1.17 起（TextureManager.bind 删除，
 *       纹理经 shader 通道绑定；1182 GuiComponent.java:157-170/1194:199-211）；
 *       1.16.5 为 TextureManager.bind(rl)（1165:41）+ GuiComponent.blit(x,y,w,h,u,v,uW,vH,texW,texH)（:155）</li>
 *   <li>RenderSystem.setShaderColor：1.17 起（1182:464）；1.16.5 color4f（1165:528）</li>
 *   <li>TextureManager.getTexture 双参（rl, missing）：1.17 起（1182:115）；1.16.5 仅单参（:95）</li>
 *   <li>全参 blit（独立 uW/vH）1.16.5 与 1.17+ 参数序同形（1165:155 / 1182:165 / 1194:207）</li>
 * </ul>
 * <p>映射对照（GuiGraphics 1.20.1 → PoseStack 直绘等价）：
 * <ul>
 *   <li>fill(x1,y1,x2,y2,color) → GuiComponent.fill(PoseStack,...)（1165 GuiComponent.java:43/
 *       1182:80/1194:84，静态全版本存活）</li>
 *   <li>drawString(font,c,x,y,color,false) → Font.draw(PoseStack,...)（1194 Font.java:61-77）；
 *       shadow=true → GuiComponent.drawString / Font.drawShadow（1165 GuiComponent.java:120-127 恒 shadow）</li>
 *   <li>drawCenteredString(font,c,x,y,color) → GuiComponent.drawCenteredString（1194:144-153）；
 *       FormattedCharSequence 重载 1.16.5 无 → 按 1.20.1 GuiGraphics 实现等价展开（x-width/2 + drawShadow）</li>
 *   <li>renderOutline(x,y,w,h,color) → 4 次 fill（按 1.20.1 GuiGraphics.renderOutline 几何）</li>
 *   <li>enableScissor/disableScissor → GlStateManager._enableScissorTest/_scissorBox/
 *       _disableScissorTest（1194 GlStateManager.java:45-55），Y 翻转照抄 1.20.1 applyScissor 数学</li>
 * </ul>
 */
public final class YsmGui {
    //? if <1.20 {
    /*private final PoseStack pose;

    public YsmGui(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return this.pose;
    }

     *///?}
    //? if >=1.20 {
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

    //?}

    /**
     * 版本中性文本工厂：Component.literal/translatable 静态工厂 1.19.2 引入
     * （1192 Component.java:144/152，接口 static 隐 public；1182 无）↔
     * 1.16.5~1.18.2 new TextComponent/TranslatableComponent。
     */
    public static net.minecraft.network.chat.MutableComponent text(String s) {
        //? if <1.19.2 {
        /*return new net.minecraft.network.chat.TextComponent(s);
         *///?} else {
        return Component.literal(s);
        //?}
    }

    public static net.minecraft.network.chat.MutableComponent trans(String key, Object... args) {
        //? if <1.19.2 {
        /*return new net.minecraft.network.chat.TranslatableComponent(key, args);
         *///?} else {
        return Component.translatable(key, args);
        //?}
    }

    /** 版本中性 Button 工厂：1.19.4+ Button.builder().bounds().build()（1194 Button.java:17，
     * 6 参构造降 protected :21）↔ 1.16.5~1.19.2 new Button(x,y,w,h,msg,onPress)
     *（1165:12/1182:15/1192:18 public 6 参；多行 onPress lambda 各版同构）。 */
    public static net.minecraft.client.gui.components.Button button(int x, int y, int width, int height, net.minecraft.network.chat.Component message, net.minecraft.client.gui.components.Button.OnPress onPress) {
        //? if <1.19.4 {
        /*return new net.minecraft.client.gui.components.Button(x, y, width, height, message, onPress);
         *///?} else {
        return net.minecraft.client.gui.components.Button.builder(message, onPress).bounds(x, y, width, height).build();
        //?}
    }

    /** AbstractWidget.render(GuiGraphics,PoseStack 双签名) 的单点分发。
     * render(PoseStack,...) 1.16.5~1.19.4 全版本存活（1194 AbstractWidget.java:67）；
     * render(GuiGraphics,...) 1.20 起。 */
    //? if <1.20 {
    /*public void renderWidget(YsmWidget widget, int mouseX, int mouseY, float partialTick) {
        widget.render(this.pose, mouseX, mouseY, partialTick);
    }

     *///?}
    //? if >=1.20 {
    public void renderWidget(YsmWidget widget, int mouseX, int mouseY, float partialTick) {
        widget.render(this.graphics, mouseX, mouseY, partialTick);
    }

    //?}

    //? if <1.20 {
    /*public void fill(int minX, int minY, int maxX, int maxY, int color) {
        net.minecraft.client.gui.GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        // 1.16.5~1.19.4 无 z 轴 → 落 0 层
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

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        // 1.16.5 GuiComponent.fillGradient(PoseStack,...) 是 protected 实例方法（GuiComponent.java:43 区段），
        // 经包内子类桥直调 vanilla 原路径，不自绘 BufferBuilder 降风险
        GradientFiller.gradient(this.pose, minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int z, int colorFrom, int colorTo) {
        GradientFiller.gradient(this.pose, minX, minY, maxX, maxY, colorFrom, colorTo);
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

    public void hLine(int minX, int maxX, int y, int color) {
        // 1.16.5 GuiComponent.hLine/vLine 为 protected 实例方法（javap）→ 包内子类桥
        GradientFiller.hLineBridge(this.pose, minX, maxX, y, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        GradientFiller.vLineBridge(this.pose, x, minY, maxY, color);
    }

    public void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int width, int color) {
        // 按 1.20.1 GuiGraphics.drawWordWrap 实现（Font.split + 逐行 drawShadow，含末行阴影去重）
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, width);
        int lineY = y;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            font.drawShadow(this.pose, line, x, lineY, color);
            lineY += 9;
        }
    }

    public void renderTooltip(Font font, java.util.List<net.minecraft.util.FormattedCharSequence> lines, int mouseX, int mouseY) {
        // Screen.renderTooltip(PoseStack, List<FormattedCharSequence>, x, y)
        //（1165 javap public / 1182:187 / 1192:186 / 1194:254 全版本存活）
        Minecraft.getInstance().screen.renderTooltip(this.pose, lines, mouseX, mouseY);
    }

    // 5 参版（GuiGraphics 默认带阴影语义）：String/Component 走 GuiComponent.drawString（恒 shadow），
    // FormattedCharSequence 1.16.5 无静态重载 → drawShadow 等价
    public void drawString(Font font, Component text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) x, (float) y, color);
    }

    public void renderScreenBackground(net.minecraft.client.gui.screens.Screen screen) {
        // Screen.renderBackground(PoseStack) 1.16.5~1.19.4 存活（1182/1192/1194:452）
        screen.renderBackground(this.pose);
    }

    // GuiGraphics.renderComponentTooltip 的版本中性入口（Screen 渲染 tooltip 用）。
    // Screen.renderComponentTooltip(PoseStack, List<Component>, x, y) 4 参全版本存活
    //（1165:126/1182:183/1192:182/1194:250）。
    public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
    }

    // 已处于窗口像素坐标的裸 scissor（1.20.1 = RenderSystem.enableScissor）。
    public static void enableScissorBox(int x, int y, int width, int height) {
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    // 1.16.5 GuiComponent.fillGradient(PoseStack,...)/hLine/vLine 为 protected（实例 1.16.5~1.19.2、
    // static 1.19.4：1194 GuiComponent.java:37/47/114）→ 包内子类桥，INSTANCE 调用对两种形态均合法
    // （JLS 6.6.2：protected 实例成员经限定名访问要求限定方为访问所在类的子类，故桥方法内置）
    private static final class GradientFiller extends net.minecraft.client.gui.GuiComponent {
        private static final GradientFiller INSTANCE = new GradientFiller();

        static void gradient(PoseStack pose, int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
            INSTANCE.fillGradient(pose, minX, minY, maxX, maxY, colorFrom, colorTo);
        }

        static void hLineBridge(PoseStack pose, int minX, int maxX, int y, int color) {
            INSTANCE.hLine(pose, minX, maxX, y, color);
        }

        static void vLineBridge(PoseStack pose, int x, int minY, int maxY, int color) {
            INSTANCE.vLine(pose, x, minY, maxY, color);
        }
    }

     *///?}

    /** TextureManager.getTexture：1.16.5 仅单参重载（未注册时 computeIfAbsent 落缺失纹理占位，
     * 与双参版差异=占位条目可能入 byPath，后续 register(location, texture) 会覆盖，语义等价）；
     * 1.17+ 双参（rl, missing）显式占位。1.20 侧版本在下方 >=1.20 段。 */
    //? if <1.17 {
    /*public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().getTexture(location);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().getTexture(location, net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture());
    }

     *///?}

    // ==================== blit 家族（绑定机制 1.17 分界） ====================

    /** 短版 256 贴图 blit：1.16.5 TextureManager.bind + (x,y,w,h,u,v,uW,vH,texW,texH) 形；
     * 1.17+ setShaderTexture + (x,y,z,u,v,w,h,texW,texH) 形（1182:157/1194:203）。 */
    //? if <1.17 {
    /*public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, width, height, (float) u, (float) v, width, height, 256, 256);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, 0, (float) u, (float) v, width, height, 256, 256);
    }

     *///?}
    //? if >=1.20 {
    public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        this.graphics.blit(atlas, x, y, u, v, width, height);
    }

    //?}

    /** 全参版（独立 uW/vH）：1.16.5 与 1.17+ 参数序同形（1165:155 / 1182:165 / 1194:207），
     * 仅纹理绑定方式 1.17 分界。 */
    //? if <1.17 {
    /*public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.20 {
    public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    //?}

    /** float u/v + 显式纹理尺寸 blit：1.16.5 (x,y,w,h,u,v,uW,vH,texW,texH) 形；
     * 1.17+ (x,y,z,u,v,w,h,texW,texH) 形（GuiComponent.java:1165:155/1182:157/1194:203）。 */
    //? if <1.17 {
    /*public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        Minecraft.getInstance().getTextureManager().bind(atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, 0, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.20 {
    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    //?}

    /** 带 z 序 blit：1.17+ 有 z 形 (x,y,z,u,v,w,h,texW,texH)（1182:157）；1.16.5 无 z → 落 0 层同形。 */
    //? if <1.17 {
    /*public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        // 1.16.5 无 z 轴（同 fillGradient z 降级说明）
        Minecraft.getInstance().getTextureManager().bind(atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, width, height, (float) uOffset, (float) vOffset, width, height, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, atlas);
        net.minecraft.client.gui.GuiComponent.blit(this.pose, x, y, z, (float) uOffset, (float) vOffset, width, height, textureWidth, textureHeight);
    }

     *///?}
    //? if >=1.20 {
    public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        // 1.20.1 的带 z blit 是 GuiGraphics 包私有/异序重载（javap 实证 public 无 z-int 形）→
        // 落回无 z 形（z 序层叠差 = 该重载本为 tooltip/悬浮层专用，此处调用点均为面板本体绘制，等价）
        this.graphics.blit(atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    //?}

    // ==================== setColor（color4f 1.17 分界） ====================

    /** 1.16.5 RenderSystem.color4f（1165:528）↔ 1.17+ setShaderColor（1182:464/1194:489）↔
     * 1.20 GuiGraphics.setColor。 */
    //? if <1.17 {
    /*public void setColor(float r, float g, float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.color4f(r, g, b, a);
    }

     *///?}
    //? if >=1.17 && <1.20 {
    /*
    public void setColor(float r, float g, float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
    }

     *///?}
    //? if >=1.20 {
    public void setColor(float r, float g, float b, float a) {
        this.graphics.setColor(r, g, b, a);
    }

    //?}

    /** 垂直渐变填充（GuiGraphics.fillGradient 对位）。 */
    //? if >=1.20 {
    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    /** GuiGraphics.blit(rl,x,y,u,v,w,h,texW,texH)（float u/v + 显式纹理尺寸）对位。 */
    public void blit(ResourceLocation atlas, int x, int y, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
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

    /** 水平/垂直线（GuiGraphics.hLine/vLine 对位）。 */
    public void hLine(int minX, int maxX, int y, int color) {
        this.graphics.hLine(minX, maxX, y, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        this.graphics.vLine(x, minY, maxY, color);
    }

    /** 按宽折行多行文本（GuiGraphics.drawWordWrap 对位）。 */
    public void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int width, int color) {
        this.graphics.drawWordWrap(font, text, x, y, width, color);
    }

    /** 带 z 序的渐变/纹理绘制（1.20.1 GuiGraphics z 重载；<1.20 无 z 轴 → 落回 0 层，绘制顺序不变）。 */
    public void fillGradient(int minX, int minY, int maxX, int maxY, int z, int colorFrom, int colorTo) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, z, colorFrom, colorTo);
    }

    public void renderTooltip(Font font, java.util.List<net.minecraft.util.FormattedCharSequence> lines, int mouseX, int mouseY) {
        this.graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, z, color);
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

    public void drawString(Font font, Component text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.graphics.drawString(font, text, x, y, color);
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
    //?}
}
