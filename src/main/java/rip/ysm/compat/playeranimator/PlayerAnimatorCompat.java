package rip.ysm.compat.playeranimator;

import net.minecraft.client.player.AbstractClientPlayer;
import rip.ysm.compat.playeranimator.platform.forge.PlayerAnimatorCompatImpl;

public final class PlayerAnimatorCompat {

    private PlayerAnimatorCompat() {
    }

    public static boolean isLoaded() {
        return PlayerAnimatorCompatImpl.isLoaded();
    }

    public static boolean isPlayerAnimated(AbstractClientPlayer abstractClientPlayer) {
        return PlayerAnimatorCompatImpl.isPlayerAnimated(abstractClientPlayer);
    }
}
