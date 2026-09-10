// 1.16.5 版本独有 shim（m2-gate-compat）：共享源同名门面的 no-op 替身。
// libs/ 29 jar 全 1.20.1 口径被闸在 1.16.5 构建外，core 代码经本 shim 保持符号可解析；
// 方法体一律返回 "mod 未加载" 语义（isLoaded=false / 空可选 / 零副作用），与 1.20.1
// 守卫链在 mod 缺席时的行为一致。真实实现见 src/main/java/rip/ysm/compat（只读保留）。
package rip.ysm.compat.bettercombat;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;

public final class BetterCombatCompat {

    private BetterCombatCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void registerBindings(CtrlBinding binding) {
    }
}
