package rip.ysm.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >1.17 {
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferUploader;
//?}
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * 扇形/圆盘 GPU 绘制（自管 GL20 着色器）。双轴差异只在 MVP 组合：
 * <ul>
 *   <li>1.20.1：RenderSystem.getProjectionMatrix()/getModelViewMatrix()（joml Matrix4f）×
 *       pose.last().pose()（GuiGraphics.pose() 链路），joml mul/get(float[]) 列主序</li>
 *   <li>1.16.5：无上述 RenderSystem 存取器（1.16.5 RenderSystem 无 getProjectionMatrix/
 *       getModelViewMatrix，也无 modelViewStack），改从 GL 固定状态 glGetFloatv 取 P/MV
 *       （1.16.5 GuiComponent.fill 把 PoseStack 矩阵烘进顶点，GL MODELVIEW 另含相机/GUI 基阵），
 *       mojang Matrix4f 仅 store(FloatBuffer)（列主序，Matrix4f.java:161），P*MV*pose 用本地
 *       mat4Mul 组合，与 1.20.1 的 proj.mul(modelview).mul(pose) 顺序一致</li>
 * </ul>
 */
public final class Pie {
    public static final float tau = (float) (Math.PI * 2.0);
    //? if >1.17 {
    private static final Matrix4f mvpScratch = new Matrix4f();
    //?}
    private static final float[] mvpFloats = new float[16];

    public static void draw(PoseStack pose, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba) {
        draw(pose, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, rgba, 1.0f);
    }

    public static void draw(PoseStack pose, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba, float feather) {
        if (!PieShader.ensureCompiled()) return;

        float pad = feather + 1.0f;
        float rectX = centerX - outerRadius - pad;
        float rectY = centerY - outerRadius - pad;
        float rectW = (outerRadius + pad) * 2.0f;
        float rectH = (outerRadius + pad) * 2.0f;

        //? if >=1.19.4 {
        RenderSystem.getProjectionMatrix().mul(RenderSystem.getModelViewMatrix(), mvpScratch);
        mvpScratch.mul(pose.last().pose());
        mvpScratch.get(mvpFloats);
        //?}
        //? if >=1.17 && <1.19.4 {
        /*
        // 1.17~1.19.2 矩阵源为 mojang → MatrixBridge 转 JOML（proj×mv×pose 数学不变）
        com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.projectionMatrix().mul(com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.modelViewMatrix(), mvpScratch);
        mvpScratch.mul(com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.pose(pose.last()));
        mvpScratch.get(mvpFloats);
         *///?}
        //? if <1.17 {
        /*// glGetFloatv/store 会推进 buffer position：不清零则第二次调用 remaining=0
        // → LWJGL Checks.checkBuffer 抛 IAE（一帧画多个扇形必现）
        projBuf.clear();
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projBuf);
        projBuf.rewind();
        projBuf.get(projArr);
        modelViewBuf.clear();
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewBuf);
        modelViewBuf.rewind();
        modelViewBuf.get(modelViewArr);
        poseBuf.clear();
        pose.last().pose().store(poseBuf);
        poseBuf.rewind();
        poseBuf.get(poseArr);
        mat4Mul(projArr, modelViewArr, tmpArr);
        mat4Mul(tmpArr, poseArr, mvpFloats);
         *///?}

        float cr = ((rgba >> 16) & 0xFF) / 255.0f;
        float cg = ((rgba >> 8) & 0xFF) / 255.0f;
        float cb = (rgba & 0xFF) / 255.0f;
        float ca = ((rgba >> 24) & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        GlStateManager._glUseProgram(PieShader.program());

        if (PieShader.locProj() >= 0) GL20.glUniformMatrix4fv(PieShader.locProj(), false, mvpFloats);
        if (PieShader.locRect() >= 0) GL20.glUniform4f(PieShader.locRect(), rectX, rectY, rectW, rectH);
        if (PieShader.locCenter() >= 0) GL20.glUniform2f(PieShader.locCenter(), centerX, centerY);
        if (PieShader.locOuterRadius() >= 0) GL20.glUniform1f(PieShader.locOuterRadius(), outerRadius);
        if (PieShader.locInnerRadius() >= 0) GL20.glUniform1f(PieShader.locInnerRadius(), Math.max(0.0f, innerRadius));
        if (PieShader.locStartAngle() >= 0) GL20.glUniform1f(PieShader.locStartAngle(), startAngle);
        if (PieShader.locEndAngle() >= 0) GL20.glUniform1f(PieShader.locEndAngle(), endAngle);
        if (PieShader.locColor() >= 0) GL20.glUniform4f(PieShader.locColor(), cr, cg, cb, ca);
        if (PieShader.locFeather() >= 0) GL20.glUniform1f(PieShader.locFeather(), feather);

        //? if <1.17 {
        /*GL30.glBindVertexArray(PieShader.dummyVao());
         *///?} else {
        GlStateManager._glBindVertexArray(PieShader.dummyVao());
        //?}
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GlStateManager._glUseProgram(0);
        //? if >1.17 {
        //? if >=1.19.2
        BufferUploader.invalidate();
        //? if <1.19.2
        /*BufferUploader.reset();*/
        //?}
        //? if <1.17 {
        /*GL30.glBindVertexArray(0);
         *///?} else {
        GlStateManager._glBindVertexArray(0);
        //?}

        RenderSystem.disableBlend();
    }

    //? if <1.17 {
    /*private static final java.nio.FloatBuffer projBuf = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static final java.nio.FloatBuffer modelViewBuf = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static final java.nio.FloatBuffer poseBuf = java.nio.ByteBuffer.allocateDirect(16 * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float[] projArr = new float[16];
    private static final float[] modelViewArr = new float[16];
    private static final float[] poseArr = new float[16];
    private static final float[] tmpArr = new float[16];

    // 列主序 4x4 乘法：out = a * b（与 joml mul/mojang multiply 同语义、同排布）
    private static void mat4Mul(float[] a, float[] b, float[] out) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + row] * b[col * 4 + k];
                }
                out[col * 4 + row] = sum;
            }
        }
    }
     *///?}
}
