package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if <1.18.2 {
/*import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
 *///?}
//? if >=1.18.2 && <1.19.3 {
/*import net.minecraft.tags.TagKey;
import net.minecraft.core.Registry;
 *///?}
//? if >=1.19.3 {
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
//?}
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class EquipmentItemAnyTag extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        EquipmentSlot slotType = MolangUtils.parseSlotType(context, arguments, 0);
        if (slotType == null) {
            return null;
        }
        ItemStack stack = CosmeticArmorHelper.getArmorItem(context.entity().entity(), slotType);
        if (stack.isEmpty()) {
            return false;
        }
        for (int i = 1; i < arguments.size(); i++) {
            ResourceLocation key = arguments.getResourceLocation(context, i);
            if (key == null) {
                return null;
            }
            //? if <1.17
            // if (stack.getItem().is((Tag<net.minecraft.world.item.Item>) ItemTags.getAllTags().getTagOrEmpty(key))) {
            //? if <1.17
                // return true;
            //? if <1.17
            // }
            //? if >=1.17 && <1.18.2 {
            /*if (net.minecraft.tags.ItemTags.getAllTags().getTagOrEmpty(key).contains(stack.getItem())) {
                return true;
            }
             *///?}
            //? if >=1.18.2 && <1.19.3 {
            /*if (stack.is(TagKey.create(net.minecraft.core.Registry.ITEM_REGISTRY, key))) {
                return true;
            }
             *///?}
            //? if >=1.19.3 {
            if (stack.is(TagKey.create(Registries.ITEM, key))) {
                return true;
            }
            //?}
        }
        return false;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
