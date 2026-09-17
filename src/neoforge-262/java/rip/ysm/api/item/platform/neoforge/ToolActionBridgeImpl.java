package rip.ysm.api.item.platform.neoforge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * neoforge 孪生（异包；RAW 源集，1.21.2+ 代，1.21.1 版在 src/neoforge-1211/java）。
 * 1.21.1 ToolActions 更名 ItemAbilities；1.21.2 IItemStackExtension.onEntitySwing
 * 增 InteractionHand 参（neoforge-1.21.3 IItemStackExtension.java:205 实证）。
 */
public final class ToolActionBridgeImpl {

    private ToolActionBridgeImpl() {
    }

    public static boolean canFishingRodCast(ItemStack stack) {
        return stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return stack.onEntitySwing(entity, InteractionHand.MAIN_HAND);
    }
}
