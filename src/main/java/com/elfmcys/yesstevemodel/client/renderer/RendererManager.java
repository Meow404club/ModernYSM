package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//? if >=1.17 {
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? }
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.17 && neoforge
/*import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;*/
//? if >=1.17 && forge
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
// RegisterClientReloadListenersEvent 为 1.16.5 所无（1.17+），1.16.5 分支在
// FMLClientSetupEvent 直接向 ReloadableResourceManager 挂 ResourceManagerReloadListener
//? if neoforge
/*import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;*/
//? if forge
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//? if neoforge
/*import net.neoforged.bus.api.SubscribeEvent;*/
//? if forge
import net.minecraftforge.eventbus.api.SubscribeEvent;
//? if neoforge && >=1.20.5
/*import net.neoforged.fml.common.EventBusSubscriber;*/
//? if neoforge && <1.20.5
/*import net.neoforged.fml.common.Mod;*/
//? if forge
import net.minecraftforge.fml.common.Mod;
import rip.ysm.compat.sbackpack.SBackpackCompat;

//? if neoforge && >=1.20.5
/*@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)*/
//? if neoforge && <1.20.5
/*@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)*/
//? if forge
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RendererManager {

    private static CustomPlayerRenderer playerRenderer;

    private static ProjectileRenderer projectileRenderer;

    private static HandItemRenderer handRenderer;

    private static VehicleRenderer vehicleRenderer;

    private RendererManager() {
    }

    /**
     * 原 architectury ReloadListenerRegistry.register(CLIENT_RESOURCES, ...) 在 forge 端是直接
     * 向 Minecraft 的 ReloadableResourceManager.registerReloadListener 挂接；
     * Forge 正规入口为 RegisterClientReloadListenersEvent（构造后、首轮资源重载前触发，落到同一管理器）。
     */
    //? if <1.17 {
    // @SubscribeEvent
    // public static void onClientSetup(FMLClientSetupEvent event) {
    //     event.enqueueWork(() -> {
    //         ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
    //         ((net.minecraft.server.packs.resources.ReloadableResourceManager) net.minecraft.client.Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);
    //     });
    // }
    //? } else {
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        event.registerReloadListener(listener);
    }
    //? }

    private static void resetRenderers() {
        playerRenderer = null;
        projectileRenderer = null;
        handRenderer = null;
        vehicleRenderer = null;
    }

    private static void initRenderers(ResourceManager resourceManager) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        // 1.16.5 无 EntityRendererProvider/Context（1.17+）：渲染器直接吃 dispatcher，
        // bakeLayer 类模型走传统构造（各渲染器 ctor 已条件化）
        //? if <1.17 {
        // playerRenderer = new CustomPlayerRenderer(entityRenderDispatcher);
        // projectileRenderer = new ProjectileRenderer(entityRenderDispatcher);
        // handRenderer = new HandItemRenderer();
        // vehicleRenderer = new VehicleRenderer(entityRenderDispatcher);
        //? } else {
        // EntityRendererProvider.Context 构造：1.19.2 = 7 参（含 blockRenderer/itemInHandRenderer）；
        // 1.17~1.18.2 = 5 参（1182 EntityRendererProvider.java:21），itemInHand 手工补
        // 1.21.2 Context 构造 8 参（+MapRenderer/EquipmentModelSet，去 itemInHandRenderer，
        // vanilla-1.21.3 EntityRendererProvider.java:35 实证）
        //? if >=1.19.2 && <1.21.2
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), Minecraft.getInstance().getBlockRenderer(), entityRenderDispatcher.getItemInHandRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().font);
        //? if >=1.21.2
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().getEquipmentModels(), Minecraft.getInstance().font);*/
        //? if <1.19.2
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().font);*/
        playerRenderer = new CustomPlayerRenderer(context);
        projectileRenderer = new ProjectileRenderer(context);
        handRenderer = new HandItemRenderer();
        vehicleRenderer = new VehicleRenderer(context);
        //? }
        SBackpackCompat.setupRenderLayers();
    }

    public static CustomPlayerRenderer getPlayerRenderer() {
        if (playerRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return playerRenderer;
    }

    public static ProjectileRenderer getProjectileRenderer() {
        if (projectileRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return projectileRenderer;
    }

    public static HandItemRenderer getHandRenderer() {
        if (handRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return handRenderer;
    }

    public static VehicleRenderer getVehicleRenderer() {
        if (vehicleRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return vehicleRenderer;
    }
}
