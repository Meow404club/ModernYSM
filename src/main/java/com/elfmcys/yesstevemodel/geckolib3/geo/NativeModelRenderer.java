

package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.MatrixBridge;
import com.elfmcys.yesstevemodel.util.log.ChatLogger;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <26 {
import net.minecraft.client.renderer.LightTexture;
//?}
// 26.x：LightTexture 删，pack 语义迁 LightCoordsUtil（26.1 LightCoordsUtil.java:8）
//? if >=26 {
/*import net.minecraft.util.LightCoordsUtil;
 *///?}
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rip.ysm.compat.oculus.OculusCompat;
import rip.ysm.compat.optifine.OptiFineDetector;
import rip.ysm.compat.realcamera.RealCameraCompat;
import rip.ysm.gpu.GpuCapability;
import rip.ysm.gpu.GpuRenderPath;
import rip.ysm.gpu.IrisRenderPath;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class NativeModelRenderer {
    private static final Matrix4f projectionModelViewMatrix = new Matrix4f();

    // fix-fpm-hide-path-matrix：三路径隐藏剔除数值矩阵打点开关。三通道任一启用：
    //   1) -Dysm.debug.hideMatrix=true  2) 环境变量 YSM_DEBUG_HIDE_MATRIX
    //   3) gameDir 下存在 ysm-debug-hide.flag 文件（与 GuiTourDriver harness.armed 同机制；
    //      矩阵 runner 实证 gradle→MDG run JVM 的 env 传递不稳定，文件通道可靠）。
    // 默认关=零开销；开=每 60 次渲染调用各路径各输出一行实测计数。路径归属由打点位置
    // 自证（CPU=renderModel 内 / SIMD=submitVertices 回调 / GPU=GpuRenderPath SSBO 回读），
    // 防止“外部判路径+回退链”再次造成归属误判（前轮伪影诊断教训）。
    private static volatile Boolean hideMatrixCache;

    static boolean debugHideMatrix() {
        Boolean b = hideMatrixCache;
        if (b == null) {
            boolean on = Boolean.getBoolean("ysm.debug.hideMatrix")
                    || System.getenv("YSM_DEBUG_HIDE_MATRIX") != null;
            if (!on) {
                try {
                    // java.io.File（Java 8 口径；Path.of 是 11+，1165 线编译实证）
                    on = new java.io.File("ysm-debug-hide.flag").exists();
                } catch (Throwable ignored) {
                }
            }
            b = on;
            hideMatrixCache = b;
        }
        return b;
    }

    private static long hideMatrixCallCounter = 0;
    // llvmpipe 矩阵轮实测：FPS 个位数时 600 次限频一轮（~50s 在世界）命中不了，降 60
    private static final long HIDE_MATRIX_LOG_INTERVAL = 60;
    // SIMD 统计暂存（nativeRenderModel 调用点填，submitVertices 回调读；渲染单线程）
    static int[] simdHiddenStats;

    public static boolean shouldLogHideMatrix() {
        if (!debugHideMatrix()) return false;
        return hideMatrixCallCounter++ % HIDE_MATRIX_LOG_INTERVAL == 0;
    }
    /**
     * fix-fpm-hide-path-matrix：SIMD/GPU 加速路径的隐藏旗标兜底。
     * 根因：offset9(HIDDEN) 只有 CPU 路径消费（renderModel/calculateBoneMatrix 的
     * hiddenFlag 判定），SIMD native（nComputeModelVertices，只读 offset10 做子树跳绘，
     * 隐藏骨自身几何照画）与 GPU compute 蒙皮（nComputeBoneMatrices 的
     * selfHidden=inheritedHidden||scale==0 不含 offset9 → bone_skin.vsh isHidden 不折叠）
     * 都丢掉了“隐藏骨自身几何”的剔除——AllHead 骨自身挂立方时即用户可见的 FPM 颈残留。
     * Java 侧兜底：存在 offset9!=0 的骨时，做一份补丁副本把该骨 scale 槽（offset+6/7/8）
     * 置 0——native 两条路径对 scale==0 均有“自身不输出顶点+子树跳过”的既有判定
     * （dllmain.cpp SIMD nComputeModelVertices 的 scale 早退分支 / GPU nComputeBoneMatrices
     * 的 selfHidden scale 判定），等价复现 CPU 路径 isVisible=false 语义。
     * 无旗标（无 FPM 常态）零拷贝原数组返回，逐路径零行为差。
     * 原生治本 patch（需重编 dll，另账返回）：nComputeModelVertices/nComputeBoneMatrices
     * 读 anim[pOffset+9] 并入各自 hidden 判定。
     */
    static float[] hiddenPatchedBoneParams(GeoModel model, float[] boneParams) {
        if (boneParams == null || model.bakedBones == null || model.bakedBones.isEmpty()) return boneParams;
        int boneCount = model.bakedBones.size();
        boolean anyHidden = false;
        for (int i = 0; i < boneCount; i++) {
            int p = i * 12;
            if (p + 9 >= boneParams.length) break;
            if (boneParams[p + 9] != 0.0f) {
                anyHidden = true;
                break;
            }
        }
        if (!anyHidden) return boneParams;
        float[] patched = boneParams.clone();
        for (int i = 0; i < boneCount; i++) {
            int p = i * 12;
            if (p + 9 >= patched.length) break;
            if (patched[p + 9] != 0.0f) {
                patched[p + 6] = 0.0f;
                patched[p + 7] = 0.0f;
                patched[p + 8] = 0.0f;
            }
        }
        return patched;
    }

    /**
     * 隐藏子树几何统计（debug 打点用，三路径同口径）：
     * 返回 [hiddenBones, hiddenCubes, hiddenQuads, totalQuads]。
     * hidden 口径=offset9!=0 骨沿 parentIdx 向上传染的子孙集合（与 CPU
     * visibleCache 承接同语义：父不可见则子不可见）。
     */
    static int[] hiddenSubtreeStats(GeoModel model, float[] boneParams) {
        if (model.bakedBones == null || boneParams == null) return null;
        int n = model.bakedBones.size();
        java.util.BitSet hiddenSelf = new java.util.BitSet(n);
        for (int i = 0; i < n; i++) {
            int p = i * 12;
            if (p + 9 >= boneParams.length) break;
            if (boneParams[p + 9] != 0.0f) hiddenSelf.set(i);
        }
        int[] stats = new int[4];
        for (int i = 0; i < n; i++) {
            boolean hidden = false;
            for (int j = i; j >= 0; j = model.bakedBones.get(j).parentIdx) {
                if (hiddenSelf.get(j)) {
                    hidden = true;
                    break;
                }
            }
            int quads = 0;
            for (GeoModel.BakedCube cube : model.bakedBones.get(i).cubes) quads += cube.quads.size();
            stats[3] += quads;
            if (!hidden) continue;
            stats[0]++;
            stats[1] += model.bakedBones.get(i).cubes.size();
            stats[2] += quads;
        }
        return stats;
    }

    public static String hideStatsLine(String path, GeoModel model, float[] boneParams, String extra) {
        int[] stats = hiddenSubtreeStats(model, boneParams);
        if (stats == null) return String.format("[ysm-hide-matrix] path=%s stats=unavailable %s", path, extra);
        return String.format("[ysm-hide-matrix] path=%s hiddenBones=%d hiddenCubes=%d hiddenQuads=%d totalQuads=%d %s",
                path, stats[0], stats[1], stats[2], stats[3], extra);
    }

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        renderMesh(buffer, pose, model, boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, null);
    }

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, net.minecraft.resources.ResourceLocation textureLocation) {
        OculusCompat.updatePBRState();
        MatrixBridge.projectionMatrix().mul(MatrixBridge.modelViewMatrix(), projectionModelViewMatrix);
        boolean isPreview = ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer();
        // fix-fpm-rc R3：RealCamera 绑定 GUI 打开期间强制 CPU 缓冲管线（原版管线顶点进
        // MultiVertexCatcher，GUI 才读得到 UV 可选）；GPU/SIMD 直写顶点不进 catcher。
        // 只影响 GUI 打开期间，关闭后恢复原路径（性能零损失面）。
        boolean rcBindGuiOpen = RealCameraCompat.isBindGuiOpen();

        if (textureLocation != null && !rcBindGuiOpen && NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get() && GeneralConfig.USE_GPU_RENDERER.get()) {

            if(!GpuCapability.isAvailable())
            {
                ChatLogger.INSTANCE.logFormatted("Disabled GPU renderer for: " + GpuCapability.getReason());
                GeneralConfig.USE_GPU_RENDERER.set(false);
                return;
            }

            // fix-fpm-hide-path-matrix：GPU 路径（Iris compute / GpuRenderPath）隐藏旗标兜底
            //（offset9→scale 补丁副本）。Iris 路径 1.20.1 现恒回退 CPU，喂副本零差；
            // 未来启用时 nComputeBoneMatricesLocal 的 hidden 判定同样需要该兜底。
            float[] gpuBoneParams = hiddenPatchedBoneParams(model, boneParams);
            if (OculusCompat.isShaderPackInUse() && !isPreview) {
                if (IrisRenderPath.tryRender(model, pose, gpuBoneParams, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            } else {
                if (GpuRenderPath.tryRender(model, pose, gpuBoneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            }
        }

        // debug-1201-windows-gpu: 带光影包（Iris 分支回退后落到此处）时不再走 SIMD native 直写——
        // Embeddium 改造的 BufferBuilder 下 native 直写渲染损坏（llvmpipe 实证：世界内模型巨大化+全黑），
        // 用户真机默认配置世界全黑亦与该链路相符。带光影包改走 CPU 缓冲路径（原版管线，Iris 兼容，
        // compat 渲染器同链已实证可见）；无光影包场景 SIMD 行为不变。
        boolean cpuBufferFallback = OculusCompat.isShaderPackInUse() || rcBindGuiOpen;
        if (NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get() && !cpuBufferFallback) { // WIP: SIMD MODEL RENDER
            // fix-fpm-hide-path-matrix：SIMD 路径隐藏旗标兜底（offset9→scale 补丁副本）
            float[] simdBoneParams = hiddenPatchedBoneParams(model, boneParams);
            if (debugHideMatrix()) simdHiddenStats = hiddenSubtreeStats(model, simdBoneParams);
            nativeRenderModel(
                    buffer,
                    pose,
                    projectionModelViewMatrix,
                    OptiFineDetector.isOptifinePresent(),
                    model,
                    simdBoneParams,
                    stateBuffer,
                    textureIndex,
                    renderPartMask,
                    packedLight,
                    packedOverlay,
                    red, green, blue, alpha,
                    isPreview
            );
        } else {
            renderModel(
                    buffer,
                    pose,
                    projectionModelViewMatrix,
                    OptiFineDetector.isOptifinePresent(),
                    model,
                    boneParams,
                    stateBuffer,
                    textureIndex,
                    renderPartMask,
                    packedLight,
                    packedOverlay,
                    red, green, blue, alpha,
                    isPreview
            );
        }
    }

    public static void renderModel(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            Matrix4f projectionModelViewMatrix,
            boolean isCompatMode,
            GeoModel mesh,
            float[] boneParams,
            float[] stateBuffer,
            int textureIndex, int renderPartMask,
            int packedLight, int packedOverlay,
            float r, float g, float b, float a,
            boolean isPreview) {

        if (mesh.bakedBones == null || mesh.bakedBones.isEmpty()) return;

        // TODO: 修復GC壓力
        // 1.16.5 由 MatrixBridge 做 moj→JOML 转置换算（保 native float[] 契约布局不变），1.20.1 直通
        Matrix4f rootPoseMat = MatrixBridge.pose(pose);
        Matrix3f rootNormalMC = MatrixBridge.normal(pose);
        Matrix4f projMat = MatrixBridge.projectionMatrix();

        Matrix4f identityMat = new Matrix4f();
        Matrix4f globalBoneMat = new Matrix4f();
        Matrix4f projBoneMat = new Matrix4f();
        Matrix3f localNormalMat = new Matrix3f();
        Matrix3f globalNormalMat = new Matrix3f();

        Vector4f p1 = new Vector4f();
        Vector4f p2 = new Vector4f();
        Vector4f p3 = new Vector4f();
        Vector4f tempPos = new Vector4f();
        Vector3f tempNorm = new Vector3f();
        Matrix4f[] boneLocalTransforms = new Matrix4f[mesh.bakedBones.size()];
        boolean[] boneVisible = new boolean[mesh.bakedBones.size()];

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            calculateBoneMatrix(i, mesh.bakedBones, boneParams, boneLocalTransforms, boneVisible, identityMat, stateBuffer);
        }

        // fix-fpm-hide-path-matrix：CPU 路径实测剔除计数（visibleCache 已填全，打点位置自证路径归属）
        // 注意：本方法参数表有 float b，统计块局部变量避开该名
        if (shouldLogHideMatrix()) {
            int visibleCubes = 0;
            int visibleQuads = 0;
            int hiddenInvisibleCubes = 0;
            int[] stats = hiddenSubtreeStats(mesh, boneParams);
            for (int i = 0; i < mesh.bakedBones.size(); i++) {
                GeoModel.BakedBone statBone = mesh.bakedBones.get(i);
                if (renderPartMask != 0 && statBone.partMask != renderPartMask && statBone.partMask != 3) continue;
                int quads = 0;
                for (GeoModel.BakedCube cube : statBone.cubes) quads += cube.quads.size();
                if (boneVisible[i]) {
                    visibleCubes += statBone.cubes.size();
                    visibleQuads += quads;
                } else if (stats != null && stats[0] > 0) {
                    // 该骨不可见：若在隐藏子树内则计入（offset9/10/scale0 任一判定命中都算）
                    for (int j = i; j >= 0; j = mesh.bakedBones.get(j).parentIdx) {
                        int p = j * 12;
                        if (p + 10 < boneParams.length
                                && (boneParams[p + 9] != 0.0f || boneParams[p + 10] != 0.0f
                                || boneParams[p + 6] == 0.0f || boneParams[p + 7] == 0.0f || boneParams[p + 8] == 0.0f)) {
                            hiddenInvisibleCubes += statBone.cubes.size();
                            break;
                        }
                    }
                }
            }
            System.out.printf("[ysm-hide-matrix] path=cpu hiddenBones=%d hiddenCubes=%d hiddenQuads=%d totalQuads=%d visibleCubes=%d visibleQuads=%d hideCulledCubes=%d%n",
                    stats == null ? -1 : stats[0], stats == null ? -1 : stats[1], stats == null ? -1 : stats[2],
                    stats == null ? -1 : stats[3], visibleCubes, visibleQuads, hiddenInvisibleCubes);
        }

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            if (!boneVisible[i]) {
                continue;
            }

            GeoModel.BakedBone bone = mesh.bakedBones.get(i);
            if (renderPartMask != 0 && bone.partMask != renderPartMask && bone.partMask != 3) {
                continue;
            }

            Matrix4f localBoneMat = boneLocalTransforms[i];
            globalBoneMat.set(rootPoseMat).mul(localBoneMat);
            projBoneMat.set(projMat).mul(globalBoneMat);

            // 法線全域矩陣
            localBoneMat.normal(localNormalMat);
            globalNormalMat.set(rootNormalMC).mul(localNormalMat);

            //? if <26
            int currentPackedLight = bone.glow ? LightTexture.pack(15, 15) : packedLight;
            //? if >=26
            /*int currentPackedLight = bone.glow ? LightCoordsUtil.pack(15, 15) : packedLight;*/

            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    if (cube.cullable) {
                        p1.set(quad.positions[0], quad.positions[1], quad.positions[2], 1.0f).mul(projBoneMat);
                        p2.set(quad.positions[3], quad.positions[4], quad.positions[5], 1.0f).mul(projBoneMat);
                        p3.set(quad.positions[6], quad.positions[7], quad.positions[8], 1.0f).mul(projBoneMat);
                        float det = p1.x() * (p2.y() * p3.w() - p3.y() * p2.w()) - p2.x() * (p1.y() * p3.w() - p3.y() * p1.w()) + p3.x() * (p1.y() * p2.w() - p2.y() * p1.w());
                        if (det <= 0.0f) {
                            continue;
                        }
                    }
                    tempNorm.set(quad.normal[0], quad.normal[1], quad.normal[2]).mul(globalNormalMat).normalize();
                    for (int v = 0; v < 4; v++) {
                        int positionOffset = v * 3;
                        int uvOffset = v * 2;
                        tempPos.set(quad.positions[positionOffset], quad.positions[positionOffset + 1], quad.positions[positionOffset + 2], 1.0f).mul(globalBoneMat);
                        // 1.21 十四参 vertex 拆为 addVertex(x,y,z,packedColor,u,v,overlay,light,nx,ny,nz)
                        //（vanilla-1.21.1 VertexConsumer.java:28 + ModelPart.java:362 用法实证）
                        // 1.21.2 FastColor 删除（ARGB 接管）：setColor(int) 按 ARGB 序解包
                        //（vanilla-1.21.3 VertexConsumer.java:50-52），1.21~1.21.2 的
                        // ABGR32.color(a,b,g,r) 位序与其不符系既有线自洽，1.21.2+ 统一 ARGB.color(a,r,g,b)
                        //? if >=1.21.2
                        /*vertexConsumer.addVertex(tempPos.x(), tempPos.y(), tempPos.z(), net.minecraft.util.ARGB.color(net.minecraft.util.ARGB.as8BitChannel(a), net.minecraft.util.ARGB.as8BitChannel(r), net.minecraft.util.ARGB.as8BitChannel(g), net.minecraft.util.ARGB.as8BitChannel(b)), quad.uvs[uvOffset], quad.uvs[uvOffset + 1], packedOverlay, currentPackedLight, tempNorm.x(), tempNorm.y(), tempNorm.z());*/
                        //? if >=1.21 && <1.21.2
                        /*vertexConsumer.addVertex(tempPos.x(), tempPos.y(), tempPos.z(), net.minecraft.util.FastColor.ABGR32.color(net.minecraft.util.FastColor.as8BitChannel(a), net.minecraft.util.FastColor.as8BitChannel(b), net.minecraft.util.FastColor.as8BitChannel(g), net.minecraft.util.FastColor.as8BitChannel(r)), quad.uvs[uvOffset], quad.uvs[uvOffset + 1], packedOverlay, currentPackedLight, tempNorm.x(), tempNorm.y(), tempNorm.z());*/
                        //? if <1.21
                        vertexConsumer.vertex(tempPos.x(), tempPos.y(), tempPos.z(), r, g, b, a, quad.uvs[uvOffset], quad.uvs[uvOffset + 1], packedOverlay, currentPackedLight, tempNorm.x(), tempNorm.y(), tempNorm.z());
                    }
                }
            }
        }
    }

    private static Matrix4f calculateBoneMatrix(int idx, java.util.List<GeoModel.BakedBone> bones, float[] boneParams, Matrix4f[] cache, boolean[] visibleCache, Matrix4f rootPose, float[] stateBuffer) {
        if (cache[idx] != null) return cache[idx];

        GeoModel.BakedBone bone = bones.get(idx);
        Matrix4f parentMatrix = rootPose;
        boolean isVisible = true;

        if (bone.parentIdx != -1) {
            parentMatrix = calculateBoneMatrix(bone.parentIdx, bones, boneParams, cache, visibleCache, rootPose, stateBuffer);
            // 如果父骨骼不可見，子骨骼必然跟著不可見
            if (!visibleCache[bone.parentIdx]) {
                isVisible = false;
            }
        }

        Matrix4f localMat = new Matrix4f(parentMatrix);

        int pOffset = idx * 12;
        float animRx = boneParams[pOffset];
        float animRy = boneParams[pOffset + 1];
        float animRz = boneParams[pOffset + 2];
        float animTx = boneParams[pOffset + 3];
        float animTy = boneParams[pOffset + 4];
        float animTz = boneParams[pOffset + 5];
        float animSx = boneParams[pOffset + 6];
        float animSy = boneParams[pOffset + 7];
        float animSz = boneParams[pOffset + 8];

        float hiddenFlag = boneParams[pOffset + 9];
        float skipChildrenFlag = boneParams[pOffset + 10];
        float trackFlag = boneParams[pOffset + 11];

        // fix-fpm-rc F1: 恢复 offset9/10 隐藏旗标消费（原被注释），镜像 native 语义
        //（native/openysm-cpp/dllmain.cpp:667 offset10!=0 子树跳绘；:1249-1250
        // inheritedHidden 传播 + scale 归零 OR 判定）。父骨不可见的传播由下方
        // visibleCache[parentIdx] 承接；Trissy AllHead.setHidden(z,z) 同置 offset9/10，
        // 与 native 路径两态对齐（诊断账 F3）。
        if (animSx == 0.0f || animSy == 0.0f || animSz == 0.0f) {
            isVisible = false;
        } else if (hiddenFlag != 0.0f || skipChildrenFlag != 0.0f) {
            isVisible = false;
        }

        localMat.translate(
                (bone.pivotX - animTx) * 0.0625f,
                (bone.pivotY + animTy) * 0.0625f,
                (bone.pivotZ + animTz) * 0.0625f
        );
        localMat.rotateZ(animRz);
        localMat.rotateY(animRy);
        localMat.rotateX(animRx);

//        if (bone.name.equals("gun")) {
//            //"".hashCode();
//        }

        if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
            localMat.scale(animSx, animSy, animSz);
        }

        // fix-fpm-rc F1: stateBuffer（viewLocator 跟踪位）不再受 isVisible 门控——native 侧
        //（dllmain.cpp:725）无隐藏守卫，先写 state 后做子树跳绘，语义对齐
        if (trackFlag == 1.0F && stateBuffer != null) {
            int offset = idx * 4;
            // bone pivot abs
            if (offset + 2 < stateBuffer.length) {
                stateBuffer[offset + 0] =-localMat.m30() * 16;
                stateBuffer[offset + 1] = localMat.m31() * 16;
                stateBuffer[offset + 2] = localMat.m32() * 16;
            }
        }

        localMat.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f);

        cache[idx] = localMat;
        visibleCache[idx] = isVisible; // 保存當前骨骼的可見性
        return localMat;
    }

    private static final float[] matrixTransferArray = new float[48];
    @SuppressWarnings("unused") // TODO: native中直接往VertexConsumer中的buffer写入顶点
    public static void submitVertices(Object v, int vertexCount, ByteBuffer fBuf, ByteBuffer iBuf) {
        // fix-fpm-hide-matrix：SIMD 路径实测输出打点（submitVertices=native 唯一回吐口，位置自证路径归属）
        if (shouldLogHideMatrix() && simdHiddenStats != null) {
            System.out.printf("[ysm-hide-matrix] path=simd hiddenBones=%d hiddenCubes=%d hiddenQuads=%d totalQuads=%d outputQuads=%d%n",
                    simdHiddenStats[0], simdHiddenStats[1], simdHiddenStats[2], simdHiddenStats[3], vertexCount / 4);
        }
        FloatBuffer f = fBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
        IntBuffer in = iBuf.order(ByteOrder.nativeOrder()).asIntBuffer();
        VertexConsumer vc = (VertexConsumer) v;
        int fIdx = 0, iIdx = 0;
        for (int n = 0; n < vertexCount; n++) {
            // 1.21.2 FastColor 删除（ARGB 接管）；缓冲布局 f[idx+3..6]=r,g,b,a（见下方 <1.21
            // 分支参数序），packedColor 统一 ARGB 序（同上 addVertex 点注释）
            //? if >=1.21.2 {
            /*vc.addVertex(
                    f.get(fIdx),     f.get(fIdx + 1), f.get(fIdx + 2),
                    net.minecraft.util.ARGB.color(net.minecraft.util.ARGB.as8BitChannel(f.get(fIdx + 6)), net.minecraft.util.ARGB.as8BitChannel(f.get(fIdx + 3)), net.minecraft.util.ARGB.as8BitChannel(f.get(fIdx + 4)), net.minecraft.util.ARGB.as8BitChannel(f.get(fIdx + 5))),
                    f.get(fIdx + 7), f.get(fIdx + 8),
                    in.get(iIdx),    in.get(iIdx + 1),
                    f.get(fIdx + 9), f.get(fIdx + 10), f.get(fIdx + 11)
            );*/
            //?}
            //? if >=1.21 && <1.21.2 {
            /*vc.addVertex(
                    f.get(fIdx),     f.get(fIdx + 1), f.get(fIdx + 2),
                    net.minecraft.util.FastColor.ABGR32.color(net.minecraft.util.FastColor.as8BitChannel(f.get(fIdx + 6)), net.minecraft.util.FastColor.as8BitChannel(f.get(fIdx + 5)), net.minecraft.util.FastColor.as8BitChannel(f.get(fIdx + 4)), net.minecraft.util.FastColor.as8BitChannel(f.get(fIdx + 3))),
                    f.get(fIdx + 7), f.get(fIdx + 8),
                    in.get(iIdx),    in.get(iIdx + 1),
                    f.get(fIdx + 9), f.get(fIdx + 10), f.get(fIdx + 11)
            );*/
            //?}
            //? if <1.21 {
            vc.vertex(
                    f.get(fIdx),     f.get(fIdx + 1), f.get(fIdx + 2),
                    f.get(fIdx + 3), f.get(fIdx + 4), f.get(fIdx + 5), f.get(fIdx + 6),
                    f.get(fIdx + 7), f.get(fIdx + 8),
                    in.get(iIdx),    in.get(iIdx + 1),
                    f.get(fIdx + 9), f.get(fIdx + 10), f.get(fIdx + 11)
            );
            //?}
            fIdx += 12;
            iIdx += 2;
        }
    }


    public static void nativeRenderModel( // TODO:
            VertexConsumer vertexConsumer, PoseStack.Pose pose, Matrix4f projectionModelViewMatrix,
            boolean isCompatMode, GeoModel mesh, float[] boneVertex, float[] stateBuffer,
            int textureIndex, int renderPartMask, int packedLight, int packedOverlay,
            float r, float g, float b, float a, boolean isPreview) {

        if (mesh.nativeModelHandle == 0) return;

        Matrix4f projMat = MatrixBridge.projectionMatrix();

        MatrixBridge.pose(pose).get(matrixTransferArray, 0);
        MatrixBridge.normal(pose).get(matrixTransferArray, 16);
        projMat.get(matrixTransferArray, 32);

        GeoModel.nComputeModelVertices(
                mesh.nativeModelHandle,
                vertexConsumer,
                matrixTransferArray,
                boneVertex,
                stateBuffer,
                renderPartMask,
                packedLight, packedOverlay,
                r, g, b, a
        );
    }
}
