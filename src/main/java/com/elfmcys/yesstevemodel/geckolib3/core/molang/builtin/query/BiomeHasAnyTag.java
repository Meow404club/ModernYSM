package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

//? if >= 1.17 {
import net.minecraft.core.registries.Registries;
//? }
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.EntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
//? if >= 1.17 {
import net.minecraft.core.Holder;
//? }
import net.minecraft.resources.ResourceLocation;
//? if >= 1.17 {
import net.minecraft.tags.TagKey;
//? }
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;

public class BiomeHasAnyTag extends EntityFunction {
    @Override
    protected Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        Entity entity = context.entity().entity();
        // 1.16.5 无 Holder/Biome 标签体系（数据包 biome tag 自 1.18 起）：任何 biome 不匹配任何 tag
        //? if < 1.17 {
        // entity.level.getBiome(entity.blockPosition());
        // for (int i = 0; i < arguments.size(); i++) {
            // ResourceLocation id = arguments.getResourceLocation(context, i);
            // if (id == null) {
                // return null;
            // }
        // }
        // return false;
        //? } else {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        for (int i = 0; i < arguments.size(); i++) {
            ResourceLocation id = arguments.getResourceLocation(context, i);
            if (id == null) {
                return null;
            }
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
            if (biome.is(tag)) {
                return true;
            }
        }
        return false;
        //? }
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }
}
