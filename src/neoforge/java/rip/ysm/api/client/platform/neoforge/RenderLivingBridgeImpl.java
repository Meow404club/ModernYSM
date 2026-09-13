package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。MinecraftForge.EVENT_BUS → NeoForge.EVENT_BUS、
 * RenderLivingEvent 同名同构造面（20.4.251 javap 实证）；post 返回值语义（是否取消）与 bus 7.2.0 一致。
 */
public final class RenderLivingBridgeImpl {
    private RenderLivingBridgeImpl() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntity entity, LivingEntityRenderer<?, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        RenderLivingEvent.Pre event = new RenderLivingEvent.Pre(entity, (LivingEntityRenderer) renderer, partialTick, poseStack, bufferSource, packedLight);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntity entity, LivingEntityRenderer<?, ?> renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post(entity, (LivingEntityRenderer) renderer, partialTick, poseStack, bufferSource, packedLight));
    }
}
