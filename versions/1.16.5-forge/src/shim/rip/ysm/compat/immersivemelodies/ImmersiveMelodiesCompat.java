// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
// ImmersiveMelodiesData 结构体保持字段原样（core 的 LivingEntityFrameState 直接持有），
// 无旋律播放时字段维持零值，updateMelodyProgress 为零副作用。
package rip.ysm.compat.immersivemelodies;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;

public final class ImmersiveMelodiesCompat {

    public static final class ImmersiveMelodiesData {
        public float pitch = 0f;
        public float volume = 0f;
        public float current = 0f;
        public long delta = 0L;
        public long time = 0L;
    }

    private ImmersiveMelodiesCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void updateMelodyProgress(LivingEntity livingEntity, ImmersiveMelodiesData imData) {
    }

    public static void registerBindings(CtrlBinding binding) {
    }
}
