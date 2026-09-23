package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.client.renderer.GlStateManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * GeoEntityRenderer→固定管线翻译层（legacy-1222-l1-render commit 2）。
 *
 * 1.12.2 无 PoseStack/MultiBufferSource/RenderType——GeoModel.bakedBones 烘焙几何
 * （BakedQuad float 面契约：positions 12/uv 8/normal 3）经骨矩阵链（boneParams 每骨
 * 12 float：rot/pos/scale/hidden/skip/track，NativeModelRenderer.calculateBoneMatrix:328
 * 同款数学）变换后 GL11 直绘。对照：vanilla-mc-1.12.2 RenderLivingBase.prepareScale:161
 * （-1,-1,1 翻转 + 0.0625 模型尺度 + -1.501 平移）。
 *
 * 矩阵约定：1.12.2 GL11 固定管线列主序（glMultMatrixf GL 列主序，MatrixBridge 头注
 * 「moj Matrix4f.store(FloatBuffer)=GL 列主序 buf[c*4+r]」同源），JOML get(float[])
 * 输出与 store 逐字节相同——直接喂 glMultMatrixf。
 *
 * ponytail: 无渲染层剔除/光照烘焙，L2 按需补；SIMD native 路径不做（固定管线无
 * VertexConsumer 契约，GpuRenderPath 固定管线禁用）。
 */
public final class LegacyModelTranslator {

    private LegacyModelTranslator() {
    }

    private static final Matrix3f NORMAL_MAT = new Matrix3f();
    private static final Vector3f NORMAL_VEC = new Vector3f();
    private static final float[] MATRIX_BUF = new float[16];
    private static final float[] NORMAL_BUF = new float[9];

    // 打点：验收需「动画 tick 计数/bone 变换打点原文行」。环境变量开关，默认关。
    private static final boolean DEBUG_LOG = Boolean.getBoolean("ysm.legacy122.debug");

    private static long debugFrame;

    /**
     * 绘制一个 YSM 模型。调用方（RenderPlayerEvent 接缝/实体 hook）已在 GL 上完成
     * 平移/旋转定位（1.12.2 RenderManager.renderEntityWithYawPitch 同款状态），
     * 本方法从模型原点起绘。
     *
     * @param model       烘焙几何（GeoModel.bakedBones 必非空）
     * @param boneParams  动画面 12 float/骨（AnimatedGeoModel.getMatrixData()）
     * @param r/g/b/a     颜色
     */
    public static void render(GeoModel model, float[] boneParams, float r, float g, float b, float a) {
        if (model == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return;
        }
        java.util.List<GeoModel.BakedBone> bones = model.bakedBones;

        // Y 轴约定矫正（legacy-1222-l2-full）：烘焙数据是 Y-up（Bedrock 约定，head
        // pivot y=32.4 在顶/脚 y=0 在底，YSMFolderDeserializer:547-553 保号实证），
        // 现代 1.20.1 线（NativeModelRenderer.renderModel/IGeoRenderer.renderEarly）
        // 对同一份 bakedBones 无任何翻转——vanilla 1.12.2 prepareScale:159-162 的
        // scale(-1,-1,1)+translate(0,-1.501,0) 是为 vanilla Y-DOWN 模型数据（头 y=0
        // 原点在颈）设计的配对翻转，Y-up 数据过 Y 翻转即头朝地。只保留 X 镜像
        //（与 L1 截图的水平朝向一致），脚在模型空间 y=0 无需 -1.501 平移。
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, 1.0F, 1.0F);

        // 1.12.2 无 RenderSystem 分离投影——GL 状态即管线状态，无需 MatrixBridge.proj/modelView
        boolean[] visibleCache = new boolean[bones.size()];
        Matrix4f[] cache = skeleton(bones, boneParams, visibleCache);

