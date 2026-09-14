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
//? if >=1.17 && neoforge && <21.4
/*import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;*/
//? if >=1.17 && neoforge && >=21.4 {
/*import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
 *///?}
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

// 1.21.6+ EventBusSubscriber 删 bus 属性
//? if neoforge && >=1.20.5 && <21.6
/*@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)*/
//? if neoforge && >=21.6
/*@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)*/
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
    // 1.16.1 forge 32 无 enqueueWork/ParallelDispatchEvent（1.16.2+ 实证）→
    // DeferredWorkQueue.runLater 同主线程排队语义。下方 <1.17 段的 // 行在活跃分支会被
    // stonecutter 剥前缀成真码（1.16.5 现状行为），故 1.16.1 需独立分支承接。
    //? if <1.16.2 {
    /*@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.fml.DeferredWorkQueue.runLater(() -> {
            ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
            ((net.minecraft.server.packs.resources.ReloadableResourceManager) net.minecraft.client.Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);
        });
    }
     *///?}
    //? if >=1.16.2 && <1.17 {
    // @SubscribeEvent
    // public static void onClientSetup(FMLClientSetupEvent event) {
    //     event.enqueueWork(() -> {
    //         ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
    //         ((net.minecraft.server.packs.resources.ReloadableResourceManager) net.minecraft.client.Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);
    //     });
    // }
    //?}
    //? if forge && >=1.17 {
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        event.registerReloadListener(listener);
    }
    //?}
    // 1.21.4 RegisterClientReloadListenersEvent 删除（neoforge-1.21.4 无此类，
    // AddClientReloadListenersEvent 接管，addListener 需显式 RL key）
    //? if neoforge && >=1.17 && <21.4 {
    /*@SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        event.registerReloadListener(listener);
    }*/
    //?}
    //? if neoforge && >=1.17 && >=21.4 {
    /*@SubscribeEvent
    public static void onRegisterReloadListeners(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) {
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        event.addListener(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "renderer_manager_reset"), listener);
    }*/
    //?}

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
        //? if >=21.2 && <21.4
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().getEquipmentModels(), Minecraft.getInstance().font);*/
        // 1.21.4 Context：ItemRenderer→ItemModelResolver、EquipmentModelSet→EquipmentAssetManager
        //（vanilla-1.21.4 EntityRendererProvider.java:38-48）；EquipmentAssetManager 无
        // Minecraft getter（vanilla 本地构造+注册重载，:530-531 同款）
        //? if >=21.4 && <21.6 {
        /*net.minecraft.client.resources.model.EquipmentAssetManager ysmEquipmentAssets = new net.minecraft.client.resources.model.EquipmentAssetManager();
        ((net.minecraft.server.packs.resources.ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(ysmEquipmentAssets);
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), ysmEquipmentAssets, Minecraft.getInstance().font);*/
        //?}
        // 1.21.6 起运行期 registerReloadListener 抛 UnsupportedOperationException
        //（listeners 集合在 AddClientReloadListenerEvent 后冻结，21.6 runClient 崩溃实证）
        // → 反射取 vanilla Minecraft 自建并注册的实例（21.6 Minecraft.java:545 构造，
        // 挂在 EntityRenderDispatcher.equipmentAssets 私有字段 :81；neoforge 全链 mojmap
        // 字面名即真名）。字段缺失兜底=孤儿实例（装备资产空、装备层退化，功能差入债）。
        // 块内容注释态存储（1201 vcs 直编原文铁律），活跃线剥壳展开
        //? if >=21.6 && <21.9 {
        /*net.minecraft.client.resources.model.EquipmentAssetManager ysmEquipmentAssets;
        try {
            java.lang.reflect.Field ysmEquipmentField = net.minecraft.client.renderer.entity.EntityRenderDispatcher.class.getDeclaredField("equipmentAssets");
            ysmEquipmentField.setAccessible(true);
            ysmEquipmentAssets = (net.minecraft.client.resources.model.EquipmentAssetManager) ysmEquipmentField.get(entityRenderDispatcher);
        } catch (ReflectiveOperationException e) {
            YesSteveModel.LOGGER.warn("[YSM] equipmentAssets reflection fallback (orphan instance, equipment layer degraded)", e);
            ysmEquipmentAssets = new net.minecraft.client.resources.model.EquipmentAssetManager();
        }
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), ysmEquipmentAssets, Minecraft.getInstance().font);*/
        //?}
        // 1.21.9 Context 10 参：+AtlasManager（Minecraft.getAtlasManager）+PlayerSkinRenderCache
        //（Minecraft.playerSkinRenderCache()，2110 EntityRendererProvider.java:42-51 实证）
        //? if >=21.9 {
        /*net.minecraft.client.resources.model.EquipmentAssetManager ysmEquipmentAssets = new net.minecraft.client.resources.model.EquipmentAssetManager();
        ((net.minecraft.server.packs.resources.ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(ysmEquipmentAssets);
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemModelResolver(), Minecraft.getInstance().getMapRenderer(), Minecraft.getInstance().getBlockRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), ysmEquipmentAssets, Minecraft.getInstance().getAtlasManager(), Minecraft.getInstance().font, Minecraft.getInstance().playerSkinRenderCache());*/
        //?}
        // 1.18.x Context 五参（七参 1.19.0 起：+BlockRenderDispatcher +ItemInHandRenderer）
        //? if <1.19
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().font);*/
        //? if >=1.19 && <1.19.2 {
        /*EntityRendererProvider.Context context = new EntityRendererProvider.Context(entityRenderDispatcher, Minecraft.getInstance().getItemRenderer(), Minecraft.getInstance().getBlockRenderer(), new net.minecraft.client.renderer.ItemInHandRenderer(Minecraft.getInstance(), entityRenderDispatcher, Minecraft.getInstance().getItemRenderer()), resourceManager, Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().font);*/
        //?}
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
