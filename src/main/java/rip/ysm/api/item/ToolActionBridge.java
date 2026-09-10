package rip.ysm.api.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.api.item.platform.forge.ToolActionBridgeImpl;

public final class ToolActionBridge {

    private ToolActionBridge() {
    }

    public static boolean canFishingRodCast(ItemStack stack) {
        return ToolActionBridgeImpl.canFishingRodCast(stack);
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return ToolActionBridgeImpl.onEntitySwing(stack, entity);
    }
}
