package com.elfmcys.yesstevemodel;

/**
 * 1.12.2 分代 NativeLibLoader（legacy-1222-l2-full）。
 *
 * 共享版绑定 JNA 装载 + Component/StringUtil 重 MC 面。1.12.2 线唯一消费点
 * 是 YSMClientMapper:564 的 isLoaded()（mesh.buildNativeCache() SIMD 加速，
 * 可选路径）。L2 无 native 装载面——恒 false 走纯 Java 路径，与共享版
 * "装载失败降级"语义一致。真 native 接入=L3 候选。
 */
public final class NativeLibLoader {

    private NativeLibLoader() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void loadLibrary() {
        // ponytail: 1.12.2 无 SIMD native 面，no-op；接入时对齐共享版 loadLibrary 契约
    }
}
