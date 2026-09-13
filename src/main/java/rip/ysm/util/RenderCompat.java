package rip.ysm.util;

/**
 * RenderSystem 面兼容门面（UseAction/Rl 同模式）。
 *
 * 1.21.2 原版渲染管线重构（GPU 抽象/RenderPipeline 化）删除了两个 RenderSystem 静态方法：
 *  - applyModelViewMatrix()：model-view 矩阵改为 draw 时活读
 *   （vanilla-1.21.3 RenderSystem.java:708-715 getModelViewMatrix/getModelViewStack 公有化，
 *   BufferUploader.drawWithShader 内部取用）→ 上传动作变 no-op；
 *  - runAsFancy(Runnable)：fancy 判定进 RenderPipelines → 直调 lambda。
 */
public final class RenderCompat {
    private RenderCompat() {
    }

    public static void applyModelViewMatrix() {
        // 1.16.5 无此 API（GL_MODELVIEW 直推路径，调用点在 <1.17 分支为注释态）
        //? if >=1.17 && <1.21.2
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    public static void runAsFancy(Runnable action) {
        //? if <1.21.2
        com.mojang.blaze3d.systems.RenderSystem.runAsFancy(action);
        //? if >=1.21.2
        /*action.run();*/
    }

    // ===== 1.21.5 RenderPipeline 化：立即绘制缓存/混合状态入口删除 → 门面 no-op =====
    // 1.21.5 BufferUploader 类删除（立即绘制系统整体移除，无可失效缓存）
    // 1.16.5 无 BufferUploader（1.17+ 类，1165 generated :33 找不到符号实证）→ 下界 >=1.17
    public static void invalidate() {
        //? if >=1.17 && <21.5
        com.mojang.blaze3d.vertex.BufferUploader.invalidate();
    }

    public static void reset() {
        //? if >=1.17 && <21.5
        com.mojang.blaze3d.vertex.BufferUploader.reset();
    }

    // 1.21.5 混合状态进 RenderPipeline 定义（GUI 管线自带 blend）→ no-op；
    // 自定义 GL 绘制路径（gpu 包）在 >=1.21.5 已降级，不受影响
    public static void enableBlend() {
        //? if <21.5
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
    }

    public static void disableBlend() {
        //? if <21.5
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    public static void defaultBlendFunc() {
        //? if <21.5
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
}
