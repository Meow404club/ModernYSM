package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//? }
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
//? if >=1.19.3 {
import net.minecraft.core.Holder;
//? }
import net.minecraft.resources.ResourceLocation;
//? if >=1.19.3 {
import net.minecraft.tags.TagKey;
//? }
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;

public class BiomeHasAllTags extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        Entity entity = context.entity().entity();
        // 1.16.5 无 biome 标签体系：每个 tag 均视为不匹配 → 有参即 false，无参（空集全称量词）为 true
        //? if <1.19.3 {
        // entity.level.getBiome(entity.blockPosition());
        // for (int i = 0; i < arguments.size(); i++) {
            // ResourceLocation id = arguments.getResourceLocation(context, i);
            // if (id == null) {
                // return null;
            // }
        // }
        // return arguments.size() == 0;
        //?} else {
        //? if >=1.19.3 && <1.20
        /*Holder<Biome> biome = entity.getLevel().getBiome(entity.blockPosition());*/
        //? if >=1.20
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        for (int i = 0; i < arguments.size(); i++) {
            ResourceLocation id = arguments.getResourceLocation(context, i);
            if (id == null) {
                return null;
            }
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
            if (!biome.is(tag)) {
                return false;
            }
        }
        return true;
        //? }
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }
}
