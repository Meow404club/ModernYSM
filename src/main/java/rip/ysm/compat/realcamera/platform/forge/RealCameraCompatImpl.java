package rip.ysm.compat.realcamera.platform.forge;
import com.elfmcys.yesstevemodel.client.compat.realcamera.platform.forge.RealCameraCompat;

public final class RealCameraCompatImpl {

    private RealCameraCompatImpl() {
    }

    public static boolean isLoaded() {
        return RealCameraCompat.isLoaded();
    }

    public static boolean isActive() {
        return RealCameraCompat.isActive();
    }
}
