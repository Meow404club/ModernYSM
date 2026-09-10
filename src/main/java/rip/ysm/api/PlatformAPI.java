package rip.ysm.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import rip.ysm.api.platform.forge.PlatformAPIImpl;

public final class PlatformAPI {
    private PlatformAPI() {
    }

    /**
     * M0 无织入降级：构造期即被 YesSteveModel.init/YsmEventBootstrap 调用，
     * stub 体按 ADR 替换矩阵①直调平移进树的 forge 实现（1.20.1-forge 单版本，
     * 多版本条件化留给 M1 mig-* 卡）。
     */
    @ExpectPlatform
    public static boolean isServer() {
        return PlatformAPIImpl.isServer();
    }

    @ExpectPlatform
    public static String getPlatformName() {
        return PlatformAPIImpl.getPlatformName();
    }
}
