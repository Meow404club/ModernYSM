// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
// getHorseGaitName 返回 null 与共享实现缺席分支一致（调用方判空）。
package rip.ysm.compat.swem;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class SWEMCompat {

    private SWEMCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    @Nullable
    public static String getHorseGaitName(LivingEntity livingEntity) {
        return null;
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
    }
}
