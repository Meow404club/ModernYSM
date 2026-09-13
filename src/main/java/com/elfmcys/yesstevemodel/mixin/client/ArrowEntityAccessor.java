package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.world.effect.MobEffectInstance;
// 1.21.11 Arrow 移 projectile.arrow 子包（neoforge-21.11.45 sources 实证）。
// 本 accessor 仅 <1.20.5 注册（mixins.json >=1.20.5 换 ArrowPotionAccessor），但类型仍须全线可编译
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.Arrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

//? if >=21.11
/*@Mixin({Arrow.class})*/
//? if <21.11
@Mixin({Arrow.class})
public interface ArrowEntityAccessor {
    @Accessor("effects")
    Set<MobEffectInstance> getEffects();
}
