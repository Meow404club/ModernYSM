package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
//? if >1.17 {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import rip.ysm.compat.oculus.OculusCompat;

import java.nio.ByteBuffer;
//?}

public final class IrisRenderPath {
    private static final float[] modelViewScratch = new float[16];


    public static boolean tryRender(GeoModel model, PoseStack.Pose pose, float[] boneParams, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, ResourceLocation textureLocation) {
        //? if <1.17 {
        /*return false;
         *///?} else {
        return tryRenderModern(model, pose, boneParams, renderPartMask, packedLight, packedOverlay, r, g, b, a, textureLocation);
        //?}
    }

    // 1.16.5 降级：整路径不存在（GL43 compute + 1.17+ ShaderInstance uniform 管线 +
    // RenderSystem.getShader/getProjectionMatrix 均为 1.17+ API；Iris 1.16.5 无官方移植，
    // oculus 系 compat 包也被 1.16.5 sourceSet 排除——记录功能差，渲染回退 geckolib3 原路径）
    //? if >1.17 {
    private static boolean tryRenderModern(GeoModel model, PoseStack.Pose pose, float[] boneParams, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, ResourceLocation textureLocation) {
        if (!GpuCapability.isAvailable()) return false;
        if (!BoneXformCompute.ensureCompiled()) return false;
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return false;

        GpuMesh mesh = GpuRenderPath.getOrBuildMesh(model);
        if (mesh == null) return false;
        mesh.ensureXformBuffers();


        ByteBuffer boneBuf = mesh.perFrameBoneBuffer;
        boneBuf.clear();
        GeoModel.nComputeBoneMatricesLocal(mesh.pointer, boneParams, packedLight, boneBuf);
        boneBuf.position(0);
        boneBuf.limit(mesh.boneCount * 144);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mesh.boneSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, boneBuf);

        GlStateManager._glUseProgram(BoneXformCompute.program());
        if (BoneXformCompute.locColor() >= 0) GL20.glUniform4f(BoneXformCompute.locColor(), r, g, b, a);
        if (BoneXformCompute.locOverlay() >= 0) GL20.glUniform1i(BoneXformCompute.locOverlay(), packedOverlay);

        if (BoneXformCompute.locModelView() >= 0) {
            //? if >=1.17 && <1.19.4
            /*com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.pose(pose).get(modelViewScratch);*/
            //? if >=1.19.4
            pose.pose().get(modelViewScratch);
            GL20.glUniformMatrix4fv(BoneXformCompute.locModelView(), false, modelViewScratch);
        }

        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, mesh.vbo);
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, mesh.xformVbo());
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, mesh.boneSsbo);

        GL43.glDispatchCompute(BoneXformCompute.dispatchGroupCount(mesh.vertexCount), 1, 1);

        GL43.glMemoryBarrier(GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT);

        GlStateManager._glUseProgram(0);

        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, 0);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        RenderType rt = RenderType.entityCutoutNoCull(textureLocation);
        rt.setupRenderState();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            rt.clearRenderState();
            return false;
        }

        // MODEL_VIEW/PROJECTION 为 Uniform（1192 ShaderInstance.java:66/68），Uniform.set(Matrix4f)
        // 参数型随版本走（<1.19.3 moj / 1.19.4+ joml），同一源码两侧均合法，无需分段；
        // GLINT_ALPHA 1.19.4 起才有（1192 ShaderInstance 无此字段）
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(pose.pose());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
        //? if >=1.19.4
        if (shader.GLINT_ALPHA != null) shader.GLINT_ALPHA.set(1.0f);

        shader.apply();

        GlStateManager._glBindVertexArray(mesh.xformVao());

        int offsetBytes = mesh.indexOffsetBytes(renderPartMask);
        int drawCount = mesh.indexDrawCount(renderPartMask);
        if (drawCount > 0) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, drawCount, GL11.GL_UNSIGNED_INT, offsetBytes);
        }

        shader.clear();
        //? if >=1.19.2
        com.mojang.blaze3d.vertex.BufferUploader.invalidate();
        //? if <1.19.2
        /*com.mojang.blaze3d.vertex.BufferUploader.reset();*/
        GlStateManager._glBindVertexArray(0);
        rt.clearRenderState();

        return true;
    }
    //?}
}
