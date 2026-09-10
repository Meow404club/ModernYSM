package rip.ysm.compat.swem;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import rip.ysm.compat.swem.platform.forge.SWEMCompatImpl;

public final class SWEMCompat {

    private SWEMCompat() {
    }

    public static boolean isLoaded() {
        return SWEMCompatImpl.isLoaded();
    }

    public static String getHorseGaitName(LivingEntity livingEntity) {
        return SWEMCompatImpl.getHorseGaitName(livingEntity);
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        SWEMCompatImpl.registerControllerFunctions(ctrlBinding);
    }
}
