package com.elfmcys.yesstevemodel.geckolib3.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState;
 *///?}
import net.minecraft.world.entity.Entity;

@SuppressWarnings({"unchecked"})
public class AnimationUtils {
    public static float convertTicksToSeconds(float ticks) {
        return ticks / 20;
    }

    public static float convertSecondsToTicks(float seconds) {
        return seconds * 20;
    }

    // 1.21.2 EntityRenderer 泛型 1→2 参（vanilla-1.21.3 EntityRenderer.java:29）
    //? if <1.21.2 {
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
