package rip.ysm.compat.create;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.player.Player;
import rip.ysm.compat.create.platform.forge.CreateCompatImpl;

public final class CreateCompat {

    private CreateCompat() {
    }

    public static boolean isLoaded() {
        return CreateCompatImpl.isLoaded();
    }

    public static boolean isPlayerOnCreateContraption(Player player) {
        return CreateCompatImpl.isPlayerOnCreateContraption(player);
    }

    public static void registerCreateFunctions(CtrlBinding binding) {
        CreateCompatImpl.registerCreateFunctions(binding);
    }
}
