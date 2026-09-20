package com.elfmcys.yesstevemodel.client.renderer;

/**
 * 1.12.2 原生孪生 AnimationDebugOverlay（legacy-1222-l1-render）。
 *
 * 共享版是现代 HUD overlay 链（1.20.1 GUI 面），1.12.2 全功能调试 overlay 是 L2。
 * 本孪生只承载编译/运行消费面：isDebugActive() 开关（-Dysm.legacy122.debug）。
 */
public class AnimationDebugOverlay {
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy122.debug");

    private AnimationDebugOverlay() {
    }

    public static boolean isDebugActive() {
        return DEBUG;
    }
}
