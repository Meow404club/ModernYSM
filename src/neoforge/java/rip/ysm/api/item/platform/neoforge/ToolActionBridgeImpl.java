package rip.ysm.api.item.platform.neoforge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;

/**
 * neoforge 孪生（异包；RAW 源集）。ToolActions 包移 net.minecraftforge.common →
 * net.neoforged.neoforge.common（FISHING_ROD_CAST 常量 20.4.251 javap 实证存在）。
 */
public final class ToolActionBridgeImpl {

    private ToolActionBridgeImpl() {
    }

    public static boolean canFishingRodCast(ItemStack stack) {
        return stack.canPerformAction(net.neoforged.neoforge.common.ToolActions.FISHING_ROD_CAST);
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return stack.onEntitySwing(entity);
    }
}
