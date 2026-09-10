package rip.ysm.compat.immersivemelodies;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import rip.ysm.compat.immersivemelodies.platform.forge.ImmersiveMelodiesCompatImpl;

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
        return ImmersiveMelodiesCompatImpl.isLoaded();
    }

    public static void updateMelodyProgress(LivingEntity livingEntity, ImmersiveMelodiesData imData) {
        ImmersiveMelodiesCompatImpl.updateMelodyProgress(livingEntity, imData);
    }

    public static void registerBindings(CtrlBinding binding) {
        ImmersiveMelodiesCompatImpl.registerBindings(binding);
    }
}
