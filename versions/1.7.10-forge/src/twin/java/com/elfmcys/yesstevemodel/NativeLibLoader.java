package com.elfmcys.yesstevemodel;

/**
 * 1.7.10 分代 NativeLibLoader（legacy1710-l2a-model-load）。
 *
 * 共享版绑定 JNA 装载 + Component/StringUtil 重 MC 面。1710 线唯一消费点
 * 是 YSMClientMapper 的 isLoaded()（mesh SIMD 加速，可选路径）。恒 false 走
 * 纯 Java 路径，与共享版"装载失败降级"语义一致（1.12.2 twin 同构）。
 */
public final class NativeLibLoader {

    private NativeLibLoader() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void loadLibrary() {
        // ponytail: 1.7.10 无 SIMD native 面，no-op；接入时对齐共享版 loadLibrary 契约
    }
}
