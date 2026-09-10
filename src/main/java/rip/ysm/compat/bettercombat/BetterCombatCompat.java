package rip.ysm.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import rip.ysm.compat.bettercombat.platform.forge.BetterCombatCompatImpl;

public final class BetterCombatCompat {

    private BetterCombatCompat() {
    }

    public static boolean isLoaded() {
        return BetterCombatCompatImpl.isLoaded();
    }

    public static void registerBindings(CtrlBinding binding) {
        BetterCombatCompatImpl.registerBindings(binding);
    }
}
