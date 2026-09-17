package rip.ysm.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
 *///?}
import net.minecraft.world.entity.LivingEntity;
//? if neoforge
/*import rip.ysm.api.client.platform.neoforge.RenderLivingBridgeImpl;*/
//? if forge
import rip.ysm.api.client.platform.forge.RenderLivingBridgeImpl;

public final class RenderLivingBridge {

    private RenderLivingBridge() {
    }

    // 1.21.2 render-state 化：NeoForge RenderLivingEvent 头参 entity→renderState
    //（neoforge-1.21.3 RenderLivingEvent.java:32 实证）→ >=1.21.2 增 state 参；
    // renderer 用 raw 型（泛型参数数 2↔3 跨代不同，raw 全代可编译）
    //? if <1.21.2 {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntity entity, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return RenderLivingBridgeImpl.firePre(entity, renderer, partialTick, poseStack, bufferSource, packedLight);
    }
    //?}

    //? if >=1.21.2 && <26.2 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        return RenderLivingBridgeImpl.firePre(state, renderer, partialTick, poseStack, bufferSource, packedLight);
    }*/
    //?}

    // 26.2 collector 形 twin：collector=RenderPlayerEvent 携带值透传（neoforge-26.2
    // RenderPlayerEvent.java:30 实证）；Static 预览路径 null 语义与 21.9+ 一致
    //? if >=26.2 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        return RenderLivingBridgeImpl.firePre(state, renderer, partialTick, poseStack, bufferSource, packedLight);
    }*/

    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        RenderLivingBridgeImpl.firePost(state, renderer, partialTick, poseStack, bufferSource, packedLight);
    }*/
    //?}
    //? if <1.21.2 {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntity entity, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        RenderLivingBridgeImpl.firePost(entity, renderer, partialTick, poseStack, bufferSource, packedLight);
    }
    //?}

    //? if >=1.21.2 && <26.2 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(LivingEntity entity, LivingEntityRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        RenderLivingBridgeImpl.firePost(state, renderer, partialTick, poseStack, bufferSource, packedLight);
    }*/
    //?}
}
