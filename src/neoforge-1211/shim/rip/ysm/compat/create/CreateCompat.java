// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.create;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;

public final class CreateCompat {

    private CreateCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isPlayerOnCreateContraption(Player player) {
        return false;
    }

    public static void registerCreateFunctions(CtrlBinding binding) {
    }
}
