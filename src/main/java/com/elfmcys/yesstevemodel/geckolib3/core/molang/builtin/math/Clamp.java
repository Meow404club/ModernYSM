package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.math;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
// 1.12.2 无 Mth（mojmap 1.14+）：走 legacy122 MthCompat 委托 vanilla MathHelper
//? if >=1.14 {
import net.minecraft.util.Mth;
//? }
//? if <1.14 {
/*import rip.ysm.legacy122.Mth;*/
//? }

public class Clamp implements Function {
    @Override
    public Object evaluate(ExecutionContext<?> context, ArgumentCollection arguments) {
        return Mth.clamp(arguments.getAsFloat(context, 0),
                arguments.getAsFloat(context, 1),
                arguments.getAsFloat(context, 2));
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }
}