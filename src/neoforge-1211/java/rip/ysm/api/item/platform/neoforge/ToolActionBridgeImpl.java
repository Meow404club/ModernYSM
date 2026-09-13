package rip.ysm.api.item.platform.neoforge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * neoforge 孪生（异包；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.21.1 ToolActions 更名 ItemAbilities（neoforge-1.21.1 common/ItemAbilities.java:128
 * FISHING_ROD_CAST 实证；1.20.6 无此类，故本孪生入 1205 分代树）。
 */
public final class ToolActionBridgeImpl {

    private ToolActionBridgeImpl() {
    }

    public static boolean canFishingRodCast(ItemStack stack) {
        return stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST);
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return stack.onEntitySwing(entity);
    }
}
