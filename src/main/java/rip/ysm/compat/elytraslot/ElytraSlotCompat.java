package rip.ysm.compat.elytraslot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.elytraslot.platform.forge.ElytraSlotCompatImpl;

public final class ElytraSlotCompat {

    private ElytraSlotCompat() {
    }

    public static boolean isLoaded() {
        return ElytraSlotCompatImpl.isLoaded();
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        return ElytraSlotCompatImpl.getElytraItem(livingEntity);
    }
}
