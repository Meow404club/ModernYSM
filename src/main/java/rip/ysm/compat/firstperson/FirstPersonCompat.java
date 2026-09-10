package rip.ysm.compat.firstperson;

import rip.ysm.compat.firstperson.platform.forge.FirstPersonCompatImpl;

public final class FirstPersonCompat {

    private FirstPersonCompat() {
    }

    public static boolean isLoaded() {
        return FirstPersonCompatImpl.isLoaded();
    }

    public static boolean isFirstPersonActive() {
        return FirstPersonCompatImpl.isFirstPersonActive();
    }

    public static boolean shouldHideHead() {
        return FirstPersonCompatImpl.shouldHideHead();
    }

    public static void setCameraDistance(float distance) {
        FirstPersonCompatImpl.setCameraDistance(distance);
    }
}
