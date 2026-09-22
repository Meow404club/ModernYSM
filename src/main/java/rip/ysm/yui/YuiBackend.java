package rip.ysm.yui;

/**
 * 版本中性绘制原语接口（YUI 后端接缝，docs/ADR-YSM-UI-UNIFIED ADR-1）。
 *
 * <p>铁律（ADR-2）：本包零 vanilla import、零条件轴——同一份编译产物喂全部版本线，
 * grep 门禁（零 {@code import net.minecraft} / {@code import com.mojang} / {@code //?}）是挂载
 * 1.12.2 白名单（build.legacy122.gradle.kts legacy122Include）的硬前提。
 *
 * <p>实现按能力代切（ADR-1），当前 2+1 个：
 * <ul>
 *   <li>YuiBackendYsmGui（rip/ysm/gui，>=1.16.5 全部现代线，委托既有 YsmGui——
 *       PoseStack/GuiGraphics/Matrix3x2fStack/26.x 四代差异由 YsmGui 内部吸收）</li>
 *   <li>YuiBackendGL1122（versions/1.12.2-forge，GL11 固定管线，MCP 面）</li>
 *   <li>YuiBackendGL1710（M-U3，暂缺）</li>
 * </ul>
 *
 * <p>坐标系约定：全部入参出参均为 GUI 逻辑坐标（GuiGraphics/ScaledResolution 面，
 * 左上原点，y 向下）；GUI→GL 窗口坐标的 Y 翻转换算只发生在各后端实现内部
 * （现代线 YsmGui.enableScissor 已吸收；固定管线 glScissor 用
 * {@code glY = windowHeight - y2*guiScale} 数学，1.12.2 在产先例
 * LegacyModelSelectScreen:159-161）。
 *
 * <p>绘制分层约定：接口不带 z 序（1.12.2/1.7.10 固定管线无 z 轴语义），
 * 层叠关系由调用顺序（后画在上）表达；现代线的 z 参数由 YsmGui 内部落 0 层。
 */
public interface YuiBackend {

    /** 实底矩形填充（x2/y2 为排他下界，同 GuiGraphics.fill/drawRect 语义）。 */
    void fillRect(int x1, int y1, int x2, int y2, int color);

    /** 垂直渐变填充（colorFrom 顶 → colorTo 底，GuiGraphics.fillGradient 对位）。 */
    void fillGradientV(int x1, int y1, int x2, int y2, int colorFrom, int colorTo);

    /** 1px 四边描边（GuiGraphics.renderOutline 几何：上下整行+左右中间列）。 */
    void outlineRect1px(int x, int y, int width, int height, int color);

    /**
     * 贴图子区域绘制（GuiGraphics.blit(rl,x,y,u,v,w,h,texW,texH) 对位）。
     * u/v 为纹理像素偏移；宽高独立于纹理尺寸（可缩放）。
     */
    void blit(Texture texture, int x, int y, int u, int v, int width, int height);

    /** 压入裁剪矩形（GUI 逻辑坐标，可嵌套，pop 恢复外层）。 */
    void scissorPush(int x1, int y1, int x2, int y2);

    /** 弹出裁剪矩形；栈空时关闭裁剪。 */
    void scissorPop();

    /**
     * 文本绘制。x 的含义由 align 决定：LEFT=左缘 / CENTER=中线 / RIGHT=右缘。
     * color 为 ARGB（仅 RGB 生效，字体渲染器自动补 alpha）。
     */
    void drawText(String text, int x, int y, int color, boolean shadow, Align align);

    /** 居中文本（恒带阴影，vanilla drawCenteredString 语义）。 */
    void drawTextCentered(String text, int centerX, int y, int color);

    /** 文本像素宽（当前 GUI 字体）。 */
    int textWidth(String text);

    /** 当前 GUI 缩放倍率（ScaledResolution.getScaleFactor / Window.getGuiScale）。 */
    double guiScale();

    /** 窗口像素高度（Y 翻转换算用，Minecraft.displayHeight / Window.getHeight）。 */
    int windowHeight();

    /**
     * 实体预览挂点（YuiPreview 回调槽）。M-U1 仅定接口+空实现，真预览等
     * M-U1R（1.12.2 固定管线 POC）定稿签名后由各后端实现：
     * 现代线直调 ModelPreviewRenderer.renderLivingEntityPreview（26.x 相位契约），
     * legacy 线直调版本树翻译层（RenderManager 路径不可用）。
     */
    default void preview(YuiPreview preview, int x1, int y1, int x2, int y2,
                         float mouseX, float mouseY, float partialTick) {
    }

    /** 文本对齐。 */
    enum Align {
        LEFT, CENTER, RIGHT
    }

    /**
     * 版本中性贴图引用（yui 树不可 import ResourceLocation——mojmap/MCP 两映射名空间
     * 不同类，以路径字符串过缝，各后端自行构造型）。
     */
    final class Texture {
        public final String path;
        public final int width;
        public final int height;

        public Texture(String path, int width, int height) {
            this.path = path;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "yuiTexture[" + path + " " + width + "x" + height + "]";
        }
    }
}
