package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

public final class GpuCapability {
    private static volatile boolean checked = false;
    private static volatile boolean available = false;
    private static volatile String reason = null;

    // ===== 21.8+ CPU 矩阵捕获面（d3-gpu-218-revive）=====
    // vanilla 21.8 起 RenderSystem 删 CPU 投影/雾读取（GpuBufferSlice 化），但 vanilla 在打包
    // UBO 前仍 CPU 现算全部数值（反编译实证：投影 GameRenderer:647 现算→:671 打包；雾
    // FogRenderer.setupFog:166 CPU 全参→:188 updateBuffer 打包；21111 同构 :749/:771、:162/:184）。
    // 由 21.8/21.11 线挂载的捕获 mixin（src/neoforge-gpu218/ 四类）在打包点把 CPU 数值喂回：
    // 语义=旧线 RenderSystem.getProjectionMatrix()/getShaderFog*() 的逐点镜像。
    // 仅渲染线程读写（捕获点与 GpuRenderPath.tryRender 同线程），无锁；非 21.8+ 线恒未捕获=死字段。
    // 包内直读（GpuRenderPath 同包消费），喂入走下方 public 方法（mixin 异包调用）。
    static final Matrix4f ysmCapturedProjection = new Matrix4f();
    // GUI 正交独立槽（CachedOrtho：gui zNear/Far=1000/11000，罩住预览 z=1250）。21.8 GUI
    // 改延迟管线：Screen.render 立即式绘制时刻「当前」投影=hud3d 透视（far=100，会把
    // z=1250 预览整裁），而 GUI 正交在帧尾 GuiRenderer.draw 才打包——预览消费上一帧值
    //（尺寸恒定，逐帧等价），对齐旧线「GUI 期 getProjectionMatrix()=GUI 正交」语义。
    static final Matrix4f ysmCapturedGuiProjection = new Matrix4f();
    private static boolean ysmGuiProjectionCaptured = false;
    static final float[] ysmCapturedFogColor = new float[4];
    static float ysmCapturedFogStart;
    static float ysmCapturedFogEnd;
    private static boolean ysmProjectionCaptured = false;
    private static boolean ysmFogCaptured = false;

    /** 捕获 mixin 喂入点：vanilla 打包投影 UBO 前的 CPU Matrix4f（喂后为当前帧有效值）。 */
    public static void ysm$onProjectionCaptured(Matrix4f proj) {
        ysmCapturedProjection.set(proj);
        ysmProjectionCaptured = true;
    }

    /** 捕获 mixin 喂入点：GUI/PiP 正交（CachedOrtho.createProjectionMatrix 产物，独立槽）。 */
    public static void ysm$onGuiProjectionCaptured(Matrix4f proj) {
        ysmCapturedGuiProjection.set(proj);
        ysmGuiProjectionCaptured = true;
    }

    static boolean ysmGuiProjectionReady() {
        return ysmGuiProjectionCaptured;
    }

    /** 捕获 mixin 喂入点：vanilla 雾 UBO 打包参数（色 rgba + environmentalStart/End=旧 FogStart/End 槽位）。 */
    public static void ysm$onFogCaptured(float r, float g, float b, float a, float envStart, float envEnd) {
        ysmCapturedFogColor[0] = r;
        ysmCapturedFogColor[1] = g;
        ysmCapturedFogColor[2] = b;
        ysmCapturedFogColor[3] = a;
        ysmCapturedFogStart = envStart;
        ysmCapturedFogEnd = envEnd;
        ysmFogCaptured = true;
    }

    // 21.8+ 复活门（d3-gpu-218-revive）：投影+雾捕获都就绪才放行；任一环缺席
    //（mixin 未挂载的版本线/vanilla 改面未触发）→ 保持降级，与历史恒 true 行为零回归。
    // 经方法调用（编译器不可折叠 → 后续语句不算不可达）
    private static boolean cpuMatrixUnavailable() {
        return !ysmProjectionCaptured || !ysmFogCaptured;
    }

