package rip.ysm.compat.immersiveaircraft;

import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import org.joml.Vector3f;

import java.util.Optional;
import rip.ysm.compat.immersiveaircraft.platform.forge.ImmersiveAirCraftCompatImpl;

public final class ImmersiveAirCraftCompat {

    private ImmersiveAirCraftCompat() {
    }

    public static boolean isLoaded() {
        return ImmersiveAirCraftCompatImpl.isLoaded();
    }

    public static Optional<Vector3f> getAircraftRotation(AnimationEvent<GeckoVehicleEntity> event) {
        return ImmersiveAirCraftCompatImpl.getAircraftRotation(event);
    }
}
