package rip.ysm.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.slashblade.platform.forge.SlashBladeCompatImpl;

public final class SlashBladeCompat {

    private SlashBladeCompat() {
    }

    public static boolean isLoaded() {
        return SlashBladeCompatImpl.isLoaded();
    }

    public static boolean isSlashBladeItem(ItemStack itemStack) {
        return SlashBladeCompatImpl.isSlashBladeItem(itemStack);
    }

    public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
        return SlashBladeCompatImpl.getComboAnimName(event);
    }

    public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
        return SlashBladeCompatImpl.handleSlashBladeAnim(livingEntity, event, str, loopType);
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        SlashBladeCompatImpl.registerControllerFunctions(ctrlBinding);
    }

    public static boolean hasNewApi() {
        return SlashBladeCompatImpl.hasNewApi();
    }
}
