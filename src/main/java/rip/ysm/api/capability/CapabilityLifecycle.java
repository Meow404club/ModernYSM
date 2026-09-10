package rip.ysm.api.capability;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.capability.platform.forge.CapabilityLifecycleImpl;

public final class CapabilityLifecycle {

    private CapabilityLifecycle() {
    }

    @ExpectPlatform
    public static void revive(Entity entity) {
        CapabilityLifecycleImpl.revive(entity);
    }

    @ExpectPlatform
    public static void invalidate(Entity entity) {
        CapabilityLifecycleImpl.invalidate(entity);
    }
}
