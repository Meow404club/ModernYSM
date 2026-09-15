package rip.ysm.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
//? if >=1.16.2
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

//? if <21.6 {
    public PoseStack pose() {
        return this.graphics.pose();
    }
//?}
// 1.21.6+ GuiGraphics.pose() 返回 Matrix3x2fStack（GUI 状态化矩阵栈）
//? if >=21.6 {
    /*public org.joml.Matrix3x2fStack pose() {
        return this.graphics.pose();
    }*/
    //?}

    //?}

    /**
     * 版本中性文本工厂：Component.literal/translatable 静态工厂 1.19.0 引入（f119 Component.java:126/130 实证）
     * （1192 Component.java:144/152，接口 static 隐 public；1182 无）↔
     * 1.16.5~1.18.2 new TextComponent/TranslatableComponent。
     */
    public static net.minecraft.network.chat.MutableComponent text(String s) {
        //? if <1.19 {
        /*return new net.minecraft.network.chat.TextComponent(s);
         *///?} else {
        return Component.literal(s);
        //?}
    }

    public static net.minecraft.network.chat.MutableComponent trans(String key, Object... args) {
        //? if <1.19 {
        /*return new net.minecraft.network.chat.TranslatableComponent(key, args);
         *///?} else {
        return Component.translatable(key, args);
        //?}
    }

    /** 版本中性 Button 工厂：1.19.3 起 6 参构造删除仅余 Builder（1.19.3 merged jar 实证）↔
     * 1.16.5~1.19.2 new Button(x,y,w,h,msg,onPress)（多行 onPress lambda 各版同构）。 */
    public static net.minecraft.client.gui.components.Button button(int x, int y, int width, int height, net.minecraft.network.chat.Component message, net.minecraft.client.gui.components.Button.OnPress onPress) {
        //? if <1.19.3 {
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
    //? if >=1.20 && <26 {
    public void renderWidget(YsmWidget widget, int mouseX, int mouseY, float partialTick) {
        widget.render(this.graphics, mouseX, mouseY, partialTick);
    }

    //?}
    // 26.x：AbstractWidget.render 删 → extractRenderState（vanilla-26.1 AbstractWidget.java:59 public final）
    //? if >=26 {
    /*public void renderWidget(YsmWidget widget, int mouseX, int mouseY, float partialTick) {
        widget.extractRenderState(this.graphics, mouseX, mouseY, partialTick);
    }

     *///?}

    //? if <1.20 {
    /*public void fill(int minX, int minY, int maxX, int maxY, int color) {
        net.minecraft.client.gui.GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        // 1.16.5~1.19.4 无 z 轴 → 落 0 层
        net.minecraft.client.gui.GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
    }

     *///?}
    // FormattedCharSequence 1.16.2 才有（1.16.1 vanilla 无此类，官方 1161 client.txt 零命中）：
    // <1.16.2 段用 FormattedText 等价形态（1.16.1 Font.draw/drawShadow/split/width 的
    // FormattedText 重载与 Screen.renderTooltip(PoseStack,List,x,y) 均在，官方 1161 实证）；
    // >=1.16.2 段保持原形。1.16.5 生成树两侧同活、成员序不变=逐字节等价。
    // 1.16.1 GuiComponent.drawString/drawCenteredString 为非静态实例方法（1.16.5 起静态，
    // 官方 1161 mojmap jar javap 实证）→ <1.16.2 段直调 Font.draw/drawShadow（语义等价：
    // 1.16.5 静态 GuiComponent.drawString 即 drawShadow 委托）。
    //? if <1.16.2 && <1.20 {
    /*public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) (x - font.width(text) / 2), (float) y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) (x - font.width(text) / 2), (float) y, color);
    }

    public void drawString(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(this.pose, text, (float) x, (float) y, color);
        } else {
            font.draw(this.pose, text, (float) x, (float) y, color);
        }
    }

    public void drawCenteredString(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) (x - font.width(text) / 2), (float) y, color);
    }

     *///?}
    //? if >=1.16.2 && <1.20 {
    /*public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
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

     *///?}
    //? if <1.20 {
    /*public void renderOutline(int x, int y, int width, int height, int color) {
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

     *///?}
    // scissor：GlStateManager._enableScissorTest/_scissorBox/RenderSystem.enableScissor 是
    // vanilla 1.16.4 才有（javap 实证：1.16.2/1.16.3 的 RenderSystem/GlStateManager 均无
    // scissor 系方法，1.16.4 齐备；1.16.1 亦无）→ <1.16.4 全段 GL11 直调；坐标数学与
    // >=1.16.4 段一致（1.16.5 _scissorBox=直透 GL20.glScissor，
    // vanilla-mc-1165 GlStateManager.java:162-165 实证，无额外翻转）。
    //? if <1.16.4 && <1.20 {
    /*public void enableScissor(int minX, int minY, int maxX, int maxY) {
        com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        int windowHeight = window.getHeight();
        double scale = window.getGuiScale();
        int x = (int) ((double) minX * scale);
        int y = (int) ((double) windowHeight - (double) maxY * scale);
        int w = Math.max(0, (int) ((double) (maxX - minX) * scale));
        int h = Math.max(0, (int) ((double) (maxY - minY) * scale));
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x, y, w, h);
    }

    public void disableScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

     *///?}
    //? if >=1.16.4 && <1.20 {
    /*public void enableScissor(int minX, int minY, int maxX, int maxY) {
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

     *///?}
    //? if <1.20 {
    /*public void hLine(int minX, int maxX, int y, int color) {
        // 1.16.5 GuiComponent.hLine/vLine 为 protected 实例方法（javap）→ 包内子类桥
        GradientFiller.hLineBridge(this.pose, minX, maxX, y, color);
    }

    public void vLine(int x, int minY, int maxY, int color) {
        GradientFiller.vLineBridge(this.pose, x, minY, maxY, color);
    }

     *///?}
    //? if <1.16.2 && <1.20 {
    /*public void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int width, int color) {
        java.util.List<net.minecraft.network.chat.FormattedText> lines = font.split(text, width);
        int lineY = y;
        for (net.minecraft.network.chat.FormattedText line : lines) {
            font.drawShadow(this.pose, line, x, lineY, color);
            lineY += 9;
        }
    }

    public void renderTooltip(Font font, java.util.List<net.minecraft.network.chat.FormattedText> lines, int mouseX, int mouseY) {
        // 1.16.1 Screen.renderTooltip(PoseStack, List, x, y)（官方 1161 client.txt :132 实证，
        // 元素为 FormattedText 代）
        Minecraft.getInstance().screen.renderTooltip(this.pose, lines, mouseX, mouseY);
    }

     *///?}
    //? if >=1.16.2 && <1.20 {
    /*public void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int width, int color) {
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

     *///?}
    // 5 参版（GuiGraphics 默认带阴影语义）：<1.16.2 直调 Font.drawShadow（同上静态方法缺席）；
    // >=1.16.2 String/Component 走 GuiComponent.drawString（恒 shadow）。
    //? if <1.16.2 && <1.20 {
    /*public void drawString(Font font, Component text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) x, (float) y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) x, (float) y, color);
    }

    public void drawString(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) x, (float) y, color);
    }

     *///?}
    //? if >=1.16.2 && <1.20 {
    /*public void drawString(Font font, Component text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        net.minecraft.client.gui.GuiComponent.drawString(this.pose, font, text, x, y, color);
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        font.drawShadow(this.pose, text, (float) x, (float) y, color);
    }

     *///?}
    //? if <1.20 {
    /*public void renderScreenBackground(net.minecraft.client.gui.screens.Screen screen) {
        // Screen.renderBackground(PoseStack) 1.16.5~1.19.4 存活（1182/1192/1194:452）
        screen.renderBackground(this.pose);
    }

    // GuiGraphics.renderComponentTooltip 的版本中性入口（Screen 渲染 tooltip 用）。
    // Screen.renderComponentTooltip(PoseStack, List<Component>, x, y) 4 参全版本存活
    //（1165:126/1182:183/1192:182/1194:250）。
     *///?}
    // renderComponentTooltip 1.16.1 无（1.16.5 Screen.java:126 起）→ 1.16.1 走
    // Screen.renderTooltip(PoseStack, List, x, y)（官方 1161 client.txt :132 实证；元素
    // FormattedText，Component 即其子代，raw List 传入行为等价）。scissorBox 静态版同 GL11 直调。
    //? if <1.16.2 && <1.20 {
    /*public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        screen.renderTooltip(this.pose, (java.util.List) lines, mouseX, mouseY);
    }

    public static void enableScissorBox(int x, int y, int width, int height) {
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

     *///?}
    // scissorBox 静态版分界同前：RenderSystem.enableScissor 1.16.4 才有 → 1.16.2/1.16.3 的
    // renderComponentTooltip 已在（与 >=1.16.4 段同款）但 scissor 走 GL11 直调（javap 实证）。
    //? if >=1.16.2 && <1.16.4 {
    /*public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
    }

    public static void enableScissorBox(int x, int y, int width, int height) {
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

     *///?}
    //? if >=1.16.4 && <1.20 {
    /*public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        screen.renderComponentTooltip(this.pose, lines, mouseX, mouseY);
    }

    public static void enableScissorBox(int x, int y, int width, int height) {
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

     *///?}
    //? if <1.20 {
    /*// 1.16.5 GuiComponent.fillGradient(PoseStack,...)/hLine/vLine 为 protected（实例 1.16.5~1.19.2、
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

    // TextureManager.getTexture：1.16.5 仅单参重载（未注册时 computeIfAbsent 落缺失纹理占位，
    // 与双参版差异=占位条目可能入 byPath，后续 register(location, texture) 会覆盖，语义等价）；
    // 1.17+ 双参（rl, missing）显式占位。1.20 侧版本在下方 >=1.20 段。
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

    //? if >=1.17 && <1.20 {
    /*
    // AbstractWidget.renderScrollingString 1.19.4 才有（1194:119/137 PoseStack，1201 GuiGraphics），
    // 1.17~1.19.2 无 → 按 1201 AbstractWidget.java:123-146 逐式等价（scissor 裁剪 + 左移滚动 +
    // 居中回退），绘制经本门面（fill/drawString/drawCenteredString/enableScissor 全版本中性）。
    // Util.getMillis 1165:?? 与 1182/1192 同名存活，滚动周期公式与 1201 逐字对齐。
    public void renderScrollingString(Font font, Component message, int minX, int minY, int maxX, int maxY, int color) {
        int textWidth = font.width(message);
        int centerY = (minY + maxY - 9) / 2 + 1;
        int availableWidth = maxX - minX;
        if (textWidth > availableWidth) {
            int overflow = textWidth - availableWidth;
            double period = Math.max((double) overflow * 0.5d, 3.0d);
            double phase = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * (double) net.minecraft.Util.getMillis() / 1000.0d / period)) / 2.0d + 0.5d;
            double offset = net.minecraft.util.Mth.lerp(phase, 0.0d, (double) overflow);
            this.enableScissor(minX, minY, maxX, maxY);
            this.drawString(font, message, minX - (int) offset, centerY, color);
            this.disableScissor();
        } else {
            this.drawCenteredString(font, message, (minX + maxX) / 2, centerY, color);
        }
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
    // 1.21.2 GuiGraphics.blit(RL,...) 全家改 Function<ResourceLocation,RenderType> 头参
    //（vanilla-1.21.3 GuiGraphics.java:689-737），GUI 贴图走 guiTexturedOverlay(RL)
    //（RenderType.java:1240，带混合 position_tex，对位旧 innerBlit 语义）
    //? if >=1.20 && <1.21.2 {
    public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        this.graphics.blit(atlas, x, y, u, v, width, height);
    }

    //?}
    //? if >=1.21.2 && <21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        this.graphics.blit(net.minecraft.client.renderer.RenderType::guiTexturedOverlay, atlas, x, y, (float) u, (float) v, width, height, 256, 256);
    }

     */
    //?}
    // 1.21.6+ RenderType 工厂引用删 → RenderPipelines.GUI_TEXTURED（RenderPipelines.java:615）
    //? if >=21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int u, int v, int width, int height) {
        this.graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, atlas, x, y, (float) u, (float) v, width, height, 256, 256);
    }*/
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
    //? if >=1.20 && <1.21.2 {
    public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, renderWidth, renderHeight, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
    }

    //?}
    //? if >=1.21.2 && <21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderType::guiTexturedOverlay, atlas, x, y, uOffset, vOffset, uWidth, vHeight, renderWidth, renderHeight, textureWidth, textureHeight);
    }

     */
    //?}
    //? if >=21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int renderWidth, int renderHeight, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        ysmEnsureTextureView(atlas);
        this.graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, atlas, x, y, uOffset, vOffset, uWidth, vHeight, renderWidth, renderHeight, textureWidth, textureHeight);
    }*/
    //?}

    // 1.21.6 纹理惰性视图（批二 c-2 21.6 线）：register/未 load 纹理的 getTextureView 抛
    // IllegalStateException（AuthorRow 头像/包图标 runClient 崩溃实证）→ blit 前预检，
    // 未就绪同帧 registerAndLoad 补载（SimpleTexture 静态资源同步可用）。
    // 已注册的 OuterFileTexture 走自身 doLoad（textureView 在 doLoad 内创建），不触发本补载
    //? if >=21.6 {
    /*private void ysmEnsureTextureView(ResourceLocation atlas) {
        net.minecraft.client.renderer.texture.AbstractTexture ysmTex = Minecraft.getInstance().getTextureManager().getTexture(atlas);
        try {
            ysmTex.getTextureView();
        } catch (IllegalStateException e) {
            Minecraft.getInstance().getTextureManager().registerAndLoad(atlas, new net.minecraft.client.renderer.texture.SimpleTexture(atlas));
        }
    }*/
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
    //? if >=1.20 && <1.21.2 {
    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    //?}
    //? if >=1.21.2 && <21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderType::guiTexturedOverlay, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    */
    //?}
    //? if >=21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        ysmEnsureTextureView(atlas);
        this.graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }*/
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
    //? if >=1.20 && <1.21.2 {
    public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        // 1.20.1 的带 z blit 是 GuiGraphics 包私有/异序重载（javap 实证 public 无 z-int 形）→
        // 落回无 z 形（z 序层叠差 = 该重载本为 tooltip/悬浮层专用，此处调用点均为面板本体绘制，等价）
        this.graphics.blit(atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    //?}
    //? if >=1.21.2 && <21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderType::guiTexturedOverlay, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

     */
    //?}
    //? if >=21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int z, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }*/
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
    //? if >=1.20 && <1.21.2 {
    public void setColor(float r, float g, float b, float a) {
        this.graphics.setColor(r, g, b, a);
    }

    //?}
    // 1.21.2 GuiGraphics.setColor 删除 → 全局 setShaderColor（vanilla-1.21.3 GuiGraphics
    // 无 setColor 定义、RenderSystem.java:384 仍在）；管线对 shaderColor 的读取
    // 归 runClient 视觉走查核验
    //? if >=1.21.2 && <21.6 {
    /*public void setColor(float r, float g, float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
    }

     */
    //?}
    // 1.21.6+ RenderSystem.setShaderColor 删 → no-op（着色差入功能债）
    //? if >=21.6 {
    /*public void setColor(float r, float g, float b, float a) {
    }*/
    //?}

    /** 垂直渐变填充（GuiGraphics.fillGradient 对位）。 */
    //? if >=1.20 {
    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        this.graphics.fillGradient(minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    /** GuiGraphics.blit(rl,x,y,u,v,w,h,texW,texH)（float u/v + 显式纹理尺寸）对位。 */
    //? if >=1.20 && <1.21.2 {
    public void blit(ResourceLocation atlas, int x, int y, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
    //?}
    //? if >=1.21.2 && <21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderType::guiTexturedOverlay, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }
     */
    //?}
    //? if >=21.6 {
    /*public void blit(ResourceLocation atlas, int x, int y, int uOffset, int vOffset, int width, int height, int textureWidth, int textureHeight) {
        this.graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, atlas, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }*/
    //?}

    /** TextureManager.getTexture(rl, missing) 双参语义：未注册时返回缺失纹理占位（不注册占位条目）。 */
    // 1.21.4 MissingTextureAtlasSprite.getTexture() 删除 → 单参 getTexture
    //（缺省缺纹理占位语义一致，vanilla-1.21.4 TextureManager.java:96）。
    // 用块条件复制方法体（行条件翻转触发 stitcher 注释转义，21.5 产物 /^ 实证——勿改回）
    //? if <21.4 {
    public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().getTexture(location, net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture());
    }
    //?}
    //? if >=21.4 {
    /*public net.minecraft.client.renderer.texture.AbstractTexture getTexture(ResourceLocation location) {
        return Minecraft.getInstance().getTextureManager().getTexture(location);
    }*/
    //?}

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        this.graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics.disableScissor();
    }

    /** 水平/垂直线（GuiGraphics.hLine/vLine 对位）。 */
    public void hLine(int minX, int maxX, int y, int color) {
        //? if <26
        this.graphics.hLine(minX, maxX, y, color);
        // 26.1 改名 horizontalLine（GuiGraphicsExtractor.java:163）
        //? if >=26
        /*this.graphics.horizontalLine(minX, maxX, y, color);*/
    }

    public void vLine(int x, int minY, int maxY, int color) {
        //? if <26
        this.graphics.vLine(x, minY, maxY, color);
        // 26.1 改名 verticalLine（GuiGraphicsExtractor.java:173）
        //? if >=26
        /*this.graphics.verticalLine(x, minY, maxY, color);*/
    }

    /** 按宽折行多行文本（GuiGraphics.drawWordWrap 对位）。 */
    public void drawWordWrap(Font font, net.minecraft.network.chat.FormattedText text, int x, int y, int width, int color) {
        //? if <26
        this.graphics.drawWordWrap(font, text, x, y, width, color);
        // 26.1 改名 textWithWordWrap（GuiGraphicsExtractor.java:277，参序同形）
        //? if >=26
        /*this.graphics.textWithWordWrap(font, text, x, y, width, color);*/
    }

    /** 带 z 序的渐变/纹理绘制（1.20.1 GuiGraphics z 重载；<1.20 无 z 轴 → 落回 0 层，绘制顺序不变）。 */
    public void fillGradient(int minX, int minY, int maxX, int maxY, int z, int colorFrom, int colorTo) {
        //? if <21.6
        this.graphics.fillGradient(minX, minY, maxX, maxY, z, colorFrom, colorTo);
        // 1.21.6+ z 轴删（stratum 制，GuiGraphics.java:171 六参形）
        //? if >=21.6
        /*this.graphics.fillGradient(minX, minY, maxX, maxY, colorFrom, colorTo);*/
    }

    public void renderTooltip(Font font, java.util.List<net.minecraft.util.FormattedCharSequence> lines, int mouseX, int mouseY) {
        //? if <21.6
        this.graphics.renderTooltip(font, lines, mouseX, mouseY);
        // 1.21.6+ tooltip 状态化（GuiGraphics.java:976）
        //? if >=21.6
        /*this.graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);*/
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.graphics.fill(minX, minY, maxX, maxY, color);
    }

    public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
        //? if <21.6
        this.graphics.fill(minX, minY, maxX, maxY, z, color);
        // 1.21.6+ z 轴删（GuiGraphics.java:151）
        //? if >=21.6
        /*this.graphics.fill(minX, minY, maxX, maxY, color);*/
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color, shadow);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color, shadow);*/
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color, shadow);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color, shadow);*/
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color, shadow);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color, shadow);*/
    }

    public void drawString(Font font, Component text, int x, int y, int color) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color);*/
    }

    public void drawString(Font font, String text, int x, int y, int color) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color);*/
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if <26
        this.graphics.drawString(font, text, x, y, color);
        // 26.1 改名 text（GuiGraphicsExtractor.java:235-260 重载族同形）
        //? if >=26
        /*this.graphics.text(font, text, x, y, color);*/
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        //? if <26
        this.graphics.drawCenteredString(font, text, x, y, color);
        // 26.1 改名 centeredText（GuiGraphicsExtractor.java:264-273）
        //? if >=26
        /*this.graphics.centeredText(font, text, x, y, color);*/
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        //? if <26
        this.graphics.drawCenteredString(font, text, x, y, color);
        // 26.1 改名 centeredText（GuiGraphicsExtractor.java:264-273）
        //? if >=26
        /*this.graphics.centeredText(font, text, x, y, color);*/
    }

    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if <26
        this.graphics.drawCenteredString(font, text, x, y, color);
        // 26.1 改名 centeredText（GuiGraphicsExtractor.java:264-273）
        //? if >=26
        /*this.graphics.centeredText(font, text, x, y, color);*/
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        // 1.21.10 GuiGraphics.renderOutline 短暂删除（2110 GuiGraphics 零命中；1.21.11 恢复，
        // 2111 GuiGraphics.java:312 同形）→ 该代 fill 四边 1px 等价改写
        //? if >=21.9 && <21.11 {
        /*this.graphics.fill(x, y, x + width, y + 1, color);
        this.graphics.fill(x, y + height - 1, x + width, y + height, color);
        this.graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        this.graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        return;
        *///?}
        //? if <21.9
        this.graphics.renderOutline(x, y, width, height, color);
        //? if >=21.11 && <26
        this.graphics.renderOutline(x, y, width, height, color);
        // 26.1 改名 outline（GuiGraphicsExtractor.java:211，几何同形=fill 四边）
        //? if >=26
        /*this.graphics.outline(x, y, width, height, color);*/
    }

    /** Screen.renderBackground(GuiGraphics) 的版本中性入口。 */
    public void renderScreenBackground(net.minecraft.client.gui.screens.Screen screen) {
        // 1.21.6 GUI 渲染重构：blur 每帧限一次（GuiRenderState.blurBeforeThisStratum
        // "Can only blur once per frame"，21.6 runClient DisclaimerScreen 崩溃实证）——
        // YSM 屏叠加在 vanilla 屏/同帧双屏时 Screen.renderBackground 的 blurred 背景二调必炸
        // → 21.6~21.10 退化为半透明遮罩 fill（视觉近似暗化背景）。分界勘误（审查修正
        // 2026-09-15）：原「21.10 起 vanilla 调用面无此限制（现役走查实证）」系误证——
        // 批二 b 的 21.10 证据仅为陈旧 jar 主菜单截图（未触 YsmGui 后景面）；21.10 首次
        // 真 GUI 走查（m3-neoforge-server-dist-fix 门禁）在 DisclaimerScreen 渲染即崩
        // 同款 blur 二调（crash-2026-09-15_11.45.51：YsmGui.renderScreenBackground →
        // Screen.renderBackground → renderBlurredBackground → blurBeforeThisStratum
        // IllegalStateException），与 21.6 同病灶 → fill 门扩至 <21.11，vanilla 分支
        // 收窄 >=21.11（21.11+ 仅 compileJava 实证、从未走查，恢复条件不变=逐屏改用
        // vanilla 后景 API，债 debt-216-blur-degraded）
        //（互斥兄弟行条件平铺：铁律禁 else 链与存储态嵌套标记；fill 为注释态存储，
        // 1201 vcs 直编原文铁律——非活跃内容不得以裸码存在于原文）
        //? if >=21.6 && <21.11 {
        /*this.graphics.fill(0, 0, screen.width, screen.height, 0xB8101010);*/
        //?}
        //? if neoforge && <21.6
        /*screen.renderBackground(this.graphics, 0, 0, 0);*/
        //? if neoforge && >=21.11 && <26
        /*screen.renderBackground(this.graphics, 0, 0, 0);*/
        // 26.x：Screen.renderBackground 删 → extractBackground（vanilla-26.1 Screen.java:376，
        // 内含 extractBlurredBackground 的 blurBeforeThisStratum——blur 每帧一次限制是否
        // 复现归 26.x tour 实测，崩则按 aaf7796 同款降级并申报）
        //? if neoforge && >=26
        /*screen.extractBackground(this.graphics, 0, 0, 0);*/
        //? if forge && <21.6
        screen.renderBackground(this.graphics);
        //? if forge && >=21.10
        /*screen.renderBackground(this.graphics);*/
    }

    /** GuiGraphics.renderComponentTooltip 的版本中性入口（Screen 渲染 tooltip 用）。 */
    public void renderScreenComponentTooltip(net.minecraft.client.gui.screens.Screen screen, Font font, List<Component> lines, int mouseX, int mouseY) {
        //? if <21.6
        this.graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        // 1.21.6+ tooltip 状态化（GuiGraphics.java:933）
        //? if >=21.6
        /*this.graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);*/
    }

    /** 已处于窗口像素坐标的裸 scissor（1.20.1 = RenderSystem.enableScissor）。 */
    //? if <21.6 {
    public static void enableScissorBox(int x, int y, int width, int height) {
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
    //?} else {
    /*// 1.21.6+ RenderSystem 剪裁入口删：预览实体立即绘制路径仍受 GL 剪裁约束，原语义直写 GL
    public static void enableScissorBox(int x, int y, int width, int height) {
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x, y, width, height);
    }

    public static void disableScissorBox() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }*/
    //?}
    //?}
}
