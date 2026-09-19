package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * gui-tail-flush：21.6+ 延迟 GUI 网格管线预览 pending 队列。
 * 病灶（d3-gpu-218-revive 遗留债 + GpuCapability.java:26 注释锚）：21.8 起 GUI 延迟网格化，
 * Screen.render 只是收集相（1218 GameRenderer.java:516），此刻的立即直绘发生在 GuiRenderer
 * 绘制窗口之前——GUI 正交要到帧尾 GuiRenderer.render→draw 才打包（1218 GuiRenderer.java:199/:203、
 * 2111 :207/:211），且 GUI 网格批次（executeDrawRange :223/:239）后绘会盖掉先绘的预览。
 * 修法：GpuRenderPath 预览 draw 改入本队列，由 src/neoforge-gpu218 GuiRendererTailMixin 在
 * GuiRenderer.render 窗尾（RETURN）flush——与当帧 GUI 正交同窗、绘制序在 GUI 批次之后不被遮挡。
 * 仅渲染线程读写（enqueue=Screen.render 收集相、flush=GuiRenderer.render 尾，同一线程），无锁。
 * 未挂载 tail mixin 的线（21.6/21.7/21.9/21.10/26.x/旧线）恒 armed=false → GpuRenderPath 立即
 * 直绘原样，零行为回归。
 */
public final class GpuPreviewQueue {
    // 诊断打点（验收面）三通道，沿 NativeModelRenderer.debugHideMatrix 同机制（gradle→MDG run JVM
    // 的 -D/env 传递不稳定，gameDir 文件通道可靠）：1) -Dysm.debug.guiFlush=true
    // 2) 环境变量 YSM_DEBUG_GUI_FLUSH 3) gameDir 下存在 ysm-debug-flush.flag。默认关=零开销。
    private static volatile Boolean debugCache;

    private static boolean debug() {
        Boolean b = debugCache;
        if (b == null) {
            boolean on = Boolean.getBoolean("ysm.debug.guiFlush")
                    || System.getenv("YSM_DEBUG_GUI_FLUSH") != null;
            if (!on) {
                try {
                    // java.io.File（Java 8 口径；Path.of 是 11+，1165 线编译实证）
                    on = new java.io.File("ysm-debug-flush.flag").exists();
                } catch (Throwable ignored) {
                }
            }
            b = on;
            debugCache = b;
        }
        return b;
    }

    // ponytail: 单帧容量帽硬顶（预览面板每帧 parts 数量级为个位数），溢出丢当帧超额 parts 不扩容
    private static final int CAP = 64;

    private static final List<Pending> PENDING = new ArrayList<Pending>();

    /** GuiRendererTailMixin（21.8/21.11 挂载）HEAD 注入置位；未挂载线恒 false。 */
    private static volatile boolean tailHookArmed;

    /** 帧计数：flush 末尾自增；同帧的 enqueue 与 flush 日志共享同号（时序分组证据）。 */
    private static long frame;

    public static boolean isTailHookArmed() {
        return tailHookArmed;
    }

    /** GuiRendererTailMixin 注入点（GuiRenderer.render HEAD）：挂载线 GUI 绘制窗打开即 armed。 */
    public static void ysm$armTailHook() {
        tailHookArmed = true;
    }

    /** GpuRenderPath.tryRender 预览分支喂入（矩阵已按线解析成 JOML：1.17~1.19.2 走 MatrixBridge 换算，
     * 1.19.3+ 直读 pose/RenderSystem）：快照入队（投影不在此取，flush 时刻才消费当帧打包值）。 */
    static void enqueue(GeoModel model, GpuMesh mesh, Matrix4f rootPose, Matrix3f rootNormal, Matrix4f modelView,
                        float[] boneParams, float[] stateBuffer,
                        int textureIndex, int renderPartMask, int packedLight, int packedOverlay,
                        float r, float g, float b, float a, ResourceLocation textureLocation) {
        if (PENDING.size() >= CAP) {
            return;
        }
        // 矩阵必须入队即拷贝：Pose/PoseStack 与 RenderSystem 模型视图底层存储渲染期可复用
        PENDING.add(new Pending(model, mesh,
                new Matrix4f(rootPose), new Matrix3f(rootNormal), new Matrix4f(modelView),
                boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay,
                r, g, b, a, textureLocation));
        if (debug()) {
            System.out.println("[ysm-gui-flush] enqueue frame=" + frame + " pend=" + PENDING.size()
                    + " mesh=" + mesh.pointer + " mask=" + renderPartMask
                    + " thread=" + Thread.currentThread().getName());
        }
    }

