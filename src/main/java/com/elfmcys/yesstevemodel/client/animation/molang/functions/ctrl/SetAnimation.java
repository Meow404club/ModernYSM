package com.elfmcys.yesstevemodel.client.animation.molang.functions.ctrl;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.PredicateBasedController;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import org.apache.commons.lang3.StringUtils;

public class SetAnimation extends ContextFunction<Object> {
    @Override
    public Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        PredicateBasedController<?> animationController = context.entity().animationEvent().getController();
        if (animationController == null) {
            return null;
        }
        String animationName = arguments.getAsString(context, 0);
        if (StringUtils.isEmpty(animationName)) {
            return null;
        }
        ILoopType loopType;
        if (arguments.size() == 1) {
            loopType = null;
        } else {
            switch (arguments.getAsInt(context, 1)) {
                case 10:
                    loopType = ILoopType.EDefaultLoopTypes.LOOP;
                    break;
                case 11:
                    loopType = ILoopType.EDefaultLoopTypes.PLAY_ONCE;
                    break;
                case 12:
                    loopType = ILoopType.EDefaultLoopTypes.HOLD_ON_LAST_FRAME;
                    break;
                default:
                    loopType = null;
                    break;
            }
        }
        animationController.setAnimation(animationName, loopType);
        return null;
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1 || size == 2;
    }
}