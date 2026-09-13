// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
// 此处不复刻"装饰护甲覆盖"逻辑，直接走原版盔甲槽（= client/compat 实现在
// CosmeticArmorReworked 缺席时的 fallthrough），行为一致。
package rip.ysm.compat.cosmeticarmorreworked;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticArmorHelper {

    private CosmeticArmorHelper() {
    }

    public static ItemStack getArmorItem(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    public static ItemStack getElytraItem(LivingEntity livingEntity) {
        ItemStack itemBySlot = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (itemBySlot.getItem() == Items.ELYTRA) {
            return itemBySlot;
        }
        return ItemStack.EMPTY;
    }
}
