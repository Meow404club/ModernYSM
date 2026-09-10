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
}
