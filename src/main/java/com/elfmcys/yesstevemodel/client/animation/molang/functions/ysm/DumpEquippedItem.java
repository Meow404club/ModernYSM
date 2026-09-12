package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.util.YsmTag;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class DumpEquippedItem extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slot;
        ResourceLocation key;
        Enchantment enchantment;
        if (!context.entity().isDebugMode() || (slot = MolangUtils.parseSlotType(context, arguments, 0)) == null) {
            return null;
        }
        ItemStack stack = CosmeticArmorHelper.getArmorItem(context.entity().entity(), slot);
        if (stack.isEmpty() || (key = YsmTag.itemKey(stack.getItem())) == null) {
            return null;
        }
        context.entity().logWarningComponent(YsmText.literal("Display ").append(copyOnClickTextCompat(stack.getItem().getName(stack).getString(99))));
        context.entity().logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(key.toString())));
        // getTags()（1.17+）↔ 1.16.5 TagCollection.getMatchingTags(item)；getEnchantmentTags()（1.17+）↔ 1.16.5 NBT 清单
        //? if <1.17 {
        /*net.minecraft.tags.ItemTags.getAllTags().getMatchingTags(stack.getItem()).forEach(tagRl ->
            context.entity().logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagRl.toString()))));
        net.minecraft.nbt.ListTag enchantmentList = stack.getTag() != null ? stack.getTag().getList("Enchantments", 10) : new net.minecraft.nbt.ListTag();
        for (int tagIndex = 0; tagIndex < enchantmentList.size(); tagIndex++) {
            CompoundTag compoundTag = enchantmentList.getCompound(tagIndex);
            ResourceLocation resourceLocationTryParse = ResourceLocation.tryParse(compoundTag.getString("id"));
            if (resourceLocationTryParse != null && (enchantment = YsmTag.enchantment(resourceLocationTryParse)) != null) {
                context.entity().logWarningComponent(YsmText.literal("Enchantment: display ").append(copyOnClickTextCompat(enchantment.getFullname(compoundTag.getInt("lvl")).getString(99))).append(YsmText.literal("  name ").append(copyOnClickTextCompat(resourceLocationTryParse.toString()))));
            }
        }
         *///?} else {
        stack.getTags().forEach(tagKey -> {
            context.entity().logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagKey.location().toString())));
        });
        for (Tag tag : stack.getEnchantmentTags()) {
            if (tag instanceof CompoundTag) {
                CompoundTag compoundTag = (CompoundTag) tag;
                ResourceLocation resourceLocationTryParse = ResourceLocation.tryParse(compoundTag.getString("id"));
                if (resourceLocationTryParse != null && (enchantment = YsmTag.enchantment(resourceLocationTryParse)) != null) {
                    context.entity().logWarningComponent(YsmText.literal("Enchantment: display ").append(copyOnClickTextCompat(enchantment.getFullname(compoundTag.getInt("lvl")).getString(99))).append(YsmText.literal("  name ").append(copyOnClickTextCompat(resourceLocationTryParse.toString()))));
                }
            }
        }
        //?}
        return null;
    }

    /** ComponentUtils.copyOnClickText（1.19.2+）↔ 1.16.5 无 → 原串直返。 */
    private static net.minecraft.network.chat.Component copyOnClickTextCompat(String str) {
        // ComponentUtils.copyOnClickText 1.19.4+：中段+1.16.5 原串直返
        //? if <1.19.4 {
        /*return YsmText.literal(str);
         *///?} else {
        return net.minecraft.network.chat.ComponentUtils.copyOnClickText(str);
        //?}
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}
