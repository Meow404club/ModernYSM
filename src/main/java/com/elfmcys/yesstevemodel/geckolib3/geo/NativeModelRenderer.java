

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
import rip.ysm.gpu.GpuCapability;
import rip.ysm.gpu.GpuRenderPath;
import rip.ysm.gpu.IrisRenderPath;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class NativeModelRenderer {
    private static final Matrix4f projectionModelViewMatrix = new Matrix4f();

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        renderMesh(buffer, pose, model, boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, null);
    }

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, net.minecraft.resources.ResourceLocation textureLocation) {
        OculusCompat.updatePBRState();
        MatrixBridge.projectionMatrix().mul(MatrixBridge.modelViewMatrix(), projectionModelViewMatrix);
        boolean isPreview = ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer();

        if (textureLocation != null && NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get() && GeneralConfig.USE_GPU_RENDERER.get()) {

            if(!GpuCapability.isAvailable())
            {
                ChatLogger.INSTANCE.logFormatted("Disabled GPU renderer for: " + GpuCapability.getReason());
                GeneralConfig.USE_GPU_RENDERER.set(false);
                return;
            }

            if (OculusCompat.isShaderPackInUse() && !isPreview) {
                if (IrisRenderPath.tryRender(model, pose, boneParams, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            } else {
                if (GpuRenderPath.tryRender(model, pose, boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            }
        }

        // debug-1201-windows-gpu: 带光影包（Iris 分支回退后落到此处）时不再走 SIMD native 直写——
        // Embeddium 改造的 BufferBuilder 下 native 直写渲染损坏（llvmpipe 实证：世界内模型巨大化+全黑），
        // 用户真机默认配置世界全黑亦与该链路相符。带光影包改走 CPU 缓冲路径（原版管线，Iris 兼容，
        // compat 渲染器同链已实证可见）；无光影包场景 SIMD 行为不变。
        boolean cpuBufferFallback = OculusCompat.isShaderPackInUse();
        if (NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get() && !cpuBufferFallback) { // WIP: SIMD MODEL RENDER
            nativeRenderModel(
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
