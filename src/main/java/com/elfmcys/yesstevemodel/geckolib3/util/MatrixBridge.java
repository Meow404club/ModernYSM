package com.elfmcys.yesstevemodel.geckolib3.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * moj math ↔ JOML 矩阵桥。1.16.5 的 PoseStack.last() 产出 com.mojang.math 矩阵
 * （1.19.3+ 才换 JOML），而共享源渲染链全部以 JOML 为工作类型。
 *
 * 布局换算推导（端到端字节码实证，非记忆；BLOCKER-1 打回修复版）：
 *  - moj 1.16.5 矩阵 field grid 即教科书矩阵（列向量约定，端到端 Sanity 全 PASS）：
 *    createTranslateMatrix(5,6,7) 置 m03/m13/m23；Vector4f.transform(1,1,1,1)->(6,7,8,1)；
 *  - moj Matrix4f.store(FloatBuffer)=GL 列主序：buf[c*4+r]=field m{r,c}（真类 store 断言
 *    buf[12]=5、buf[3]=0）。源码反证链：每帧投影上传 GameRenderer.resetProjectionMatrix
 *    →RenderSystem.multMatrix:522→GlStateManager._multMatrix:1143→store→glMultMatrixf:1139，
 *    行主序假设则平移落底行、全游戏渲染崩。反编译 bufferIndex 双参同现雪人 ☃，不可信；
 *  - JOML set(a,b,v) 直写 mem[a*4+b]=field m{a}{b}（MemUtilUnsafe.set 偏移 a*16+b*4，
 *    javap 复核；field 名序=内存序）。注意：JOML field 名 digits=(列,行)，m30=平移锚，
 *    与 moj 的 (行,列) 命名互为转置——此前"set(col,row) 写 m_{row,col}"的口误即打回根因；
 *  - 故 buffer 路径（pose/fromGl）恒等拷贝即正确：out.set(c, r, buf.get(c*4+r))
 *    ⇒ mem[k]=buf[k]，JOML get(float[]) 输出与 moj store/glGetFloatv 输出逐字节相同
 *    （SanityEndToEnd 实测）。若按打回单给 buffer 路径换位为 set(r,c,…) 反产 T^T，勿改；
 *  - Matrix3f（normal）无 store，走反射读 field：moj field m{ij}=G3(i,j)（(行,列)命名），
 *    欲还原须 mem[r*3+c]←G3(c,r)=moj(c,r)，即 out.set(r, c, FIELD[c*3+r])——
 *    换位只属于本函数。旧写法 set(c,r,…) 产法线转置（rotZ90 把 (1,0,0) 映成 (0,-1,0)），
 *    端到端修复后 (0,1,0)。
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

    //? if <1.17 {
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
    //             // set(a,b,v) 直写 mem[a*4+b]（joml-1.10.5 字节码实证），故本循环=mem[k]=buf[k]
    //             // 恒等拷贝：store 与 JOML 内存映像同为 GL 列主序（SanityEndToEnd 全 PASS）。
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
    //                 // BLOCKER-1 真修复点：moj 3x3 field m{ij}=G3(i,j)（digits=行,列），
    //                 // JOML set(a,b)→mem[a*3+b]=几何槽位 G3(b,a)；欲还原 G3 须
    //                 // mem[r*3+c]←moj(c,r)，即 set(r, c, FIELD[c*3+r])。
    //                 // 旧写法 set(c,r,…) 把 G3(c,r) 填进 G3(r,c) 槽位=法线矩阵转置
    //                 //（SanityEndToEnd：rotZ90 把 (1,0,0) 映成 (0,-1,0)）。
    //                 out.set(r, c, MOJ_M3_FIELDS[c * 3 + r].getFloat(moj));
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
    //     // 必须 DIRECT 缓冲：glGetFloatv 是原生写入，堆缓冲（FloatBuffer.allocate）地址=0，
    //     // Mesa/libgallium 直接对 0 地址写 → SIGSEGV（1.16.5 dev runClient hs_err 实证，
    //     // si_addr=0x0，Problematic frame=libgallium）。
    //     java.nio.FloatBuffer buf = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    //     com.mojang.blaze3d.platform.GlStateManager._getMatrix(glMode, buf);
    //     buf.rewind();
    //     Matrix4f out = new Matrix4f();
    //     for (int c = 0; c < 4; c++) {
    //         for (int r = 0; r < 4; r++) {
    //             // glGetFloatv 输出=GL 列主序，与 store 同布局，恒等拷贝同 pose()。
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
