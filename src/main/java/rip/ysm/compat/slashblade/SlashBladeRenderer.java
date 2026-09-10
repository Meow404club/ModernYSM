package rip.ysm.compat.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.slashblade.platform.forge.SlashBladeRendererImpl;

public final class SlashBladeRenderer {

    private SlashBladeRenderer() {
    }

    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStack stack, float partialTick) {
        SlashBladeRendererImpl.renderOnEntity(entity, model, poseStack, bufferSource, packedLight, stack, partialTick);
    }

    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStack stack) {
        SlashBladeRendererImpl.renderRightWaist(model, poseStack, bufferSource, packedLight, stack);
    }
}
