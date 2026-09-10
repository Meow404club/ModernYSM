package rip.ysm.compat.cosmeticarmorreworked;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.cosmeticarmorreworked.platform.forge.CosmeticArmorHelperImpl;

public final class CosmeticArmorHelper {

    private CosmeticArmorHelper() {
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return CosmeticArmorHelperImpl.getArmorItem(entity, slot);
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        return CosmeticArmorHelperImpl.getElytraItem(livingEntity);
    }
}
