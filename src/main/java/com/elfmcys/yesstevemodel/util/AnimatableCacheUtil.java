package com.elfmcys.yesstevemodel.util;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
// 1.12.2 forge 无 net.minecraftforge.api.distmarker 包（SideOnly 代差，1.13+ 才有）
//? if forge && >=1.13
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge && >=1.13
import net.minecraftforge.api.distmarker.OnlyIn;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
//? if >=1.14
import net.minecraft.resources.ResourceLocation;
//? if <1.14
/*import net.minecraft.util.ResourceLocation;*/
//? if >=1.13
import net.minecraft.world.entity.Entity;
//? if <1.13
/*import net.minecraft.entity.Entity;*/


import java.util.concurrent.TimeUnit;

//? if neoforge && <21.9 {
@OnlyIn(Dist.CLIENT)
//?}
//? if forge && >=1.13 {
/*@OnlyIn(Dist.CLIENT)*/
//?}
public final class AnimatableCacheUtil {
    public static final Cache<ResourceLocation, Entity> ENTITIES_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
}