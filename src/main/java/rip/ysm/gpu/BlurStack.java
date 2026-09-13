package rip.ysm.gpu;

// 1.21.5 GlStateManager 迁移 platform→opengl 包
// 1.21.5 GlStateManager 迁移 platform→opengl 包（vcs 直通铁律：非 1.20.1 分支源码态必须注释）
//? if <21.5
import rip.ysm.util.RenderCompat;
//? if <21.5
import com.mojang.blaze3d.platform.GlStateManager;
//? if >=21.5
/*import com.mojang.blaze3d.opengl.GlStateManager;*/
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import org.joml.Matrix4f;
//?}
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.List;

public final class BlurStack {
    private static final List<Region> regions = new ArrayList<>();
    //? if >1.17 {
    private static final Matrix4f mvpScratch = new Matrix4f();
    private static final float[] mvpFloats = new float[16];
    private static long frameCounter = 0L;
    //?}

    private BlurStack() {
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius) {
        pushBlur(x, y, w, h, cornerRadius, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius, int tintRgba) {
        Region r = new Region();
        r.isPie = false;
        r.x = x;
        r.y = y;
        r.w = w;
        r.h = h;
        r.cornerRadius = cornerRadius;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius) {
        pushBlurPie(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius, int tintRgba) {
        float pad = 1.0f;
        Region r = new Region();
        r.isPie = true;
        r.x = centerX - outerRadius - pad;
        r.y = centerY - outerRadius - pad;
        r.w = (outerRadius + pad) * 2.0f;
        r.h = (outerRadius + pad) * 2.0f;
        r.pieCenterX = centerX;
        r.pieCenterY = centerY;
        r.pieInner = innerRadius;
        r.pieOuter = outerRadius;
        r.pieStart = startAngle;
        r.pieEnd = endAngle;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void popBlur() {
        if (!regions.isEmpty()) regions.remove(regions.size() - 1);
    }

    public static void clear() {
        regions.clear();
    }

    public static boolean isEmpty() {
        return regions.isEmpty();
    }

    // flush 参数改 PoseStack（调用方传 graphics.pose()）；1.16.5 降级为空实现：
    // 毛玻璃依赖 1.17+ capture/shader 批处理管线（RenderSystem.getProjectionMatrix 1.16.5 不存在），
    // 1.16.5 记录功能差：GUI 背景不模糊（半透明底色仍可读），区域队列清空防泄漏
    public static void flush(PoseStack pose) {
        //? if <1.17 {
        /*flushLegacy();
         *///?} else {
        flushModern(pose);
        //?}
    }

    //? if <1.17 {
    /*private static void flushLegacy() {
        regions.clear();
    }
     *///?} else {
    private static void flushModern(PoseStack pose) {
        // 1.21.5+ RenderPipeline 化：毛玻璃 capture（RenderTarget 抽象化删 frameBufferId）与
        // blend/cull/depth 立即绘制状态删除 → Blur 整路径降级，仅清区域队列防泄漏（裁决②，功能债）。
        // SearchSuggestions.pushBlur/flush 调用点保持原样（pushBlur 只入队、flush 降级后即时清空）。
        //? if >=21.5
        /*regions.clear();
        return;*/
        //? if <21.5 {
        if (!BlurShader.ensureCompiled()) {
            regions.clear();
            return;
        }

        frameCounter++;
        BlurShader.captureScreen(frameCounter);

        //? if >=1.19.4 {
        RenderSystem.getProjectionMatrix().mul(RenderSystem.getModelViewMatrix(), mvpScratch);
        mvpScratch.mul(pose.last().pose());
        mvpScratch.get(mvpFloats);
        //?}
        //? if >=1.17 && <1.19.4 {
        /*
        // 1.17~1.19.2 RenderSystem/PoseStack 产出 mojang 矩阵 → MatrixBridge 恒等拷贝转 JOML
        com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.projectionMatrix().mul(com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.modelViewMatrix(), mvpScratch);
        mvpScratch.mul(com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.pose(pose.last()));
        mvpScratch.get(mvpFloats);
         *///?}

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(BlurShader.captureTextureId());
        GlStateManager._glUseProgram(BlurShader.program());

        if (BlurShader.locProj() >= 0) GL20.glUniformMatrix4fv(BlurShader.locProj(), false, mvpFloats);
        if (BlurShader.locScreenSize() >= 0)
            GL20.glUniform2f(BlurShader.locScreenSize(), BlurShader.captureWidth(), BlurShader.captureHeight());
        if (BlurShader.locGamma() >= 0) GL20.glUniform1f(BlurShader.locGamma(), 6.0f);

        GlStateManager._glBindVertexArray(BlurShader.dummyVao());

        for (Region r : regions) {
            float tr = ((r.tintRgba >> 16) & 0xFF) / 255.0f;
            float tg = ((r.tintRgba >> 8) & 0xFF) / 255.0f;
            float tb = (r.tintRgba & 0xFF) / 255.0f;
            float ta = ((r.tintRgba >> 24) & 0xFF) / 255.0f;
            if (BlurShader.locRect() >= 0) GL20.glUniform4f(BlurShader.locRect(), r.x, r.y, r.w, r.h);
            if (BlurShader.locRectSize() >= 0) GL20.glUniform2f(BlurShader.locRectSize(), r.w, r.h);
            if (BlurShader.locBlurRadius() >= 0)
                GL20.glUniform1f(BlurShader.locBlurRadius(), Math.max(1.0f, r.blurRadius));
            if (BlurShader.locTint() >= 0) GL20.glUniform4f(BlurShader.locTint(), tr, tg, tb, ta);
            if (r.isPie) {
                if (BlurShader.locMode() >= 0) GL20.glUniform1i(BlurShader.locMode(), 1);
                if (BlurShader.locPieCenter() >= 0)
                    GL20.glUniform2f(BlurShader.locPieCenter(), r.pieCenterX, r.pieCenterY);
                if (BlurShader.locPieInner() >= 0) GL20.glUniform1f(BlurShader.locPieInner(), r.pieInner);
                if (BlurShader.locPieOuter() >= 0) GL20.glUniform1f(BlurShader.locPieOuter(), r.pieOuter);
                if (BlurShader.locPieStart() >= 0) GL20.glUniform1f(BlurShader.locPieStart(), r.pieStart);
                if (BlurShader.locPieEnd() >= 0) GL20.glUniform1f(BlurShader.locPieEnd(), r.pieEnd);
                if (BlurShader.locPieFeather() >= 0) GL20.glUniform1f(BlurShader.locPieFeather(), 1.0f);
            } else {
                if (BlurShader.locMode() >= 0) GL20.glUniform1i(BlurShader.locMode(), 0);
                if (BlurShader.locRadius() >= 0) GL20.glUniform1f(BlurShader.locRadius(), r.cornerRadius);
                if (BlurShader.locCorner() >= 0) GL20.glUniform4f(BlurShader.locCorner(), 1.0f, 1.0f, 1.0f, 1.0f);
            }
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        }

        GlStateManager._glUseProgram(0);
        //? if >=1.19.2
        RenderCompat.invalidate();
        //? if <1.19.2
        /*RenderCompat.reset();*/
        GlStateManager._glBindVertexArray(0);
        RenderSystem.disableBlend();

        regions.clear();
        //?}
    }
    //?}

    private static final class Region {
        boolean isPie;
        float x;
        float y;
        float w;
        float h;
        float cornerRadius;
        float pieCenterX;
        float pieCenterY;
        float pieInner;
        float pieOuter;
        float pieStart;
        float pieEnd;
        float blurRadius;
        int tintRgba;
    }
}
