package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.21.2+ 代）。1.21.2 render-state 化：
 * RenderLivingEvent 构造头参 entity→renderState（neoforge-1.21.3
 * RenderLivingEvent.java:32/96 实证）；实体引用由共享桥调用方随行。
 */
public final class RenderLivingBridgeImpl {
    private RenderLivingBridgeImpl() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        RenderLivingEvent.Pre event = new RenderLivingEvent.Pre(state, renderer, partialTick, poseStack, bufferSource, packedLight);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post(state, renderer, partialTick, poseStack, bufferSource, packedLight));
    }
}
