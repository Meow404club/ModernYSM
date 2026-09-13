package com.elfmcys.yesstevemodel.util;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;


import java.util.concurrent.TimeUnit;

//? if neoforge && <21.9 {
@OnlyIn(Dist.CLIENT)
//?}
//? if forge {
/*@OnlyIn(Dist.CLIENT)*/
//?}
public final class AnimatableCacheUtil {
    public static final Cache<ResourceLocation, Entity> ENTITIES_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
}