package rip.ysm.gpu;

import rip.ysm.util.RenderCompat;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.mixin.client.RenderSystemAccessor;
// 1.21.5 GlStateManager 迁移 platform→opengl 包（vcs 直通铁律：非 1.20.1 分支源码态必须注释）
//? if <21.5
import com.mojang.blaze3d.platform.GlStateManager;
// 26.3 renderpearl 包迁移：blaze3d.opengl.* 整包搬 com.mojang.renderpearl.backend.opengl
//（/tmp/vanilla-263 com/mojang/renderpearl/backend/opengl 实证，GlStateManager 方法面同名同参）
//? if >=21.5 && <26.3
/*import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;*/
//? if >=26.3
/*import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.GlTexture;*/
// 21.8 帧图：实体/手部分派发生在 RenderPass 执行窗口外（探针实证 drawFbo=0）→
// 绘制期需显式绑主目标 FBO（GlTexture.getFbo 缓存命中 vanilla 自建 id）。仅 >=21.8 消费。
// 注意 26.3 RenderTarget 不随 renderpearl 迁移（/tmp/vanilla-263 blaze3d/pipeline/RenderTarget.java 在）
//? if >=21.8 && <26.3
/*import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.pipeline.RenderTarget;*/
//? if >=26.3
/*import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import com.mojang.blaze3d.pipeline.RenderTarget;*/
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GpuRenderPath {
    private static final float[] rootPoseScratch = new float[16];
    private static final float[] rootNormalScratch = new float[9];
    private static final float[] projScratch = new float[16];
    private static final Matrix4f projMVScratch = new Matrix4f();
    private static final Vector3f[] currentLights = new Vector3f[2];
    private static final ConcurrentHashMap<Long, GpuMesh> meshMap = new ConcurrentHashMap<>();
    private static final AtomicLong ref = new AtomicLong(1);
    private static final Matrix4f pivotAbsScratchMat = new Matrix4f();
    private static int[] pivotAbsPathScratch = new int[64];

    // 21.8+ FBO 自绑 DSA 工厂静态化（审查 ponytail 备忘：tryRender 逐帧 HashSet+create 分配）。
    // create=纯 caps 选择函数（1218 DirectStateAccess.java:16-23 / 21111:18-25：按 caps 选
    // Core/Emulated，Set 仅收 marker 字符串、getFbo 链零消费），实例不持上下文状态，GL 调用
    // 全部在调用时刻打当前上下文——缓存安全。失效=上下文重建（LWJGL 换新 GLCapabilities
    // 实例）或 21.9+ device 换代（isGlOnDx12 语义入参），按实例身份不等即重建，与逐次
    // create 逐点等价（同 caps/device → 同 Core/Emulated 选择）。
    //? if >=21.8 && <26.2 {
    /*private static GLCapabilities ysmDsaCaps;
    private static DirectStateAccess ysmDsa;
    private static final java.util.Set<String> ysmDsaExtensions = new java.util.HashSet<>();
    private static Object ysmDsaDevice;*/
    //?}

    public static boolean tryRender(
            GeoModel model,
            PoseStack.Pose pose,
            float[] boneParams,
            float[] stateBuffer,
            int textureIndex,
            int renderPartMask,
            int packedLight,
            int packedOverlay,
            float r, float g, float b, float a,
            ResourceLocation textureLocation
    ) {
        if (!GpuCapability.isAvailable()) return false;
        // 1.21.8+ 复活闸门在 GpuCapability.isAvailable（CPU 投影/雾捕获就绪即放行，见其类内注记）
        //? if <1.17 {
        /*// 1.16.5 恒 false 闸门（照 gui-hud 卡 IrisRenderPath 同款降级）：本方法依赖
        // RenderSystem.getProjectionMatrix/getModelViewMatrix/getShaderTexture/getShaderFog*、
        // BufferUploader.invalidate 等 1.17+ shader 管线 API（1.16.5 jar javap 实证缺席），
        // GL43 compute 路径整体降级走 CPU 渲染。功能差已记 tasks.feature-debts-1165。
        return false;
         *///?} else {
        if (!BoneSkinShader.ensureCompiled()) return false;
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return false;

        if (model.gpuMeshHandle == 0) {
            GpuMesh mesh = GpuMeshBuilder.build(model);
            if (mesh == null) return false;
            model.gpuMeshHandle = encodeMeshRef(mesh);
        }
        GpuMesh mesh = decodeMeshRef(model.gpuMeshHandle);
        if (mesh == null) return false;

        //? if >=1.17 && <1.19.3 {
        /*
        Matrix4f rootPose = com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.pose(pose);
        Matrix3f rootNormal = com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.normal(pose);
        Matrix4f projMat = com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.projectionMatrix();
        Matrix4f mvMat = com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge.modelViewMatrix();
         *///?}
        //? if >=1.19.3 && <21.6 {
        Matrix4f rootPose = pose.pose();
        Matrix3f rootNormal = pose.normal();
        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        Matrix4f mvMat = RenderSystem.getModelViewMatrix();
        //?}
        // 21.6+ GpuCapability 门放行后（21.8/21.11，捕获 mixin 已喂入）走真实矩阵：
        // 投影=GpuCapability 捕获面（vanilla GameRenderer:671(1218)/:771(2111) 打包点镜像），
        // modelview=RenderSystem.getModelViewStack（1218 LevelRenderer:445-447/21111 :512-514
        // 仍 push 相机旋转，语义同旧线 getModelViewMatrix）。21.6/21.7 门恒关此分支不可达。
        // 预览态用 GUI 正交槽：21.8 延迟 GUI 管线 Screen.render 时刻当前捕获=hud3d 透视
        // （far=100 会把 z=1250 预览整裁），GUI 正交=上一帧值（尺寸恒定），对齐旧线 GUI 语义
        //? if >=21.6 && <26.2 {
        /*Matrix4f rootPose = pose.pose();
        Matrix3f rootNormal = pose.normal();
        Matrix4f projMat = (ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer())
                && GpuCapability.ysmGuiProjectionReady()
                ? GpuCapability.ysmCapturedGuiProjection : GpuCapability.ysmCapturedProjection;
        Matrix4f mvMat = RenderSystem.getModelViewMatrix();*/
        //?}
        // 26.2：RenderSystem.getModelViewMatrix 删 → getModelViewMatrixCopy（RenderSystem.java:204）
        //? if >=26.2 {
        /*Matrix4f rootPose = pose.pose();
        Matrix3f rootNormal = pose.normal();
        Matrix4f projMat = (ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer())
                && GpuCapability.ysmGuiProjectionReady()
                ? GpuCapability.ysmCapturedGuiProjection : GpuCapability.ysmCapturedProjection;
        Matrix4f mvMat = RenderSystem.getModelViewMatrixCopy();*/
        //?}

        rootPose.get(rootPoseScratch);
        rootNormal.get(rootNormalScratch);
        projMat.mul(mvMat, projMVScratch);
        projMVScratch.get(projScratch);

        ByteBuffer boneBuf = mesh.perFrameBoneBuffer;
        boneBuf.clear();

        updatePivotAbsStateBuffer(model, boneParams, stateBuffer);

        GeoModel.nComputeBoneMatrices(mesh.pointer, rootPoseScratch, rootNormalScratch, boneParams, packedLight, boneBuf);
        boneBuf.position(0);
        boneBuf.limit(mesh.boneCount * 144);

        //? if <21.5 {
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        //?}
        // 1.21.5 状态面进 RenderPipeline，RenderSystem 无静态入口 → GlStateManager._*（opengl 包）。
        // 注意：else+存储态不展开（21.5 生成树实证），一律拆兄弟 if 块
        //? if >=21.5 && <26.2 {
        /*GlStateManager._disableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._disableBlend();*/
        //?}
        // 26.2 blend 开关改 index 形（opengl/GlStateManager.java:84 _disableBlend(int)）
        //? if >=26.2 {
        /*GlStateManager._disableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._disableBlend(0);*/
        //?}

        Minecraft mc = Minecraft.getInstance();
        AbstractTexture modelTex = mc.getTextureManager().getTexture(textureLocation);
        //? if <21.5
        int modelTexId = modelTex.getId();
        // 1.21.5 AbstractTexture.getId 删 → getTexture()(GpuTexture) 具体类 GlTexture.glId()
        //（neoforge-21.5.98-sources AbstractTexture.java:51 / GlTexture.java:82）
        //? if >=21.5 && <21.8
        /*int modelTexId = ((GlTexture) modelTex.getTexture()).glId();*/
        //? if >=21.8
        /*int modelTexId = ((GlTexture) modelTex.getTexture()).glId();*/

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + 2);
        // 1.21.11 LightTexture.turnOnLightLayer 删（2111 LightTexture 方法面实证）→ no-op
        //? if <21.11
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + 1);
        // 1.21.11 OverlayTexture 移 texture 包且 setupOverlayColor 删（2111 方法面实证）→ no-op
        //? if <21.11
        mc.gameRenderer.overlayTexture().setupOverlayColor();
        // 1.21.5 getShaderTexture 返回 GpuTexture（RenderSystem.java:306）
        //? if <21.5
        GlStateManager._bindTexture(RenderSystem.getShaderTexture(1)); // overlayTexture里的texture没getter，固定bind 1
        //? if >=21.5 && <21.6
        /*GlStateManager._bindTexture(((GlTexture) RenderSystem.getShaderTexture(1)).glId());*/
        // 1.21.6 getShaderTexture 返回 GpuTextureView（21.6 RenderSystem.java:272，无 glId）且路径已降级：跳过冗余绑定
        //? if >=21.6 && <21.11 {
        /*GlStateManager._activeTexture(GL13.GL_TEXTURE0 + 1);
        mc.gameRenderer.overlayTexture().setupOverlayColor();*/
        //?}

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        GlStateManager._bindTexture(modelTexId);

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mesh.boneSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, boneBuf);
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, BoneSkinShader.ssbo, mesh.boneSsbo);

        // 1.21.2 fog 状态打包 FogParameters record（getShaderFogStart/End/Color/Shape 删除，
        // vanilla-1.21.3 RenderSystem.java:348 getShaderFog()）
        //? if >=1.21.2 && <21.6 {
        /*
        net.minecraft.client.renderer.FogParameters ysmFogParams = RenderSystem.getShaderFog();
        float fogStart = ysmFogParams.start();
        float fogEnd = ysmFogParams.end();
        float[] fogColor = new float[] { ysmFogParams.red(), ysmFogParams.green(), ysmFogParams.blue(), ysmFogParams.alpha() };
        int fogShape = ysmFogParams.shape().getIndex();
        */
        //?}
        // 21.6 fog 改 GpuBufferSlice（21.6 RenderSystem.java:170）→ CPU 雾值由捕获面喂入
        //（vanilla FogRenderer.setupFog:166(1218)/:162(21111) CPU 全参→updateBuffer:213/:205 打包，
        // 捕获 mixin 镜像；envStart/End=旧 FogParameters.start/end 语义槽位；fogShape 已随
        // FogParameters 代删除 → 0=sphere（大气雾旧默认）。21.6/21.7 门恒关此分支不可达）
        //? if >=21.6 {
        /*float fogStart = GpuCapability.ysmCapturedFogStart;
        float fogEnd = GpuCapability.ysmCapturedFogEnd;
        float[] fogColor = GpuCapability.ysmCapturedFogColor;
        int fogShape = 0;
        // GUI 预览时刻 21.8 vanilla 雾=NONE（GuiRenderer 阶段 emptyBuffer，updateBuffer 不覆写
        // → 捕获面残留世界雾），预览几何在千米级视距会被整只雾掉=对现状 CPU 预览回归；
        // 对齐旧线 GUI 语义=setupNoFog（1.20.1 FogRenderer.java:194-195 setShaderFogStart(MAX)）
        if (ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer()) {
            fogStart = Float.MAX_VALUE;
        }*/
        //?}
        //? if <1.21.2 {
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        float[] fogColor = RenderSystem.getShaderFogColor();
        // 1171 无 RenderSystem.getShaderFogShape（1171 编译实证）→ 常量 0=FogShape.SPHERE（pre-1.18 恒球 fog 语义）
        //? if <1.17
        /*int fogShape = RenderSystem.getShaderFogShape().getIndex();*/
        //? if >=1.17 && <1.18.2
        /*int fogShape = 0;*/
        //? if >=1.18.2 && <1.21.2
        int fogShape = RenderSystem.getShaderFogShape().getIndex();
        //?}

        // 21.8 帧图：实体/手部分派发生在 RenderPass 执行窗口之外（探针实证全量 drawFbo=0），
        // 立即直绘落在默认帧缓冲=被末帧合成覆盖。绘制期显式绑主目标 FBO（getFbo 缓存命中
        // vanilla 自建 id，DirectStateAccess 实参仅作 factory，命中缓存不触发改实现）；
        // 21.8- vanilla 每 pass 自绑 FBO，无需恢复。21.6/21.7 门恒关此分支不可达。
        // create 三参化（GraphicsWorkarounds）21.9 起（21.9/26.1 DirectStateAccess.java:18
        // 编译实证；21.8 仍两参。get(GpuDevice) 公共实例工厂）
        //? if >=21.8 && <21.9 {
        /*RenderTarget ysmMain = mc.getMainRenderTarget();
        if (ysmDsa == null || GL.getCapabilities() != ysmDsaCaps) {
            ysmDsaCaps = GL.getCapabilities();
            ysmDsa = DirectStateAccess.create(ysmDsaCaps, ysmDsaExtensions);
        }
        int ysmMainFbo = ((GlTexture) ysmMain.getColorTexture()).getFbo(ysmDsa, ysmMain.getDepthTexture());
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, ysmMainFbo);
        GlStateManager._viewport(0, 0, ysmMain.width, ysmMain.height);*/
        //?}
        //? if >=21.9 && <26.2 {
        /*RenderTarget ysmMain = mc.getMainRenderTarget();
        Object ysmDeviceNow = RenderSystem.getDevice();
        if (ysmDsa == null || GL.getCapabilities() != ysmDsaCaps || ysmDeviceNow != ysmDsaDevice) {
            ysmDsaCaps = GL.getCapabilities();
            ysmDsaDevice = ysmDeviceNow;
            ysmDsa = DirectStateAccess.create(ysmDsaCaps, ysmDsaExtensions,
                    com.mojang.blaze3d.GraphicsWorkarounds.get(RenderSystem.getDevice()));
        }
        int ysmMainFbo = ((GlTexture) ysmMain.getColorTexture()).getFbo(ysmDsa, ysmMain.getDepthTexture());
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, ysmMainFbo);
        GlStateManager._viewport(0, 0, ysmMain.width, ysmMain.height);*/
        //?}
        // 26.2：GraphicsWorkarounds 删 → GlHeuristics 无公开获取口（构造器包私有）→
        // heuristics 传 null（GPU 路径 26.x 门控关闭，仅编译面）；
        // getMainRenderTarget → mc.gameRenderer.mainRenderTarget()（Minecraft.java:673 同款）
        //? if >=26.2 {
        /*RenderTarget ysmMain = mc.gameRenderer.mainRenderTarget();
        // 26.2 FBO 获取口内化（GlDevice 包私有+frameBufferCache 内化，FrameBufferCache.getFbo
        // 仅 opengl 包可达）→ 绘制期自绑不可达，placeholder 0（GPU 路径 26.x 门控关闭，
        // 功能债归 native/GPU 卡池）
        int ysmMainFbo = 0;
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, ysmMainFbo);
        GlStateManager._viewport(0, 0, ysmMain.width, ysmMain.height);*/
        //?}

        GlStateManager._glUseProgram(BoneSkinShader.program());
        if (BoneSkinShader.locProj() >= 0) GL20.glUniformMatrix4fv(BoneSkinShader.locProj(), false, projScratch);
        if (BoneSkinShader.locColor() >= 0) GL20.glUniform4f(BoneSkinShader.locColor(), r, g, b, a);
        if (BoneSkinShader.locOverlay() >= 0) GL20.glUniform1i(BoneSkinShader.locOverlay(), packedOverlay);
        if (BoneSkinShader.locFogStart() >= 0) GL20.glUniform1f(BoneSkinShader.locFogStart(), fogStart);
        if (BoneSkinShader.locFogEnd() >= 0) GL20.glUniform1f(BoneSkinShader.locFogEnd(), fogEnd);

        if (BoneSkinShader.locFogColor() >= 0)
            GL20.glUniform4f(BoneSkinShader.locFogColor(), fogColor[0], fogColor[1], fogColor[2], fogColor[3]);

        if (BoneSkinShader.locFogShape() >= 0) GL20.glUniform1i(BoneSkinShader.locFogShape(), fogShape);

        refreshLights();

        if (BoneSkinShader.locLight0() >= 0)
            GL20.glUniform3f(BoneSkinShader.locLight0(), currentLights[0].x, currentLights[0].y, currentLights[0].z);
        if (BoneSkinShader.locLight1() >= 0)
            GL20.glUniform3f(BoneSkinShader.locLight1(), currentLights[1].x, currentLights[1].y, currentLights[1].z);

        GlStateManager._glBindVertexArray(mesh.vao);

        int offsetBytes = mesh.indexOffsetBytes(renderPartMask);
        int drawCount = mesh.indexDrawCount(renderPartMask);
        if (drawCount > 0) {
            if (BoneSkinShader.locAlphaMode() >= 0) GL20.glUniform1i(BoneSkinShader.locAlphaMode(), 1);
            GL11.glDrawElements(GL11.GL_TRIANGLES, drawCount, GL11.GL_UNSIGNED_INT, offsetBytes);

            if (model.isTranslucentTexture(textureIndex)) {
                //? if <21.5 {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                //?}
                //? if >=21.5 && <26.2 {
                /*GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);*/
                //?}
                //? if >=26.2 {
                /*GlStateManager._enableBlend(0);
                GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);*/
                //?}
                if (BoneSkinShader.locAlphaMode() >= 0) GL20.glUniform1i(BoneSkinShader.locAlphaMode(), 2);
                GL11.glDrawElements(GL11.GL_TRIANGLES, drawCount, GL11.GL_UNSIGNED_INT, offsetBytes);
                //? if <21.5
                RenderSystem.disableBlend();
                //? if >=21.5 && <26.2
                /*GlStateManager._disableBlend();*/
                //? if >=26.2
                /*GlStateManager._disableBlend(0);*/
            }
        }

        // fix-fpm-hide-path-matrix：GPU 路径实测剔除打点——draw 后 SSBO 仍在绑定时回读每骨
        // BoneDataOut.isHidden（stride 144，字节偏移 128），数 GPU 侧真实被折叠的骨数。
        // 打点位置自证路径归属（GpuRenderPath 内=GPU compute 蒙皮真实生效），debug 开关默认关。
        if (com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer.shouldLogHideMatrix()) {
            java.nio.ByteBuffer ssboRead = java.nio.ByteBuffer.allocateDirect(mesh.boneCount * 144).order(java.nio.ByteOrder.nativeOrder());
            GL15.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, ssboRead);
            int ssboHiddenBones = 0;
            // BoneDataOut 布局：transform[16](64B)+normal[16](64B)+packedLight(128)+isHidden(132)+pad(136..140)
            for (int i = 0; i < mesh.boneCount; i++) {
                if (ssboRead.getInt(i * 144 + 132) != 0) ssboHiddenBones++;
            }
            String line = com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer.hideStatsLine(
                    "gpu", model, boneParams, String.format("ssboHiddenBones=%d drawCount=%d", ssboHiddenBones, drawCount));
            System.out.println(line);
        }

        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, BoneSkinShader.ssbo, 0);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        GlStateManager._glUseProgram(0);

        //? if >=1.19.2 && <21.5

        RenderCompat.invalidate();
        //? if <1.19.2
        /*RenderCompat.reset();*/
        GlStateManager._glBindVertexArray(0);

        // 1.21.11 turnOffLightLayer removed (same note as turnOnLightLayer) -> no-op
        //? if <21.11
        mc.gameRenderer.lightTexture().turnOffLightLayer();

        return true;
        //?}
    }

    private static void refreshLights() {
        //? if <1.17 {
        /*Vector3f[] arr = RenderSystemAccessor.ysm$getShaderLightDirections();
         *///?}
        // 1.17~1.19.2 accessor 为 mojang Vector3f 签名（JOML 前夜）不可读，恒 null → 兜底默认平行光
        //? if >=1.17 && <1.19.3 {
        /*Vector3f[] arr = null;
         *///?}
        //? if >=1.19.3 && <21.6 {
        Vector3f[] arr = RenderSystemAccessor.ysm$getShaderLightDirections();
        //?}
        // 1.21.6+ RenderSystem.shaderLightDirections 改 GpuBufferSlice 类型（2108 RenderSystem.java:78）
        // → Vector3f[] accessor 不可读，走默认平行光兜底（功能债入账）
        //? if >=21.6 {
        /*Vector3f[] arr = null;
         *///?}
        currentLights[0] = (arr != null && arr.length > 0 && arr[0] != null) ? arr[0] : new Vector3f(0.2f, 1.0f, -0.7f).normalize();
        currentLights[1] = (arr != null && arr.length > 1 && arr[1] != null) ? arr[1] : new Vector3f(-0.2f, 1.0f, 0.7f).normalize();
    }

    public static void disposeMesh(GeoModel model) {
        if (model.gpuMeshHandle == 0) return;
        GpuMesh mesh = meshMap.remove(model.gpuMeshHandle);
        if (mesh != null) mesh.dispose();
        model.gpuMeshHandle = 0;
    }

    public static GpuMesh getOrBuildMesh(GeoModel model) {
        if (model.gpuMeshHandle == 0) {
            GpuMesh mesh = GpuMeshBuilder.build(model);
            if (mesh == null) return null;
            model.gpuMeshHandle = encodeMeshRef(mesh);
        }
        return decodeMeshRef(model.gpuMeshHandle);
    }

    private static long encodeMeshRef(GpuMesh mesh) {
        long ref = GpuRenderPath.ref.getAndIncrement();
        meshMap.put(ref, mesh);
        return ref;
    }

    private static GpuMesh decodeMeshRef(long ref) {
        return meshMap.get(ref);
    }

    private static void updatePivotAbsStateBuffer(GeoModel model, float[] boneParams, float[] stateBuffer) {
        if (stateBuffer == null || boneParams == null) return;
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return;

        int boneCount = model.bakedBones.size();

        for (int i = 0; i < boneCount; i++) {
            int pOffset = i * 12;
            if (pOffset + 11 >= boneParams.length) break;

            float unk3 = boneParams[pOffset + 11];
            if (unk3 != 1.0f) continue;

            int sOffset = i * 4;
            if (sOffset + 2 >= stateBuffer.length) continue;

            computeOnePivotAbs(i, model.bakedBones, boneParams, stateBuffer, sOffset);
        }
    }

    private static void computeOnePivotAbs(int targetIdx, List<GeoModel.BakedBone> bones, float[] boneParams, float[] stateBuffer, int stateOffset) {
        int depth = 0;
        int idx = targetIdx;

        while (idx != -1) {
            if (depth >= pivotAbsPathScratch.length) {
                int[] newPath = new int[pivotAbsPathScratch.length * 2];
                System.arraycopy(pivotAbsPathScratch, 0, newPath, 0, pivotAbsPathScratch.length);
                pivotAbsPathScratch = newPath;
            }

            pivotAbsPathScratch[depth++] = idx;
            idx = bones.get(idx).parentIdx;
        }

        Matrix4f localMat = pivotAbsScratchMat.identity();
        boolean isVisible = true;

        for (int p = depth - 1; p >= 0; p--) {
            int boneIdx = pivotAbsPathScratch[p];
            GeoModel.BakedBone bone = bones.get(boneIdx);

            int pOffset = boneIdx * 12;
            if (pOffset + 11 >= boneParams.length) return;

            float animRx = boneParams[pOffset];
            float animRy = boneParams[pOffset + 1];
            float animRz = boneParams[pOffset + 2];
            float animTx = boneParams[pOffset + 3];
            float animTy = boneParams[pOffset + 4];
            float animTz = boneParams[pOffset + 5];
            float animSx = boneParams[pOffset + 6];
            float animSy = boneParams[pOffset + 7];
            float animSz = boneParams[pOffset + 8];

            if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) {
                isVisible = false;
            }

            if (!isVisible) {
                return;
            }

            localMat.translate((bone.pivotX - animTx) * 0.0625f, (bone.pivotY + animTy) * 0.0625f, (bone.pivotZ + animTz) * 0.0625f);

            localMat.rotateZ(animRz);
            localMat.rotateY(animRy);
            localMat.rotateX(animRx);

            if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
                localMat.scale(animSx, animSy, animSz);
            }

            if (boneIdx == targetIdx) {
                stateBuffer[stateOffset] = -localMat.m30() * 16.0f;
                stateBuffer[stateOffset + 1] = localMat.m31() * 16.0f;
                stateBuffer[stateOffset + 2] = localMat.m32() * 16.0f;
                return;
            }

            localMat.translate(-bone.pivotX / 16.0f, -bone.pivotY / 16.0f, -bone.pivotZ / 16.0f);
        }
    }
}
