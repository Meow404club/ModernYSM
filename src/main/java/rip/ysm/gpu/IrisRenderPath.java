package rip.ysm.gpu;

import rip.ysm.util.RenderCompat;
import com.elfmcys.yesstevemodel.YesSteveModel;
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
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import rip.ysm.compat.oculus.OculusCompat;

import java.nio.ByteBuffer;
//?}

public final class IrisRenderPath {
    // 数值打点限频（3+600 风格）：前 3 次必打，之后每 600 次 1 条
    private static int irisDbgDrawCount = 0;
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
        // iris-1201-experimental 实验支线：默认关=与 debug-1201-windows-gpu 暂停门行为逐分支等价
        // （同样 return false 回退 CPU 缓冲路径，零 GL 调用）。开关开=加固后直绘：
        // ① IrisVertexFormats.ENTITY 兼容扩展 VAO（GpuMesh.ensureIrisBuffers，属性 7/8/9 补齐）
        // ② compute 后追加 GL_SHADER_STORAGE|GL_BUFFER_UPDATE barrier
        // ③ shader.apply() 后显式 glUseProgram 重绑（对抗 ExtendedShader.lastApplied 跨 pass 缓存：
        //    ExtendedShader.java:138-141 同实例跳过 glUseProgram，而我方 compute 收尾 _glUseProgram(0)
        //    ——同 shader 连续画第 2+ 个实体时 draw 落在 program 0）
        // 真机 GO/NO-GO 由用户 Windows（AMD）回测裁决，llvmpipe 绿不作依据。
        if (!GeneralConfig.USE_GPU_IRIS_DIRECT.get()) {
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

        // 加固②：在 VERTEX_ATTRIB|ELEMENT 之外追加 SHADER_STORAGE|BUFFER_UPDATE barrier——
        // SSBO 写→VA 读理论只需 VERTEX_ATTRIB_ARRAY，但 AMD Adrenalin 对
        // SHADER_STORAGE→VERTEX_ATTRIB_ARRAY 可见性有实测缺口（debug-1201-windows-gpu 高发），
        // belt-and-braces 补两组屏障位（对正确驱动零语义差，纯多余 fence）
        GL43.glMemoryBarrier(GL43.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT | GL43.GL_ELEMENT_ARRAY_BARRIER_BIT
                | GL43.GL_SHADER_STORAGE_BARRIER_BIT | GL43.GL_BUFFER_UPDATE_BARRIER_BIT);

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

        // 加固③：显式重绑 program。ExtendedShader.apply() 走 lastApplied 缓存
        // （ExtendedShader.java:138-141 lastApplied==this 则跳过 glUseProgram），本帧已画过
        // 普通实体后再画 YSM 时（compute 收尾曾 _glUseProgram(0)），缓存命中会把 draw 留在
        // program 0。vanilla ShaderInstance.apply 无条件重绑（ShaderInstance.java:337），对
        // 非 Iris 包装 shader 此调用同样幂等安全。
        GL20.glUseProgram(shader.getId());

        // 加固①：切 Iris 兼容扩展 VAO（属性 0-5 同旧 xformVao，7/8/9 = iris_Entity/
        // mc_midTexCoord/at_tangent）。无包路径（GpuRenderPath）不受影响仍用 xformVao。
        mesh.ensureIrisBuffers();
        GlStateManager._glBindVertexArray(mesh.irisVao());

        int offsetBytes = mesh.indexOffsetBytes(renderPartMask);
        int drawCount = mesh.indexDrawCount(renderPartMask);
        if (drawCount > 0) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, drawCount, GL11.GL_UNSIGNED_INT, offsetBytes);
        }

        // 数值打点⑤：programId / draw framebuffer / glGetError / drawCount / VAO 属性启用掩码，
        // 限频=前 3 次 + 每 600 次 1 条（3+600 风格）。禁看图，纯数值。
        int glErr = GL11.glGetError();
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        long attrMask = 0;
        for (int ai = 0; ai <= 10; ai++) {
            if (GL20.glGetVertexAttribi(ai, GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED) != 0) attrMask |= 1L << ai;
        }
        int dbgN = ++irisDbgDrawCount;
        if (dbgN <= 3 || dbgN % 600 == 0) {
            YesSteveModel.LOGGER.info("[iris-exp] draw#{} programId={} fbo=0x{} glErr=0x{} drawCount={} attrMask=0x{}",
                    dbgN, shader.getId(), Integer.toHexString(drawFbo), Integer.toHexString(glErr), drawCount, Long.toHexString(attrMask));
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
