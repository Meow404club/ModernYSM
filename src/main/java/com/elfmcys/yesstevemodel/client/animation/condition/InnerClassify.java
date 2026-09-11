package com.elfmcys.yesstevemodel.client.animation.condition;

import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.slashblade.SlashBladeCompat;
import com.elfmcys.yesstevemodel.util.ItemTagsConstants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

public class InnerClassify {

    private static final String EMPTY = "";

    public static String doClassifyTest(String str, LivingEntity livingEntity, InteractionHand interactionHand) {
        String itemType = getItemType(livingEntity.getItemInHand(interactionHand));
        if (!itemType.equals("")) {
            return str + itemType;
        }
        return "";
    }

    public static String getItemType(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (SlashBladeCompat.isSlashBladeItem(itemStack)) {
            return "slashblade";
        }
        if ((item instanceof SwordItem) || ItemTagsConstants.SWORDS.matches(itemStack)) {
            return "sword";
        }
        if (TouhouLittleMaidCompat.isMaidItem(item)) {
            return "gohei";
        }
        if ((item instanceof AxeItem) || ItemTagsConstants.AXES.matches(itemStack)) {
            return "axe";
        }
        if ((item instanceof PickaxeItem) || ItemTagsConstants.PICKAXES.matches(itemStack)) {
            return "pickaxe";
        }
        if ((item instanceof ShovelItem) || ItemTagsConstants.SHOVELS.matches(itemStack)) {
            return "shovel";
        }
        if ((item instanceof HoeItem) || ItemTagsConstants.HOES.matches(itemStack)) {
            return "hoe";
        }
        if ((item instanceof ShieldItem) || ItemTagsConstants.SHIELDS.matches(itemStack)) {
            return "shield";
        }
        if ((item instanceof CrossbowItem) || ItemTagsConstants.CROSSBOWS.matches(itemStack)) {
            return "crossbow";
        }
        if ((item instanceof BowItem) || ItemTagsConstants.BOWS.matches(itemStack)) {
            return "bow";
        }
        if ((item instanceof FishingRodItem) || ItemTagsConstants.FISHING_RODS.matches(itemStack)) {
            return "fishing_rod";
        }
        if ((item instanceof TridentItem) || ItemTagsConstants.TRIDENTS.matches(itemStack)) {
            return "spear";
        }
        if ((item instanceof ThrowablePotionItem) || ItemTagsConstants.THROWABLE_POTION.matches(itemStack)) {
            return "throwable_potion";
        }
        return "";
    }
}