// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.firstperson;

public final class FirstPersonCompat {

    private FirstPersonCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isFirstPersonActive() {
        return false;
    }

    public static boolean shouldHideHead() {
        return false;
    }

    public static void setCameraDistance(float distance) {
    }
}
