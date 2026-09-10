package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.math;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
// RandomSource 为 1.17+ 类；1.16.5 用 java.util.Random
//? if <1.17 {
// import java.util.Random;
//? } else {
import net.minecraft.util.RandomSource;
//? }

public class DieRollInteger extends ContextFunction<Object> {
    @Override
    public boolean validateArgumentSize(int size) {
        return size == 3;
    }

    @Override
    protected Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        int i = Math.round(arguments.getAsFloat(context, 0));
        int min = arguments.getAsInt(context, 1);
        int range = arguments.getAsInt(context, 2);
        if(min > range) {
            int temp = min;
            min = range;
            range = temp - range;
        } else {
            range -= min;
        }
        int total = 0;
        //? if <1.17
        // Random rnd = context.entity().random();
        //? if >=1.17
        RandomSource rnd = context.entity().random();
        while (i-- > 0) {
            total += min + rnd.nextInt(range);
        }
        return total;
    }
}