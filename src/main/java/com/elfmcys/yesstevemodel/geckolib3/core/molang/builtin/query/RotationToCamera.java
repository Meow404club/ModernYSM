package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.ContextFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

public class RotationToCamera extends ContextFunction<Object> {
    @Override
    public Object eval(ExecutionContext<IContext<Object>> context, ArgumentCollection arguments) {
        int args = arguments.getAsInt(context, 0);
        if (args < 0 || args > 1) {
            return null;
        }
//? if <26.2
        Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
//? if >=26.2
        /*Camera mainCamera = Minecraft.getInstance().gameRenderer.mainCamera();*/
        if (args == 0) {
            //? if >=21.11
            /*return -mainCamera.xRot();*/
            //? if <21.11
            return -mainCamera.getXRot();
        }
        //? if >=21.11
        /*return 180.0f + mainCamera.yRot();*/
        //? if <21.11
        return 180.0f + mainCamera.getYRot();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}