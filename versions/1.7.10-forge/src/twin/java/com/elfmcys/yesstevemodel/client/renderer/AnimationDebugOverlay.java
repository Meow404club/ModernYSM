package com.elfmcys.yesstevemodel.client.renderer;

/**
 * 1.7.10 原生孪生 AnimationDebugOverlay（legacy1710-l2a-model-load）。
 *
 * 共享版是现代 HUD overlay 链，1710 全功能调试 overlay 不在本卡。
 * 本孪生只承载编译/运行消费面：isDebugActive() 开关（-Dysm.legacy1710.debug）。
 */
public class AnimationDebugOverlay {
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy1710.debug");

    private AnimationDebugOverlay() {
    }

    public static boolean isDebugActive() {
        return DEBUG;
    }
}
