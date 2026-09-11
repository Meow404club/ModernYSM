package rip.ysm.api.item.platform.forge;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FishingRodItem;

public final class ToolActionBridgeImpl {

    private ToolActionBridgeImpl() {
    }

    // ToolActions/canPerformAction 为 1.18+ API（1.16.5 jar 检索 0 命中）：<1.17 以
    // FishingRodItem 类型判定等价替代（vanilla FISHING_ROD_CAST 唯一实现即 FishingRodItem）
    public static boolean canFishingRodCast(ItemStack stack) {
        //? if <1.17 {
        /*return stack.getItem() instanceof FishingRodItem;
         *///?} else {
        return stack.canPerformAction(net.minecraftforge.common.ToolActions.FISHING_ROD_CAST);
        //?}
    }

    public static boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return stack.onEntitySwing(entity);
    }
}