        int quadsDrawn = 0;
        for (int i = 0; i < bones.size(); i++) {
            if (!visibleCache[i]) {
                continue;
            }
            GeoModel.BakedBone bone = bones.get(i);
            Matrix4f boneMat = cache[i];
            boneMat.get(MATRIX_BUF);
            NORMAL_MAT.set(boneMat).normal().get(NORMAL_BUF);

            GL11.glPushMatrix();
            GL11.glMultMatrixf(MATRIX_BUF);
            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    GL11.glBegin(GL11.GL_QUADS);
                    for (int v = 0; v < 4; v++) {
                        NORMAL_VEC.set(quad.normal[0], quad.normal[1], quad.normal[2]);
                        NORMAL_VEC.mul(NORMAL_MAT);
                        GL11.glNormal3f(NORMAL_VEC.x, NORMAL_VEC.y, NORMAL_VEC.z);
                        // BakedQuad UV 布局 per-vertex（uv 8 floats/quad）：烘焙面已是 atlas UV
                        GL11.glTexCoord2f(quad.uvs[v * 2], quad.uvs[v * 2 + 1]);
                        GL11.glColor4f(r, g, b, a);
                        GL11.glVertex3f(quad.positions[v * 3], quad.positions[v * 3 + 1], quad.positions[v * 3 + 2]);
                    }
                    GL11.glEnd();
                    quadsDrawn++;
                }
            }
            GL11.glPopMatrix();
        }

        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        if (DEBUG_LOG && (debugFrame++ % 40 == 0)) {
            System.out.printf("[ysm-legacy122] translator frame=%d bones=%d quadsDrawn=%d boneParams=%d rootBoneRot=(%.3f,%.3f,%.3f)%n",
                    debugFrame, bones.size(), quadsDrawn, boneParams == null ? -1 : boneParams.length,
                    boneParams == null ? 0 : boneParams[0], boneParams == null ? 0 : boneParams[1],
                    boneParams == null ? 0 : boneParams[2]);
        }
    }

    // NativeModelRenderer.calculateBoneMatrix 同款数学（rootPose 恒单位阵：
    // 1.12.2 定位走 GL 状态栈，非模型矩阵），可见性判定镜像 offset6-10 契约。
    // M-U2 r2：矩阵计算提为 skeleton() 单源。r3：computeBounds（AABB 自适应
    // 缩放）随 FILL0.80 发明一并弃用——主线卡内=固定 scale30×heightScale 原点锚。
    private static Matrix4f[] skeleton(java.util.List<GeoModel.BakedBone> bones,
                                       float[] boneParams, boolean[] visibleCache) {
        Matrix4f rootPose = new Matrix4f();
        Matrix4f[] cache = new Matrix4f[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            isVisibleBone(i, bones, boneParams, cache, visibleCache, rootPose);
        }
        return cache;
    }

    private static boolean isVisibleBone(int idx, java.util.List<GeoModel.BakedBone> bones,
                                         float[] boneParams, Matrix4f[] cache, boolean[] visibleCache,
                                         Matrix4f rootPose) {
        if (cache[idx] != null) return visibleCache[idx];

        GeoModel.BakedBone bone = bones.get(idx);
        boolean isVisible = true;
        Matrix4f localMat = new Matrix4f();

        if (bone.parentIdx != -1) {
            isVisibleBone(bone.parentIdx, bones, boneParams, cache, visibleCache, rootPose);
            localMat.set(cache[bone.parentIdx]);
            if (!visibleCache[bone.parentIdx]) {
                isVisible = false;
            }
        } else {
            localMat.set(rootPose);
        }

        int p = idx * 12;
        float animRx = boneParams[p];
        float animRy = boneParams[p + 1];
        float animRz = boneParams[p + 2];
        float animTx = boneParams[p + 3];
        float animTy = boneParams[p + 4];
        float animTz = boneParams[p + 5];
        float animSx = boneParams[p + 6];
        float animSy = boneParams[p + 7];
        float animSz = boneParams[p + 8];
        float hiddenFlag = boneParams[p + 9];
        float skipChildrenFlag = boneParams[p + 10];

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
        if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
            localMat.scale(animSx, animSy, animSz);
        }
        localMat.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f);

        cache[idx] = localMat;
        visibleCache[idx] = isVisible;
        return isVisible;
    }
}
