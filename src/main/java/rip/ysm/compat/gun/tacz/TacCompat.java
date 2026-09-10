package rip.ysm.compat.gun.tacz;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.gun.tacz.platform.forge.TacCompatImpl;

public final class TacCompat {

    private TacCompat() {
    }

    public static boolean isLoaded() {
        return TacCompatImpl.isLoaded();
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        TacCompatImpl.registerControllerFunctions(binding);
    }

    public static void applyItemTransform(ItemStack stack, AnimatedGeoModel model, LivingEntity entity, PoseStack poseStack, int packedLightIn, float partialTicks) {
        TacCompatImpl.applyItemTransform(stack, model, entity, poseStack, packedLightIn, partialTicks);
    }

    public static PlayState handleTaczAnimState(LivingEntity entity, AnimationEvent<? extends LivingAnimatable<?>> event, String animation, ILoopType loopType) {
        return TacCompatImpl.handleTaczAnimState(entity, event, animation, loopType);
    }

    public static PlayState handleGunHoldAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return TacCompatImpl.handleGunHoldAnimState(stack, event);
    }

    public static PlayState handleGunActionAnimState(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event) {
        return TacCompatImpl.handleGunActionAnimState(stack, event);
    }

    public static void handleGunSound(LivingEntity entity, ItemStack stack) {
        TacCompatImpl.handleGunSound(entity, stack);
    }

    public static void handleItemSound(ItemStack stack) {
        TacCompatImpl.handleItemSound(stack);
    }

    public static ResourceLocation getGunTexture(ItemStack stack) {
        return TacCompatImpl.getGunTexture(stack);
    }
}
