package rip.ysm.compat.ironsspellbooks;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import net.minecraft.world.entity.LivingEntity;
import rip.ysm.compat.ironsspellbooks.platform.forge.SpellbooksCompatImpl;

public final class SpellbooksCompat {

    private SpellbooksCompat() {
    }

    public static boolean isLoaded() {
        return SpellbooksCompatImpl.isLoaded();
    }

    public static void registerBindings(CtrlBinding binding) {
        SpellbooksCompatImpl.registerBindings(binding);
    }

    public static PlayState resolvePlayState(AnimationEvent<LivingAnimatable<?>> event, LivingEntity entity) {
        return SpellbooksCompatImpl.resolvePlayState(event, entity);
    }
}
