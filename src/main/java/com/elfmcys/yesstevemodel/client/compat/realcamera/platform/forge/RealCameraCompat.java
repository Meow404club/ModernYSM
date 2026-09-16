package com.elfmcys.yesstevemodel.client.compat.realcamera.platform.forge;

import net.minecraftforge.fml.ModList;

public class RealCameraCompat {

    private static final String MOD_ID = "realcamera";

    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
        if (IS_LOADED) {
            // fix-fpm-rc B1：骨骼直产 BindResult 软注册（反射，缺席/漂移静默禁用）
            RealCameraApiBinder.register();
        }
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    public static boolean isActive() {
        if (IS_LOADED) {
            return RealCameraChecker.isRealCameraActive();
        }
        return false;
    }

    /** fix-fpm-rc R3：绑定 GUI 打开判定（详见 RealCameraChecker.isRealCameraBindGuiOpen）。 */
    public static boolean isBindGuiOpen() {
        return IS_LOADED && RealCameraChecker.isRealCameraBindGuiOpen();
    }
}