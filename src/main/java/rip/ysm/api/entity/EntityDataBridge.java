package rip.ysm.api.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
//? if neoforge
/*import rip.ysm.api.entity.platform.neoforge.EntityDataBridgeImpl;*/
//? if forge
import rip.ysm.api.entity.platform.forge.EntityDataBridgeImpl;

public final class EntityDataBridge {

    private EntityDataBridge() {
    }

    public static CompoundTag getPersistentData(Entity entity) {
        return EntityDataBridgeImpl.getPersistentData(entity);
    }

    public static boolean shouldRiderSit(Entity vehicle) {
        return EntityDataBridgeImpl.shouldRiderSit(vehicle);
    }
}
