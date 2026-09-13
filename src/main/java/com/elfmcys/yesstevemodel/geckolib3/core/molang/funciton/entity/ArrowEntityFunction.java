package com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
// 1.21.11 Arrow 移 projectile.arrow 子包
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.Arrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.Arrow;

public abstract class ArrowEntityFunction extends ContextFunction<Arrow> {
    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Arrow;
    }
}