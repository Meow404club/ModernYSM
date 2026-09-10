package rip.ysm.compat.firstperson.platform.forge;
import com.elfmcys.yesstevemodel.client.compat.firstperson.platform.forge.FirstPersonCompat;

public final class FirstPersonCompatImpl {

    private FirstPersonCompatImpl() {
    }

    public static boolean isLoaded() {
        return FirstPersonCompat.isLoaded();
    }

    public static boolean isFirstPersonActive() {
        return FirstPersonCompat.isFirstPersonActive();
    }

    public static boolean shouldHideHead() {
        return FirstPersonCompat.shouldHideHead();
    }

    public static void setCameraDistance(float distance) {
        FirstPersonCompat.setCameraDistance(distance);
    }
}
