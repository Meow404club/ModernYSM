package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.util.YsmTag;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
import com.elfmcys.yesstevemodel.mixin.client.ArrowEntityAccessor;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
// 1.21.11 Arrow 移 projectile.arrow 子包（neoforge-21.11.45 sources 实证）
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.Arrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.Arrow;

public class EffectLevel extends ContextFunction<Entity> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size >= 1;
    }

    @Override
    public Object eval(ExecutionContext<IContext<Entity>> context, ArgumentCollection arguments) {
        int effects = 0;

        for (int i = 0; i < arguments.size(); i++) {
            ResourceLocation effectId = arguments.getResourceLocation(context, i);
            if (effectId != null) {
                MobEffect mobEffect = YsmTag.mobEffect(effectId);
                if (mobEffect != null) {
                    if (context.entity().geoInstance() instanceof PlayerCapability
                            && ((PlayerCapability) context.entity().geoInstance()).isLocalPlayerModel()) {
                        PlayerCapability cap = (PlayerCapability) context.entity().geoInstance();
                        effects += cap.getPositionTracker().getEffectAmplifier(mobEffect);
                    } else if (((IContext<?>)context.entity()).entity() instanceof LivingEntity) {
                        // 1.20.5+ LivingEntity.getEffect 收 Holder<MobEffect>（vanilla-1.20.6:955）
                        // → 注册表内值 wrapAsHolder 取回 canonical reference holder
                        //? if neoforge && >=1.20.5 {
                        /*MobEffectInstance mobEffectInstance = ((LivingEntity)((IContext<?>)context.entity()).entity())
                                .getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffect));*/
                        //?} else {
                        MobEffectInstance mobEffectInstance = ((LivingEntity)((IContext<?>)context.entity()).entity())
                                .getEffect(mobEffect);
                        //?}
                        if (mobEffectInstance != null) {
                            effects += mobEffectInstance.getAmplifier() + 1;
                        }
                    } else {
                        if (!(((IContext<?>)context.entity()).entity() instanceof Arrow)) {
                            return null;
                        }

                        //? if neoforge && >=1.20.5 {
                        /*for (MobEffectInstance mobEffectInstance : ((com.elfmcys.yesstevemodel.mixin.client.ArrowPotionAccessor)((IContext<?>)context.entity()).entity())
                                .ysm$getPotionContents().getAllEffects()) {
                            if (mobEffectInstance.getEffect().value() == mobEffect) {
                                effects += mobEffectInstance.getAmplifier() + 1;
                                break;
                            }
                        }*/
                        //?}
                        //? if forge || neoforge && <1.20.5 {
                        for (MobEffectInstance mobEffectInstance : ((ArrowEntityAccessor)((IContext<?>)context.entity()).entity())
                                .getEffects()) {
                            if (mobEffectInstance.getEffect() == mobEffect) {
                                effects += mobEffectInstance.getAmplifier() + 1;
                                break;
                            }
                        }
                        //?}
                    }
                }
            }
        }

        return effects;
    }
}
