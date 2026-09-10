package rip.ysm.compat.playeranimator.platform.forge;

import net.minecraft.client.player.AbstractClientPlayer;
import com.elfmcys.yesstevemodel.client.compat.playeranimator.platform.forge.PlayerAnimatorCompat;

public final class PlayerAnimatorCompatImpl {

    private PlayerAnimatorCompatImpl() {
    }

    public static boolean isLoaded() {
        return PlayerAnimatorCompat.isLoaded();
    }

    public static boolean isPlayerAnimated(AbstractClientPlayer abstractClientPlayer) {
        return PlayerAnimatorCompat.isPlayerAnimated(abstractClientPlayer);
    }
}
