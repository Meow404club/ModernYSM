package rip.ysm.compat.elytraslot.platform.forge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.elytraslot.platform.forge.ElytraSlotCompat;

public final class ElytraSlotCompatImpl {

    private ElytraSlotCompatImpl() {
    }

    public static boolean isLoaded() {
        return ElytraSlotCompat.isLoaded();
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        return ElytraSlotCompat.getElytraItem(livingEntity);
    }
}
