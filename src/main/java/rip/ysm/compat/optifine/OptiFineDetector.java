package rip.ysm.compat.optifine;

import rip.ysm.compat.optifine.platform.forge.OptiFineDetectorImpl;


public final class OptiFineDetector {

    private OptiFineDetector() {
    }

    public static boolean isOptifinePresent() {
        return OptiFineDetectorImpl.isOptifinePresent();
    }
}
