package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.util.YsmTag;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class DumpRelativeBlock extends EntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        BlockState blockState;
        ResourceLocation key;
        if (!context.entity().isDebugMode() || (blockState = MolangUtils.getRelativeBlockState(context, arguments)) == null || (key = YsmTag.blockKey(blockState.getBlock())) == null) {
            return null;
        }
        context.entity().logWarningComponent(YsmText.literal("Display ").append(copyOnClickTextCompat(blockState.getBlock().getName().getString(99))));
        context.entity().logWarningComponent(YsmText.literal("Name ").append(copyOnClickTextCompat(key.toString())));
        //? if <1.17 {
        /*net.minecraft.tags.BlockTags.getAllTags().getMatchingTags(blockState.getBlock()).forEach(tagRl ->
            context.entity().logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagRl.toString()))));*/
        //?}
        //? if >=1.18.2 {
        blockState.getTags().forEach(tagKey -> {
            context.entity().logWarningComponent(YsmText.literal("Tag ").append(copyOnClickTextCompat(tagKey.location().toString())));
        });
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
        return size == 3;
    }
}
