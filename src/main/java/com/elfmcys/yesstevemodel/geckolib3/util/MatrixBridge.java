package com.elfmcys.yesstevemodel.geckolib3.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * moj math ↔ JOML 矩阵桥。1.16.5 的 PoseStack.last() 产出 com.mojang.math 矩阵
 * （1.19.3+ 才换 JOML），而共享源渲染链全部以 JOML 为工作类型。
 *
 * 布局换算推导（源码实证，非记忆）：
 *  - vanilla 1.16.5 Vector3f.transform(Matrix4f) 按行向量 v·M 求值，
 *    JOML 按列向量 M·v 求值 → 同一几何变换 T 满足 M_moj = T^T；
 *  - moj Matrix4f.store(FloatBuffer) 落盘 mem[i*4+j]=m_ij（行主序，bufferIndex(i,j)=i*4+j）；
 *  - JOML get(float[],off) 输出 arr[c*4+r]=T(r,c)（列主序，native/JNI 契约即此布局），
 *    故 T(r,c)=M_moj(c,r)=buf.get(c*4+r)，即 out.set(c, r, buf.get(c*4+r))（转置回填）；
 *  - Matrix3f（normal）1.16.5 无 store/getter，走反射读 protected m00..m22（同转置规则），
 *    反射 Field 静态缓存，每帧 9 次 getFloat 开销可忽略；
 *  - 1.16.5 无 RenderSystem.getProjectionMatrix()/getModelViewMatrix()（1.17+），
 *    投影/模型视图从 GL 固定管线状态读（GlStateManager._getMatrix，public，
 *    mulTextureByProjModelView 内部同款调用：2983=GL_PROJECTION_MATRIX、2982=GL_MODELVIEW_MATRIX）。
 *
 * 1.20.1 分支全部直通（PoseStack 产出即 JOML），调用点无条件单路径，行为零变化。
 * 非活跃分支整块 // 前缀（共享源恒 1.20.1 合法 Java，stonecutter 生成 1.16.5 时剥离）。
 * Task: m2-render-pipeline-condition
 */
public final class MatrixBridge {

    private MatrixBridge() {
    }

    //? if < 1.17 {
    // private static final java.lang.reflect.Field[] MOJ_M3_FIELDS = cacheMojFields();
    //
    // private static java.lang.reflect.Field[] cacheMojFields() {
    //     String[] names = {"m00", "m01", "m02", "m10", "m11", "m12", "m20", "m21", "m22"};
    //     java.lang.reflect.Field[] fields = new java.lang.reflect.Field[9];
    //     try {
    //         for (int i = 0; i < 9; i++) {
    //             java.lang.reflect.Field f = com.mojang.math.Matrix3f.class.getDeclaredField(names[i]);
    //             f.setAccessible(true);
    //             fields[i] = f;
    //         }
    //     } catch (ReflectiveOperationException e) {
    //         throw new ExceptionInInitializerError(e);
    //     }
    //     return fields;
    // }
    //
    // public static Matrix4f pose(PoseStack.Pose pose) {
    //     java.nio.FloatBuffer buf = java.nio.FloatBuffer.allocate(16);
    //     pose.pose().store(buf);
    //     Matrix4f out = new Matrix4f();
    //     for (int c = 0; c < 4; c++) {
    //         for (int r = 0; r < 4; r++) {
    //             out.set(c, r, buf.get(c * 4 + r));
    //         }
    //     }
    //     return out;
    // }
    //
    // public static Matrix3f normal(PoseStack.Pose pose) {
    //     com.mojang.math.Matrix3f moj = pose.normal();
    //     Matrix3f out = new Matrix3f();
    //     try {
    //         for (int c = 0; c < 3; c++) {
    //             for (int r = 0; r < 3; r++) {
    //                 out.set(c, r, MOJ_M3_FIELDS[c * 3 + r].getFloat(moj));
    //             }
    //         }
    //     } catch (IllegalAccessException e) {
    //         throw new IllegalStateException("MatrixBridge.normal reflection failed", e);
    //     }
    //     return out;
    // }
    //
    // public static Matrix4f projectionMatrix() {
    //     return fromGl(2983);
    // }
    //
    // public static Matrix4f modelViewMatrix() {
    //     return fromGl(2982);
    // }
    //
    // private static Matrix4f fromGl(int glMode) {
    //     java.nio.FloatBuffer buf = java.nio.FloatBuffer.allocate(16);
    //     com.mojang.blaze3d.platform.GlStateManager._getMatrix(glMode, buf);
    //     buf.rewind();
    //     Matrix4f out = new Matrix4f();
    //     for (int c = 0; c < 4; c++) {
    //         for (int r = 0; r < 4; r++) {
    //             out.set(c, r, buf.get(c * 4 + r));
    //         }
    //     }
    //     return out;
    // }
    //? } else {
    public static Matrix4f pose(PoseStack.Pose pose) {
        return pose.pose();
    }

    public static Matrix3f normal(PoseStack.Pose pose) {
        return pose.normal();
    }

    public static Matrix4f projectionMatrix() {
        return com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
    }

    public static Matrix4f modelViewMatrix() {
        return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
    }
    //? }
}
