package com.elfmcys.yesstevemodel.mixin.client;

// 1.21.11 ThrowableItemProjectile 移 projectile.throwableitemprojectile 子包
//（neoforge-21.11.45 sources 实证；21.10 同包未动）
//? if >=21.11
/*import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;*/
//? if <21.11
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >=21.11
/*@Mixin({ThrowableItemProjectile.class})*/
//? if <21.11
@Mixin({ThrowableItemProjectile.class})
public interface ThrowableItemProjectileAccessor {
    @Invoker("getDefaultItem")
    Item invokeGetDefaultItem();
}
