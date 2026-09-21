package rip.ysm.legacy1710;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * 固定管线翻译层（legacy-1710-l1-render）。
 *
 * 1.12.2 LegacyModelTranslator（versions/1.12.2-forge legacy122 包，合并链
 * 24c9e8f/613fa38）的 1.7.10 twin：烘焙几何（LegacyBakedModel，12 float
 * positions/8 uv/3 normal）经骨矩阵链（boneParams 12 float/骨：rot/pos/scale/
 * hidden/skip，NativeModelRenderer.calculateBoneMatrix 同款数学）GL11 直绘。
 *
 * GL 状态对位 vanilla-mc-1.7.10 RendererLivingEntity.doRender:52-98：
 * glPushMatrix/glDisable(CULL) → rotateCorpse → glEnable(RESCALE_NORMAL 32826)
 * → scale(-1,-1,1) → translate(0,-24*0.0625-0.0078125,0)。本方法在 mixin 注入点
 * （RendererLivingEntity renderModel 前）接管时，调用方已做平移/旋转/翻转——
 * Y 轴约定（1.12.2 修复教训 613fa38）：烘焙数据 Y-up，vanilla 1.7.10 管线的
 * scale(-1,-1,1) 是为 Y-DOWN 模型数据（ModelBiped 同族）设计的配对翻转，
 * 只保留 scale(-1,1,1) X 镜像补偿，不做 Y 翻译/-1.501 位移。
 *
 * ponytail: 无渲染剔除/光照烘焙，L2 按需补。
 */
public final class LegacyModelTranslator {

    private LegacyModelTranslator() {
    }

    private static final Matrix4f LOCAL_BONE_MAT = new Matrix4f();
    private static final Matrix3f NORMAL_MAT = new Matrix3f();
    private static final Vector3f NORMAL_VEC = new Vector3f();
    private static final float[] MATRIX_BUF = new float[16];
    private static final float[] NORMAL_BUF = new float[9];

    // 打点：验收需「渲染帧 quadsDrawn/boneParams 打点原文行」。环境变量开关，默认关。
    private static final boolean DEBUG_LOG = Boolean.getBoolean("ysm.legacy1710.debug");

    private static long debugFrame;

    /**
     * 绘制一个 YSM 模型。调用方（mixin 注入点）已在其 GL 状态内完成实体定位
     * （1.7.10 RendererLivingEntity.renderLivingAt:244 同款），本方法从模型原点起绘。
     */
    public static void render(LegacyBakedModel model, float[] boneParams, float r, float g, float b, float a) {
        if (model == null || model.bones.isEmpty()) {
            return;
        }
        int boneCount = model.bones.size();

        // Y 轴约定（613fa38）：烘焙数据 Y-up；注入点已过 vanilla scale(-1,-1,1)
        // （RendererLivingEntity.doRender:96），此处只补 X 镜像→净效果 scale(-1,1,1)。
        // 脚在模型空间 y=0，无 -1.501 平移。
        GL11.glPushMatrix();
        GL11.glScalef(-1.0F, 1.0F, 1.0F);

        Matrix4f rootPose = new Matrix4f();
        Matrix4f[] cache = new Matrix4f[boneCount];
        boolean[] visibleCache = new boolean[boneCount];

        int quadsDrawn = 0;
        for (int i = 0; i < boneCount; i++) {
            if (!isVisibleBone(i, model, boneParams, cache, visibleCache, rootPose)) {
                continue;
            }
            LegacyBakedModel.BakedBone bone = model.bones.get(i);
            Matrix4f boneMat = cache[i];
            boneMat.get(MATRIX_BUF);
            NORMAL_MAT.set(boneMat).normal().get(NORMAL_BUF);

            GL11.glPushMatrix();
            GL11.glMultMatrixf(MATRIX_BUF);
            for (LegacyBakedModel.BakedCube cube : bone.cubes) {
                for (LegacyBakedModel.BakedQuad quad : cube.quads) {
                    GL11.glBegin(GL11.GL_QUADS);
                    for (int v = 0; v < 4; v++) {
                        NORMAL_VEC.set(quad.normal[0], quad.normal[1], quad.normal[2]);
                        NORMAL_VEC.mul(NORMAL_MAT);
                        GL11.glNormal3f(NORMAL_VEC.x, NORMAL_VEC.y, NORMAL_VEC.z);
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

        GL11.glPopMatrix();

        if (DEBUG_LOG && (debugFrame++ % 40 == 0)) {
            System.out.printf("[ysm-legacy1710] translator frame=%d bones=%d quadsDrawn=%d boneParams=%d%n",
                    debugFrame, boneCount, quadsDrawn, boneParams == null ? -1 : boneParams.length);
        }
    }

    // NativeModelRenderer.calculateBoneMatrix 同款数学（rootPose 恒单位阵：
    // 1.7.10 定位走 GL 状态栈），可见性判定镜像 offset6-10 契约
    private static boolean isVisibleBone(int idx, LegacyBakedModel model,
                                         float[] boneParams, Matrix4f[] cache, boolean[] visibleCache,
                                         Matrix4f rootPose) {
        if (cache[idx] != null) return visibleCache[idx];

        LegacyBakedModel.BakedBone bone = model.bones.get(idx);
        boolean isVisible = true;
        Matrix4f localMat = LOCAL_BONE_MAT.set(rootPose);

        if (bone.parentIdx != -1) {
            isVisibleBone(bone.parentIdx, model, boneParams, cache, visibleCache, rootPose);
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

        cache[idx] = new Matrix4f(localMat);
        visibleCache[idx] = isVisible;
        return isVisible;
    }
}
