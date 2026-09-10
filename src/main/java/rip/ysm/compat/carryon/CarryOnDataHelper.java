package rip.ysm.compat.carryon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import rip.ysm.compat.carryon.platform.forge.CarryOnDataHelperImpl;

public final class CarryOnDataHelper {

    public enum CarryType {
        ENTITY,
        BLOCK,
        PLAYER,
        NONE
    }

    private CarryOnDataHelper() {
    }

    public static boolean isPlayerCarrying(LivingEntity livingEntity) {
        return CarryOnDataHelperImpl.isPlayerCarrying(livingEntity);
    }

    public static CarryType getCarryType(Player player) {
        return CarryOnDataHelperImpl.getCarryType(player);
    }
}
