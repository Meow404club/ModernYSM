package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；客户端 molang 状态能力）。口径见 PlayerCapabilityProvider 孪生头注。
 */
public class VehicleCapabilityProvider {

    public static final EntityCapability<VehicleCapability, Void> VEHICLE_CAP =
            EntityCapability.createVoid(Rl.of("yes_steve_model", "vehicle_animatable"), VehicleCapability.class);

    private static final Map<Entity, VehicleCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static VehicleCapabilityProvider forEntity(Entity entity) {
        return INSTANCES.computeIfAbsent(entity, VehicleCapabilityProvider::new);
    }

    private VehicleCapability capability;

    private Entity entity;

    private VehicleCapabilityProvider(Entity entity) {
        this.entity = entity;
    }

    public VehicleCapability getCapability() {
        return getOrCreateCapability();
    }

    private VehicleCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new VehicleCapability(this.entity);
            this.entity = null;
        }
        return this.capability;
    }
}
