package rip.ysm.compat.oculus;

import rip.ysm.compat.oculus.platform.forge.OculusCompatImpl;


public final class OculusCompat {

    private OculusCompat() {
    }

    public static boolean isLoaded() {
        return OculusCompatImpl.isLoaded();
    }

    public static boolean isPBRActive() {
        return OculusCompatImpl.isPBRActive();
    }

    public static void updatePBRState() {
        OculusCompatImpl.updatePBRState();
    }

    public static boolean isShaderPackInUse() {
        return OculusCompatImpl.isShaderPackInUse();
    }

    public static boolean isRenderingShadowPass() {
        return OculusCompatImpl.isRenderingShadowPass();
    }
}
