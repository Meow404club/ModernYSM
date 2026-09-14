package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.entity.EntityRenderCache;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
//? if <1.19.3 {
/*import com.mojang.math.Matrix4f;
*///?}
//? if >=1.19.3 {
import org.joml.Matrix4f;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class WorldRendererMixin {
    // renderLevel 描述符仅矩阵参数内部名随版本不同：1.16.5~1.19.2 = com/mojang/math/Matrix4f
    //（vanilla 未内置 JOML，1.19.3 起 = org/joml/Matrix4f），参数序 PoseStack,F,J,Z,Camera,
    // GameRenderer,LightTexture,Matrix4f 三代一致（1165 LevelRenderer.java:1014/1194:294）
    // At 目标两侧同名同描述符：FogRenderer.setupColor(Camera,F,ClientLevel,I,F)V（1165:30）、
    // RenderType.entitySolid(ResourceLocation)（1165 RenderType.java:277）
    //? if <1.17 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V")})
    private void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
     *///?}
    //? if >=1.17 && <1.19.3 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V")})
    private void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
     *///?}
    // 1.20.6 renderLevel 去 PoseStack 头参（vanilla-1.20.6 LevelRenderer.java:904）；1.21.1 再改
    // DeltaTracker 头参（vanilla-1.21.1 LevelRenderer.java:914）
    //? if >=1.19.3 && <1.20.5 {
    @Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V")})
    private void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    //?}
    //? if >=1.20.5 && <1.21 {
    /*@Inject(method = {"renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V")})
    private void renderLevel(float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=1.21 && <21.2 {
    /*@Inject(method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V")})
    private void renderLevel(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    // 1.21.3 renderLevel 头插 GraphicsResourceAllocator（neoforge-21397 LevelRenderer.java:412 实证，
    // 8 参仍带 GameRenderer+LightTexture）；1.21.4 再删 LightTexture（214157/21598 实证，7 参）。
    // FogRenderer.setupColor INVOKE 锚点 21.3 起不存在（21397/214157/21598 三 sources grep 全 0）
    // → 21.3+ 与 21.8+ 同法 HEAD/TAIL 包夹（方法体由共享裸尾承接，@Local 不需要）
    //? if >=21.2 && <21.4 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At("HEAD")})
    private void renderLevel(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=21.4 && <21.6 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At("HEAD")})
    private void renderLevel(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    // 1.21.6+ renderLevel 头参换代（GraphicsResourceAllocator/GpuBufferSlice/Vector4f，
    // 2111 LevelRenderer.java:461-472 实证；21.8 为 9 参（单 Matrix4f 对）、21.10 起 10 参）
    // 且 FogRenderer.setupColor At 锚点不可恃 → 语义改 HEAD/TAIL 包夹（首尾置位/复位）
    //? if >=21.6 && <21.10 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"}, at = {@At("HEAD")})
    private void renderLevel(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice bufferSlice, org.joml.Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (YesSteveModel.isAvailable()) {
            ModelPreviewRenderer.setFirstPersonMode(true);
            EntityRenderCache.tick(deltaTracker.getGameTimeDeltaPartialTick(false));
        }
     *///?}
    //? if >=21.10 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"}, at = {@At("HEAD")})
    private void renderLevel(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, Matrix4f fogMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice bufferSlice, org.joml.Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (YesSteveModel.isAvailable()) {
            ModelPreviewRenderer.setFirstPersonMode(true);
            EntityRenderCache.tick(deltaTracker.getGameTimeDeltaPartialTick(false));
        }
     *///?}
        if (YesSteveModel.isAvailable()) {
            ModelPreviewRenderer.setFirstPersonMode(true);
            //? if <1.21
            EntityRenderCache.tick(partialTick);
            //? if >=1.21
            /*EntityRenderCache.tick(deltaTracker.getGameTimeDeltaPartialTick(false));*/
        }
    }

    //? if <1.17 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")})
    private void renderLevelPost(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
     *///?}
    //? if >=1.17 && <1.19.3 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")})
    private void renderLevelPost(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
     *///?}
    //? if >=1.19.3 && <1.20.5 {
    @Inject(method = {"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")})
    private void renderLevelPost(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    //?}
    //? if >=1.20.5 && <1.21 {
    /*@Inject(method = {"renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")})
    private void renderLevelPost(float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=1.21 && <21.2 {
    /*@Inject(method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")})
    private void renderLevelPost(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=21.2 && <21.4 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At("TAIL")})
    private void renderLevelPost(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=21.4 && <21.6 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At("TAIL")})
    private void renderLevelPost(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, CallbackInfo ci) {
     *///?}
    //? if >=21.6 && <21.10 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"}, at = {@At("TAIL")})
    private void renderLevelPost(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice bufferSlice, org.joml.Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (YesSteveModel.isAvailable()) {
            EntityRenderCache.clear();
            ModelPreviewRenderer.setFirstPersonMode(false);
        }
     *///?}
    //? if >=21.10 {
    /*@Inject(method = {"renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"}, at = {@At("TAIL")})
    private void renderLevelPost(com.mojang.blaze3d.resource.GraphicsResourceAllocator resourceAllocator, net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, Matrix4f fogMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice bufferSlice, org.joml.Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (YesSteveModel.isAvailable()) {
            EntityRenderCache.clear();
            ModelPreviewRenderer.setFirstPersonMode(false);
        }
     *///?}
        if (YesSteveModel.isAvailable()) {
            EntityRenderCache.clear();
            ModelPreviewRenderer.setFirstPersonMode(false);
        }
    }
}
