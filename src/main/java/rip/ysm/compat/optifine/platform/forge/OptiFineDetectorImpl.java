package rip.ysm.compat.optifine.platform.forge;
import com.elfmcys.yesstevemodel.client.compat.optifine.platform.forge.OptiFineDetector;

public final class OptiFineDetectorImpl {

    private OptiFineDetectorImpl() {
    }

    public static boolean isOptifinePresent() {
        return OptiFineDetector.isOptifinePresent();
    }
}
