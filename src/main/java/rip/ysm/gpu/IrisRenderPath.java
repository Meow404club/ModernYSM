package rip.ysm.gpu;

import rip.ysm.util.RenderCompat;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
// 1.21.5 GlStateManager 迁移 platform→opengl 包
//? if <21.5
import com.mojang.blaze3d.platform.GlStateManager;
// 1.21.5 GlStateManager 迁移 platform→opengl 包（vcs 直通铁律：非 1.20.1 分支源码态必须注释）
//? if >=21.5
/*import com.mojang.blaze3d.opengl.GlStateManager;*/
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
//? if >1.17 {
// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
//?}
// 1.21.2 ShaderInstance 删除（ShaderManager/CompiledShaderProgram 重构，vanilla-1.21.3
// 无该类）→ ShaderInstance 触点全部收进 <1.21.2 分支，1.21.2+ 本路径降级；
// import 用行条件裸行（1.20.1 活跃节点 shader 段需要，>=1.21.2 生成线条件假自动注释）
//? if >1.17 && <1.21.2
import net.minecraft.client.renderer.ShaderInstance;
//? if >1.17 {
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
    // debug-1201-windows-gpu: 见 tryRenderModern 顶部注记——真机回归通过后置 false 恢复
    private static final boolean DISABLED_PENDING_REAL_GPU_VALIDATION = true;
    private static final float[] modelViewScratch = new float[16];


    public static boolean tryRender(GeoModel model, PoseStack.Pose pose, float[] boneParams, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, ResourceLocation textureLocation) {
        //? if <1.17
        /*return false;*/
        // 1.21.2+ 降级：ShaderInstance 删除后 uniform 直写面随 CompiledShaderProgram 重构变化，
        // 且 iris neoforge 生态未接——本路径整体回退 geckolib3 原路径（与 1.16.5 同级功能债）
        //? if >=1.21.2
        /*return false;*/
        //? if >=1.17 && <1.21.2
        return tryRenderModern(model, pose, boneParams, renderPartMask, packedLight, packedOverlay, r, g, b, a, textureLocation);
    }

    // 1.16.5 降级：整路径不存在（GL43 compute + 1.17+ ShaderInstance uniform 管线 +
    // RenderSystem.getShader/getProjectionMatrix 均为 1.17+ API；Iris 1.16.5 无官方移植，
    // oculus 系 compat 包也被 1.16.5 sourceSet 排除——记录功能差，渲染回退 geckolib3 原路径）
    //? if >1.17 && <1.21.2 {
    private static boolean tryRenderModern(GeoModel model, PoseStack.Pose pose, float[] boneParams, int renderPartMask, int packedLight, int packedOverlay, float r, float g, float b, float a, ResourceLocation textureLocation) {
        // debug-1201-windows-gpu: 真实 GPU（用户 AMD RX 7900 XT / Win11 Adrenalin 26.8.1）上本路径
        // 世界内全黑——直绘取 RenderSystem.getShader() 在 Iris 管线内是 Iris 包装 shader + G-buffer
        // FBO 绑定，驱动相关地画空却恒 return true 吞掉回退（llvmpipe 软件渲染同路径可见，属宽容）。
        // 回退 native SIMD 缓冲路径（原版管线 = Iris 兼容，compat 渲染器同链已实证可见）。
        // GL43 compute 直绘待真机回归验证后再恢复，勿删下方实现。
        if (DISABLED_PENDING_REAL_GPU_VALIDATION) {
            return false;
        }
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
            //? if >=1.17 && <1.19.3
            /*com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.pose(pose).get(modelViewScratch);*/
            //? if >=1.19.3
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

        //? if <1.21.2 {
        // 裸内层（内容 1.20.1 同在，vcs 直通合规）：存储态包裹会令 1.20.1 活跃节点丢失
        // shader 段=Iris 路径空绘制仍 return true（双在产线终验实证）
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            rt.clearRenderState();
            return false;
        }

        // MODEL_VIEW/PROJECTION 为 Uniform（1192 ShaderInstance.java:66/68），Uniform.set(Matrix4f)
        // 参数型随版本走（<1.19.3 moj / 1.19.4+ joml），同一源码两侧均合法，无需分段
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(pose.pose());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
        // GLINT_ALPHA 1.19.4 起才有（1192/1.19.3 ShaderInstance 无此字段）；序=基线（apply 前置上传）
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
        //?}
        //? if >=1.19.2 && <21.5
        RenderCompat.invalidate();
        //? if <1.19.2
        /*RenderCompat.reset();*/
        GlStateManager._glBindVertexArray(0);
        rt.clearRenderState();

        return true;
    }
    //?}
}
