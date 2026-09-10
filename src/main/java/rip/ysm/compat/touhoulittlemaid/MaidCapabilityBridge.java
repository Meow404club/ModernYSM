package rip.ysm.compat.touhoulittlemaid;

import net.minecraft.world.entity.Entity;

import java.util.Optional;
import rip.ysm.compat.touhoulittlemaid.platform.forge.MaidCapabilityBridgeImpl;

public final class MaidCapabilityBridge {

    private MaidCapabilityBridge() {
    }

    public static Optional<Object> get(Entity entity) {
        return MaidCapabilityBridgeImpl.get(entity);
    }
}