    /**
     * GuiRendererTailMixin 注入点（GuiRenderer.render RETURN）：GuiRenderer 绘制窗尾 flush。
     * guiWidth/guiHeight=当帧 GUI 正交的逻辑尺寸（window/guiScale，1218 GuiRenderer.java:204 同式），
     * 供当帧正交指纹比对（CachedOrtho 尺寸未变时不重算 createProjectionMatrix，槽值=当帧打包值逐帧等价）。
     */
    public static void ysm$flush(float guiWidth, float guiHeight) {
        if (PENDING.isEmpty()) {
            frame++;
            return;
        }
        int n = PENDING.size();
        // drawPending 绘制面仅 >=1.17 存在（见 GpuRenderPath 同名注）；<1.17 线 enqueue 入口
        //（tryRender >=21.6 存储分支）恒缺席 → 队列恒空，此循环剔除后 flush 恒走 empty 短路。
        //? if >=1.17 {
        for (int i = 0; i < n; i++) {
            GpuRenderPath.drawPending(PENDING.get(i));
        }
        //?}
        if (debug()) {
            Matrix4f proj = GpuCapability.ysmGuiProjectionReady()
                    ? GpuCapability.ysmCapturedGuiProjection
                    : GpuCapability.ysmCapturedProjection;
            // 当帧期望 GUI 正交指纹：CachedOrthoProjectionMatrixBuffer("gui",1000,11000,invertY=true)
            // → setOrtho(0,w,h,0,1000,11000)（1218 CachedOrthoProjectionMatrixBuffer.java:47-49），
            // m00=2/w、m11=-2/h、m22=-2/(far-near)、m32=-(far+near)/(far-near)
            boolean match = Math.abs(proj.m00() - 2.0f / guiWidth) < 1.0e-3f
                    && Math.abs(proj.m11() + 2.0f / guiHeight) < 1.0e-3f
                    && Math.abs(proj.m22() + 2.0f / 11000.0f) < 1.0e-5f
                    && Math.abs(proj.m32() + 1.2f) < 1.0e-4f;
            System.out.println("[ysm-gui-flush] flush@GuiRenderer.render-TAIL frame=" + frame
                    + " drained=" + n
                    + " orthoFp=[m00=" + proj.m00() + ",m11=" + proj.m11()
                    + ",m22=" + proj.m22() + ",m32=" + proj.m32() + "]"
                    + " guiW=" + guiWidth + " guiH=" + guiHeight
                    + " currentFrameOrtho=" + match
                    + " thread=" + Thread.currentThread().getName());
        }
        PENDING.clear();
        frame++;
    }

    /** 单次预览 draw 的 pending 快照。boneParams/stateBuffer 为同帧存活引用（flush 在同帧稍后）。 */
    static final class Pending {
        final GeoModel model;
        final GpuMesh mesh;
        final Matrix4f rootPose;
        final Matrix3f rootNormal;
        final Matrix4f modelView;
        final float[] boneParams;
        final float[] stateBuffer;
        final int textureIndex;
        final int renderPartMask;
        final int packedLight;
        final int packedOverlay;
        final float r;
        final float g;
        final float b;
        final float a;
        final ResourceLocation textureLocation;

        Pending(GeoModel model, GpuMesh mesh, Matrix4f rootPose, Matrix3f rootNormal, Matrix4f modelView,
                float[] boneParams, float[] stateBuffer,
                int textureIndex, int renderPartMask, int packedLight, int packedOverlay,
                float r, float g, float b, float a, ResourceLocation textureLocation) {
            this.model = model;
            this.mesh = mesh;
            this.rootPose = rootPose;
            this.rootNormal = rootNormal;
            this.modelView = modelView;
            this.boneParams = boneParams;
            this.stateBuffer = stateBuffer;
            this.textureIndex = textureIndex;
            this.renderPartMask = renderPartMask;
            this.packedLight = packedLight;
            this.packedOverlay = packedOverlay;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.textureLocation = textureLocation;
        }
    }
}
