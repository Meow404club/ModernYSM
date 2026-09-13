package com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
// 1.21.11 AbstractArrow 移 projectile.arrow 子包
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.AbstractArrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.AbstractArrow;

public class AbstractArrowEntityVariable extends LambdaVariable<AbstractArrow> {
    public AbstractArrowEntityVariable(IValueEvaluator<?, IContext<AbstractArrow>> evaluator) {
        super(evaluator);
    }

    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof AbstractArrow;
    }
}