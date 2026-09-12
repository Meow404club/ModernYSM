package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if <1.17 {
/*import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
 *///?}
//? if >=1.17 && <1.19.4 {
/*import net.minecraft.tags.TagKey;
 *///?}
//? if >=1.19.4 {
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
//?}
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RelativeBlockHasAllTags extends EntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        BlockState block = MolangUtils.getRelativeBlockState(context, arguments);
        if (block == null) {
            return null;
        }
        for (int i = 3; i < arguments.size(); i++) {
            ResourceLocation tagId = arguments.getResourceLocation(context, i);
            if (tagId == null) {
                return null;
            }

            //? if <1.17 {
            /*Tag<net.minecraft.world.level.block.Block> tag = (Tag<net.minecraft.world.level.block.Block>) BlockTags.getAllTags().getTagOrEmpty(tagId);
            if (!block.is(tag)) {
                return false;
            }
             *///?}
            //? if >=1.17 && <1.19.4 {
            /*
            TagKey<Block> tag = TagKey.create(net.minecraft.core.Registry.BLOCK_REGISTRY, tagId);
            if (!block.is(tag)) {
                return false;
            }
             *///?}
            //? if >=1.19.4 {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
            if (!block.is(tag)) {
                return false;
            }
            //? }
        }
        return true;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 4;
    }
}
