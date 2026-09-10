package rip.ysm.compat.simplehats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.simplehats.platform.forge.SimpleHatsHelperImpl;

public final class SimpleHatsHelper {

    private SimpleHatsHelper() {
    }

    public static boolean isLoaded() {
        return SimpleHatsHelperImpl.isLoaded();
    }

    public static ItemStack getHatItem(LivingEntity livingEntity) {
        return SimpleHatsHelperImpl.getHatItem(livingEntity);
    }
}
