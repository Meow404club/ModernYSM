package rip.ysm.api.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.entity.platform.forge.EntityDataBridgeImpl;

public final class EntityDataBridge {

    private EntityDataBridge() {
    }

    @ExpectPlatform
    public static CompoundTag getPersistentData(Entity entity) {
        return EntityDataBridgeImpl.getPersistentData(entity);
    }

    @ExpectPlatform
    public static boolean shouldRiderSit(Entity vehicle) {
        return EntityDataBridgeImpl.shouldRiderSit(vehicle);
    }
}
