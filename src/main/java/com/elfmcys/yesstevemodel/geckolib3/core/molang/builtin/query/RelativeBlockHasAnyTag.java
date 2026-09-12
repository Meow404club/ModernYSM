package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if <1.18.2 {
// import net.minecraft.tags.BlockTags;
// import net.minecraft.tags.Tag;
//? }
// Registries 常量类 1.19.3+；1.17~1.19.2 用 Registry.BLOCK_REGISTRY（1182 Registry.java 实证）
//? if >=1.18.2 && <1.19.4 {
/*import net.minecraft.tags.TagKey;
import net.minecraft.core.Registry;
 *///?}
//? if >=1.19.4 {
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
//?}
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class RelativeBlockHasAnyTag extends EntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        BlockState block = MolangUtils.getRelativeBlockState(context, arguments);
        if (block == null) {
            return null;
        }
        for (int i = 3; i < arguments.size(); i++) {
            ResourceLocation key = arguments.getResourceLocation(context, i);
            if (key == null) {
                return null;
            }
            //? if <1.18.2
            // if (block.is((Tag<net.minecraft.world.level.block.Block>) BlockTags.getAllTags().getTagOrEmpty(key))) {
            //? if <1.18.2
                // return true;
            //? if <1.18.2
            // }
            //? if >=1.18.2 && <1.19.4 {
            /*if (block.is(TagKey.create(net.minecraft.core.Registry.BLOCK_REGISTRY, key))) {
                return true;
            }
             *///?}
            //? if >=1.19.4 {
            if (block.is(TagKey.create(Registries.BLOCK, key))) {
                return true;
            }
            //?}
        }
        return false;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 4;
    }
}
