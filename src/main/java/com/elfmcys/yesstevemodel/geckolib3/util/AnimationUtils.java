package com.elfmcys.yesstevemodel.geckolib3.util;

import net.minecraft.client.Minecraft;
// 1.12.2 无本块四类（RenderManager 代 EntityRenderDispatcher/Render 代 EntityRenderer/
// Entity→net.minecraft.entity.Entity）：<1.14 分支 getRenderer 全 FQN 引用，无需 import
//? if >=1.14 {
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
//?}
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
 *///?}

@SuppressWarnings({"unchecked"})
public class AnimationUtils {
    public static float convertTicksToSeconds(float ticks) {
        return ticks / 20;
    }

    public static float convertSecondsToTicks(float seconds) {
        return seconds * 20;
    }

    // 1.12.2：RenderManager.getEntityRenderObject → Render（vanilla-mc-1122
    // RenderManager.java:243 + Minecraft.java:2251 getMinecraft/:2615 getRenderManager 实证，
    // compile jar javap 复核）；本仓 getRenderer 消费面仅 json 三件套的 convertSecondsToTicks
    // 静态导入，getRenderer 各代分支保形不保调。
    //? if <1.14 {
    /*public static <T extends net.minecraft.entity.Entity> net.minecraft.client.renderer.entity.Render<T> getRenderer(T entity) {
        return net.minecraft.client.Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
    }*/
    //?}
    // 1.21.2 EntityRenderer 泛型 1→2 参（vanilla-1.21.3 EntityRenderer.java:29）
    //? if >=1.14 && <1.21.2 {
    public static <T extends Entity> EntityRenderer<T> getRenderer(T entity) {
        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        return (EntityRenderer<T>) renderManager.getRenderer(entity);
    }
    //?}

    //? if >=1.21.2 {
    /*public static <T extends Entity> EntityRenderer<T, EntityRenderState> getRenderer(T entity) {
        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        return (EntityRenderer<T, EntityRenderState>) renderManager.getRenderer(entity);
    }*/
    //?}
}
