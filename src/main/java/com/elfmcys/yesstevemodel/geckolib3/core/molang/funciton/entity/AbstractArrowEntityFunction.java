package com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
// 1.21.11 AbstractArrow 移 projectile.arrow 子包
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.AbstractArrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.AbstractArrow;

public abstract class AbstractArrowEntityFunction extends ContextFunction<AbstractArrow> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractArrow;
    }
}