package rip.ysm.compat.realcamera;

import rip.ysm.compat.realcamera.platform.forge.RealCameraCompatImpl;


public final class RealCameraCompat {

    private RealCameraCompat() {
    }

    public static boolean isLoaded() {
        return RealCameraCompatImpl.isLoaded();
    }

    public static boolean isActive() {
        return RealCameraCompatImpl.isActive();
    }

    /** fix-fpm-rc R3：RealCamera 绑定 GUI 打开判定（NativeModelRenderer 强制 CPU 管线消费）。 */
    public static boolean isBindGuiOpen() {
        return RealCameraCompatImpl.isBindGuiOpen();
    }
}
