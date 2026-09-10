// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.optifine;

public final class OptiFineDetector {

    private OptiFineDetector() {
    }

    public static boolean isOptifinePresent() {
        return false;
    }
}
