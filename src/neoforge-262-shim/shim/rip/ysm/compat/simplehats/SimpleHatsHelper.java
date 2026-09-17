// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.simplehats;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SimpleHatsHelper {

    private SimpleHatsHelper() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static ItemStack getHatItem(LivingEntity livingEntity) {
        return ItemStack.EMPTY;
    }
}
