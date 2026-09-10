package rip.ysm.api;

import com.elfmcys.yesstevemodel.platform.YsmPlatform;
import net.minecraftforge.api.distmarker.Dist;

/**
 * 平台门面（rip.ysm.api 公共 API 面，兼容旧调用点）。M1 起内部委托
 * {@link YsmPlatform}（Forge 原生），不再依赖 architectury @ExpectPlatform/织入。
 * 调用方签名不变：isServer/getPlatformName。
 */
public final class PlatformAPI {
    private PlatformAPI() {
    }

    /** 专用服为 true（对齐原 PlatformAPIImpl.isServer 的 Dist.DEDICATED_SERVER 语义）。 */
    public static boolean isServer() {
        return YsmPlatform.getEnv() == Dist.DEDICATED_SERVER;
    }

    public static String getPlatformName() {
        return "Forge";
    }
}
