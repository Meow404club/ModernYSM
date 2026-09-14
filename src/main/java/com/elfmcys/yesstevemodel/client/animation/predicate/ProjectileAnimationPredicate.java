package com.elfmcys.yesstevemodel.client.animation.predicate;

import com.elfmcys.yesstevemodel.client.animation.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.GeckoProjectileEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import com.elfmcys.yesstevemodel.util.accessors.ProjectileStateAccessor;
import net.minecraft.world.entity.projectile.Projectile;

public class ProjectileAnimationPredicate implements IAnimationPredicate<GeckoProjectileEntity> {

    public static final String[] ENVIRONMENT_STATES = {"water", "ground", "fly", "fire"};

    @Override
    public PlayState predicate(AnimationEvent<GeckoProjectileEntity> event, ExpressionEvaluator<?> evaluator) {
        Projectile projectile = event.getAnimatable().getEntity();
        if (projectile == null) {
            return PlayState.STOP;
        }
        if (projectile.isInWater()) {
            return IAnimationPredicate.predicate(event, "water");
        }
        if (projectile.isOnFire()) {
            return IAnimationPredicate.predicate(event, "fire");
        }
        // >=21.3 ProjectileStateAccessor.isInGround 被 Mixin 丢弃（与目标 protected 同签名，
        // AbstractArrowEntityMixin 头注）→ inGroundTime>0 近似（滞后一 tick，功能债）
        //? if <21.2 {
        if ((projectile instanceof ProjectileStateAccessor) && ((ProjectileStateAccessor) projectile).isInGround()) {
            return IAnimationPredicate.predicate(event, "ground");
        }
        //?}
        //? if >=21.2 {
        /*if ((projectile instanceof ProjectileStateAccessor) && ((ProjectileStateAccessor) projectile).getInGroundTime() > 0) {
            return IAnimationPredicate.predicate(event, "ground");
        }*/
        //?}
        return IAnimationPredicate.predicate(event, "air");
    }
}