package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if <1.17 {
// import net.minecraft.core.Registry;
//? }
//? if >=1.17 && <1.19.4 {
/*import net.minecraft.core.Registry;
 *///?}
//? if >=1.19.4 {
import net.minecraft.core.registries.Registries;
//?}
import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
//? if >=1.19.4 {
import net.minecraft.core.registries.BuiltInRegistries;
//? }
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class IsItemNameAny extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        ResourceLocation key;
        EquipmentSlot slotType = MolangUtils.parseSlotType(context, arguments, 0);
        if (slotType == null) {
            return null;
        }
        ItemStack stack = CosmeticArmorHelper.getArmorItem(context.entity().entity(), slotType);
        //? if <1.19.4
        // if (!stack.isEmpty() && (key = Registry.ITEM.getKey(stack.getItem())) != null) {
        //? if >=1.19.4
        if (!stack.isEmpty() && (key = BuiltInRegistries.ITEM.getKey(stack.getItem())) != null) {
            for (int i = 1; i < arguments.size(); i++) {
                ResourceLocation location = arguments.getResourceLocation(context, i);
                if (location == null) {
                    return null;
                }
                if (location.equals(key)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 2;
    }
}
