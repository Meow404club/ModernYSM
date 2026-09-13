package rip.ysm.api.entity.platform.neoforge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * neoforge 孪生（异包；RAW 源集）。getPersistentData 为 neoforge IEntityExtension 默认方法
 * （20.4.251 javap 实证）；shouldRiderSit 同在 IEntityExtension（forge Entity 直挂 →
 * neoforge 经扩展接口同名默认方法，语义一致）。
 */
public final class EntityDataBridgeImpl {

    private EntityDataBridgeImpl() {
    }

    public static CompoundTag getPersistentData(Entity entity) {
        return entity.getPersistentData();
    }

    public static boolean shouldRiderSit(Entity vehicle) {
        return vehicle.shouldRiderSit();
    }
}
