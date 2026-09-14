package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

public final class GpuCapability {
    private static volatile boolean checked = false;
    private static volatile boolean available = false;
    private static volatile String reason = null;

    // 1.21.8+ 降级闸门：恒 true，但经方法调用（编译器不可折叠 → 后续语句不算不可达）
    private static boolean cpuMatrixUnavailable() {
        return true;
    }

    public static boolean isAvailable() {
        // 1.21.8+ RenderSystem 删 CPU 投影/雾读取（GpuBufferSlice 化，RenderSystem.java:296/168）→
        // GL43 compute 蒙皮路径无法取 CPU 矩阵，整体降级 vanilla CPU 渲染（性能债，1.21.1 native 降级先例）
        //? if >=21.8 {
        /*if (cpuMatrixUnavailable()) {
            reason = "1.21.8+ removed CPU-side projection/fog access from RenderSystem";
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
        //? if >=1.18.2 && <1.18.2
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);*/
        //? if >=1.18.2 && <21.5
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
