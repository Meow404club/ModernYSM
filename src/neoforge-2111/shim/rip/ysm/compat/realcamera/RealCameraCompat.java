// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.realcamera;

public final class RealCameraCompat {

    private RealCameraCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isActive() {
        return false;
    }
}
