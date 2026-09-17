// 26.2 代 shim 副本（2111 shim 分代；SlashBladeRenderer 26.2 分歧=collector 形签名，
// mod-absent 零副作用语义同 1.16.5 原件，见 BetterCombatCompat 头注）。
// 渲染入口零副作用：mod 缺席时无刀挂渲染，调用方 isSlashBladeItem 守卫本就不放行。
package rip.ysm.compat.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeRenderer {

    private SlashBladeRenderer() {
    }

    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight, ItemStack stack, float partialTick) {
    }

    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight, ItemStack stack) {
    }
}
