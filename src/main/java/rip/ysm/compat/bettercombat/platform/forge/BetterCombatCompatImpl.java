package rip.ysm.compat.bettercombat.platform.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.bettercombat.platform.forge.BetterCombatCompat;

public final class BetterCombatCompatImpl {

    private BetterCombatCompatImpl() {
    }

    public static boolean isLoaded() {
        return BetterCombatCompat.isLoaded();
    }

    public static void registerBindings(CtrlBinding binding) {
        BetterCombatCompat.registerBindings(binding);
    }
}
