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
        // 1.21.5 SwordItem/PickaxeItem 类删除（1.21.5 world/item 无此二类，斧铲锄仍在）：
        // instanceof 半支退役，物品标签判定语义不变
        //? if <21.5
        if ((item instanceof SwordItem) || ItemTagsConstants.SWORDS.matches(itemStack)) {
        //? if >=21.5
        /*if (ItemTagsConstants.SWORDS.matches(itemStack)) {*/
            return "sword";
        }
        if (TouhouLittleMaidCompat.isMaidItem(item)) {
            return "gohei";
        }
        // 26.3 AxeItem/ShovelItem/HoeItem 类删除（工具组件化收尾，/tmp/vanilla-263
        // world/item 无此类；同 21.5 SwordItem/PickaxeItem 删除先例）→ 仅标签判定
        //? if <26.3
        if ((item instanceof AxeItem) || ItemTagsConstants.AXES.matches(itemStack)) {
        //? if >=26.3
        /*if (ItemTagsConstants.AXES.matches(itemStack)) {*/
            return "axe";
        }
        //? if <21.5
        if ((item instanceof PickaxeItem) || ItemTagsConstants.PICKAXES.matches(itemStack)) {
        //? if >=21.5
        /*if (ItemTagsConstants.PICKAXES.matches(itemStack)) {*/
            return "pickaxe";
        }
        //? if <26.3
        if ((item instanceof ShovelItem) || ItemTagsConstants.SHOVELS.matches(itemStack)) {
        //? if >=26.3
        /*if (ItemTagsConstants.SHOVELS.matches(itemStack)) {*/
            return "shovel";
        }
        //? if <26.3
        if ((item instanceof HoeItem) || ItemTagsConstants.HOES.matches(itemStack)) {
        //? if >=26.3
        /*if (ItemTagsConstants.HOES.matches(itemStack)) {*/
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