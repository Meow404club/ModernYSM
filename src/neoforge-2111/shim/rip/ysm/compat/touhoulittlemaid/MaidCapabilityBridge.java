// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.touhoulittlemaid;

import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class MaidCapabilityBridge {

    private MaidCapabilityBridge() {
    }

    public static Optional<Object> get(Entity entity) {
        return Optional.empty();
    }
}
