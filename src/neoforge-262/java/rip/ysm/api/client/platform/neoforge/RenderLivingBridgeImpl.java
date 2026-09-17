package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 26.2 代孪生（2111 树的 RenderLivingBridgeImpl 26.2 分歧版；全树分代挂载，同 FQCN）。
 * 分歧点：26.2 MultiBufferSource 删（render-dag 换代）→ 桥参直接换代 SubmitNodeCollector
 *（事件构造第五参同型，neoforge-26.2 RenderLivingEvent.java:31 实证），21.11 null 传递
 * 升级为真 collector 透传。
 */
public final class RenderLivingBridgeImpl {
    private RenderLivingBridgeImpl() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        RenderLivingEvent.Pre event = new RenderLivingEvent.Pre(state, renderer, partialTick, poseStack, bufferSource);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post(state, renderer, partialTick, poseStack, bufferSource));
    }
}
