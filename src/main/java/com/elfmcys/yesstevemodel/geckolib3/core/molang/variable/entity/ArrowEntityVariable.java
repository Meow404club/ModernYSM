package com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.entity;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.LambdaVariable;
// 1.21.11 Arrow 移 projectile.arrow 子包
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.Arrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.Arrow;

public class ArrowEntityVariable extends LambdaVariable<Arrow> {
    public ArrowEntityVariable(IValueEvaluator<?, IContext<Arrow>> evaluator) {
        super(evaluator);
    }

    @Override
    public boolean validateContext(IContext<?> context) {
        return context.entity() instanceof Arrow;
    }
}