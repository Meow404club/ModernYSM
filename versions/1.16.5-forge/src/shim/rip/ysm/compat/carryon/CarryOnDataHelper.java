// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
// getCarryType 返回 NONE：调用方 switch 无匹配分支 → PlayState.STOP，与 1.20.1
// 无 CarryOn 时守卫链行为一致（client/compat/carryon 实现的缺席分支）。
package rip.ysm.compat.carryon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
        return false;
    }

    public static CarryType getCarryType(Player player) {
        return CarryType.NONE;
    }
}
