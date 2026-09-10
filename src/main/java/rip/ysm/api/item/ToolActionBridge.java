package rip.ysm.api.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.api.item.platform.forge.ToolActionBridgeImpl;

public final class ToolActionBridge {

    private ToolActionBridge() {
    }

    @ExpectPlatform
    public static boolean canFishingRodCast(ItemStack stack) {
        return ToolActionBridgeImpl.canFishingRodCast(stack);
    }

    @ExpectPlatform
    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return ToolActionBridgeImpl.onEntitySwing(stack, entity);
    }
}
