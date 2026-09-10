package rip.ysm.compat.swem.platform.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import com.elfmcys.yesstevemodel.client.compat.swem.platform.forge.SWEMCompat;

public final class SWEMCompatImpl {

    private SWEMCompatImpl() {
    }

    public static boolean isLoaded() {
        return SWEMCompat.isLoaded();
    }

    public static String getHorseGaitName(LivingEntity livingEntity) {
        return SWEMCompat.getHorseGaitName(livingEntity);
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        SWEMCompat.registerControllerFunctions(ctrlBinding);
    }
}
