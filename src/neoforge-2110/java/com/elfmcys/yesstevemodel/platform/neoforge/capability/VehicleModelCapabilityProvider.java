package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。口径见 ModelInfoCapabilityProvider 孪生头注；
 * 载具为可持久实体但 20.4 重铸后无逐实体保存事件，会话级持久化（语义降级点已入交接账）。
 */
public class VehicleModelCapabilityProvider {

    public static final EntityCapability<VehicleModelCapability, Void> VEHICLE_MODEL_CAP =
            EntityCapability.createVoid(Rl.of(YesSteveModel.MOD_ID, "vehicle_model_id"), VehicleModelCapability.class);

    private static final Map<Entity, VehicleModelCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static VehicleModelCapabilityProvider forEntity(Entity entity) {
        return INSTANCES.computeIfAbsent(entity, e -> new VehicleModelCapabilityProvider());
    }

    private VehicleModelCapability capability = null;

    public VehicleModelCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new VehicleModelCapability();
        }
        return this.capability;
    }

    public CompoundTag serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }

    public void deserializeNBT(CompoundTag compoundTag) {
        getOrCreateCapability().deserializeNBT(compoundTag);
    }
}