    public static boolean isAvailable() {
        // 21.8+ RenderSystem 删 CPU 投影/雾读取（GpuBufferSlice 化）→ 由捕获 mixin 喂回
        //（vanilla 打包点证据见捕获面注记）；未捕获时保持整体降级 vanilla CPU 渲染
        //? if >=21.8 {
        /*if (cpuMatrixUnavailable()) {
            reason = "1.21.8+ proj/fog CPU capture inactive (capture mixins absent or not fired)";
            return false;
        }*/
        //?}
        if (!checked) check();
        return available;
    }

    public static String getReason() {
        if (!checked) check();
        return reason;
    }

    public static synchronized void check() {
        if (checked) return;
        checked = true;

        if (System.getProperty("OYSM_DISABLE_GPU") != null) {
            reason = "gpu renderer has been disabled";
            return;
        }
        if (!NativeLibLoader.isLoaded()) {
            reason = "native ysm-core not loaded";
            return;
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac") || osName.contains("darwin")) {
            reason = "macOS GL is capped at 4.1 and lacks GL_ARB_shader_storage_buffer_object";
            return;
        }

        GLCapabilities caps;
        String glVersion;
        String glRenderer;
        String glVendor;
        String glslVersion;
        try {
            //? if >1.17 {
        //? if <1.18
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);*/
        //? if >=1.18 && <21.5
        RenderSystem.assertOnRenderThreadOrInit();
        // 1.21.5 删 OrInit 形（21.5 RenderSystem 仅 assertOnRenderThread:109）
        //? if >=21.5
        /*RenderSystem.assertOnRenderThread();*/
        //?}
            caps = GL.getCapabilities();
            glVersion = GL11.glGetString(GL11.GL_VERSION);
            glRenderer = GL11.glGetString(GL11.GL_RENDERER);
            glVendor = GL11.glGetString(GL11.GL_VENDOR);
            glslVersion = GL11.glGetString(0x8B8C);
        } catch (Throwable t) {
            reason = "GL capabilities not available: " + t.getMessage();
            return;
        }

        if (glVersion == null) {
            reason = "GL version not available";
            return;
        }

        System.out.println("OpenGL version: " + glVersion);
        System.out.println("OpenGL renderer version: " + glRenderer);
        System.out.println("OpenGL vendor: " + glVendor);
        System.out.println("OpenGL glsl version: " + glslVersion);

        if (!caps.OpenGL30) {
            reason = "OpenGL 3.0 not supported (got " + glVersion + ")";
            return;
        }

        boolean hasSsbo = caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object;
        boolean hasIfaceQuery = caps.OpenGL43 || caps.GL_ARB_program_interface_query;
        boolean hasLayoutBinding = caps.OpenGL42 || caps.GL_ARB_shading_language_420pack;
        boolean hasExplicitAttrib = caps.OpenGL33 || caps.GL_ARB_explicit_attrib_location;
        boolean hasPackedNormal = caps.OpenGL33 || caps.GL_ARB_vertex_type_2_10_10_10_rev;
        if (!hasSsbo) {
            reason = "SSBO not supported, GL_VERSION=" + glVersion;
            return;
        }
        if (!hasIfaceQuery) {
            reason = "GL_ARB_program_interface_query not supported; GL_VERSION=" + glVersion;
            return;
        }
        if (!hasLayoutBinding) {
            reason = "GL_ARB_shading_language_420pack not supported; GL_VERSION=" + glVersion;
            return;
        }
        if (!hasExplicitAttrib) {
            reason = "GL_ARB_explicit_attrib_location not supported; GL_VERSION=" + glVersion;
            return;
        }
        if (!hasPackedNormal) {
            reason = "GL_ARB_vertex_type_2_10_10_10_rev not supported; GL_VERSION=" + glVersion;
            return;
        }

        available = true;
        reason = "ok (GL " + glVersion + ", " + glRenderer + ")";
    }
}
