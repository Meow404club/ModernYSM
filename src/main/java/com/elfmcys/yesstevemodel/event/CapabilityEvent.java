package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncStarModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CVersionCheckPacket;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.AuthModelsCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.AuthModelsCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.ModelInfoCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.ModelInfoCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.PlayerCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.PlayerCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileModelCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileModelCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.StarModelsCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.StarModelsCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.VehicleCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.VehicleCapabilityProvider;
//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.VehicleModelCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.VehicleModelCapabilityProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
//? if neoforge
/*import net.neoforged.neoforge.event.TickEvent;*/
//? if forge
import net.minecraftforge.event.TickEvent;
//? if >=1.19.2 && neoforge
/*import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;*/
//? if >=1.19.2 && forge
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
//? if <1.19.2 {
/*// 事件名反向差：1.16.x 为 EntityJoinWorldEvent（1.19+ 才改名 JoinLevel），javap 实证
import net.minecraftforge.event.entity.EntityJoinWorldEvent;*/
//?}
//? if neoforge
/*import net.neoforged.neoforge.event.entity.player.PlayerEvent;*/
//? if forge
import net.minecraftforge.event.entity.player.PlayerEvent;
//? if neoforge
/*import net.neoforged.bus.api.SubscribeEvent;*/
//? if forge
import net.minecraftforge.eventbus.api.SubscribeEvent;
//? if neoforge
/*import net.neoforged.fml.common.Mod;*/
//? if forge
import net.minecraftforge.fml.common.Mod;
//? if <1.17 {
/*// 1.16.5 ServerTickEvent 无 getServer()（1.18.2+ 才加），走 ServerLifecycleHooks
import net.minecraftforge.fml.server.ServerLifecycleHooks;*/
//?}
import rip.ysm.api.PlatformAPI;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Forge 原生实现（architectury 迁移）：
 * <ul>
 * <li>PlayerEvent.PLAYER_CLONE -&gt; {@link PlayerEvent.Clone}（同一 Forge 背板事件，getOriginal/getEntity/isWasDeath 一一对应）；</li>
 * <li>EntityEvent.ADD -&gt; {@link EntityJoinLevelEvent}（architectury-forge 9.2.14 EventHandlerImplCommon 反编译实证背板）；
 * 原 handler 恒返回 EventResult.pass()，architectury 仅对 isFalse()（fail）setCanceled，本处无 fail 出口 =&gt; void 处理器等价；</li>
 * <li>TickEvent.SERVER_POST -&gt; {@link TickEvent.ServerTickEvent} phase == Phase.END（architectury START-&gt;SERVER_PRE/END-&gt;SERVER_POST 分派实证）。</li>
 * </ul>
 * capability attach 不在本类：由 platform/forge/ForgeCapabilityHooks 的 AttachCapabilitiesEvent 注解式处理。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapabilityEvent {

    private CapabilityEvent() {
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        // 1.16.x reviveCaps/invalidateCaps 是 protected（1.17+ 才改 public）；且 1.16.5 的
        // PlayerList.recreatePlayerEntity 以 removePlayer(level,true) 保数据直到 copyFrom、clone 事件后
        // 才 remove(false) 使 caps 失效（forge-1.16.x PlayerList.java.patch 注释实证）——
        // 事件分发时旧玩家 caps 仍有效，无需 revive/invalidate，两侧语义等价。
        //? if forge && >=1.17 {
        oldPlayer.reviveCaps();
        //?}
        Optional<ModelInfoCapability> oldModelInfoCap = getModelInfoCap(oldPlayer);
        Optional<AuthModelsCapability> oldAuthModelsCap = getAuthModelsCap(oldPlayer);
        Optional<StarModelsCapability> oldStarModelsCap = getStarModelsCap(oldPlayer);
        //? if forge && >=1.17 {
        oldPlayer.invalidateCaps();
        //?}
        Optional<ModelInfoCapability> modelInfoCap = getModelInfoCap(newPlayer);
        Optional<AuthModelsCapability> authModelsCap = getAuthModelsCap(newPlayer);
        Optional<StarModelsCapability> starModelsCap = getStarModelsCap(newPlayer);
        modelInfoCap.ifPresent(newModelInfo -> {
            Objects.requireNonNull(newModelInfo);
            oldModelInfoCap.ifPresent(newModelInfo::copyFrom);
        });
        authModelsCap.ifPresent(newAuthModels -> {
            Objects.requireNonNull(newAuthModels);
            oldAuthModelsCap.ifPresent(newAuthModels::copyFrom);
        });
        starModelsCap.ifPresent(newStarModels -> {
            Objects.requireNonNull(newStarModels);
            oldStarModelsCap.ifPresent(newStarModels::copyFrom);
        });
    }

    //? if >=1.19.2 {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        onEntityJoin(event.getEntity());
    }
    //?} else {
    /*@SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinWorldEvent event) {
        onEntityJoin(event.getEntity());
    }*/
//?}

    private static void onEntityJoin(Entity entity) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            getModelInfoCap(player).ifPresent(modelInfoCap -> {
                if (!NetworkHandler.isPlayerConnected(player) && !modelInfoCap.isMandatory()) {
                    modelInfoCap.markDirty();
                    return;
                }
                modelInfoCap.stopAnimation(player);
                Optional<S2CSetModelAndTexturePacket> optional = modelInfoCap.createSyncMessage(player, false);
                Consumer<? super S2CSetModelAndTexturePacket> consumer = message -> NetworkHandler.sendToClientPlayer(message, player);
                Objects.requireNonNull(modelInfoCap);
                if (optional.isPresent()) {
                consumer.accept(optional.get());
            } else {
                modelInfoCap.markDirty();
            }
            });
            getAuthModelsCap(player).ifPresent(authModelsCap -> {
                for (String modelId : ServerModelManager.getAuthModels()) {
                    authModelsCap.addModel(modelId);
                }
                NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(authModelsCap.getAuthModels()), player);
            });
            getStarModelsCap(player).ifPresent(starModelsCap -> NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(starModelsCap.getStarModels()), player));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // TickEvent.ServerTickEvent.getServer 1.19.3+；1.16.5 fml.server 包 / 1.17~1.18.2
        // net.minecraftforge.server 包 / 1.19.3+ 事件自带
        //? if <1.17
        /*onServerTickEnd(net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer());*/
        //? if >=1.17 && <1.18.2
        /*onServerTickEnd(net.minecraftforge.fmllegacy.server.ServerLifecycleHooks.getCurrentServer());*/
        //? if >=1.18.2 && <1.19.2
        /*onServerTickEnd(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());*/
        //? if >=1.19.2
        onServerTickEnd(event.getServer());
    }

    private static void onServerTickEnd(MinecraftServer server) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        Boolean bool = ServerConfig.LOW_BANDWIDTH_USAGE.get();
        for (ServerPlayer serverPlayer : players) {
            getModelInfoCap(serverPlayer).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerConnected(serverPlayer) && !cap.isMandatory()) {
                    if (serverPlayer.tickCount == 200 || serverPlayer.tickCount == 600 || serverPlayer.tickCount == 1800) {
                        NetworkHandler.sendToClientPlayer(new S2CVersionCheckPacket(), serverPlayer);
                    }
                    return;
                }
                if (cap.isDirty()) {
                    cap.getAnimSync().updateAndSync(serverPlayer, false, bool);
                    cap.createSyncMessage(serverPlayer, true).ifPresent(message -> {
                        cap.clearDirty();
                        NetworkHandler.sendToTrackingEntityAndSelf(message, serverPlayer);
                        // 1.16.x 无 Entity.getFirstPassenger()（1.19+ 才有），取乘客列表首元素等价
                        //? if >=1.17
                        if (serverPlayer.getVehicle() != null && serverPlayer.getVehicle().getFirstPassenger() == serverPlayer) {
                        //? if <1.17
                        /*if (serverPlayer.getVehicle() != null && !serverPlayer.getVehicle().getPassengers().isEmpty() && serverPlayer.getVehicle().getPassengers().get(0) == serverPlayer) {*/
                            syncVehicleModel(serverPlayer.getVehicle(), serverPlayer);
                        }
                    });
                } else {
                    cap.getAnimSync().updateAndSync(serverPlayer, true, bool);
                }
            });
        }
    }

    public static void syncProjectileModel(Projectile projectile, ServerPlayer serverPlayer) {
        ModelInfoCapability.get(serverPlayer).ifPresent(modelInfoCap -> {
            if (!NetworkHandler.isPlayerConnected(serverPlayer) && !modelInfoCap.isMandatory()) {
                return;
            }
            ProjectileModelCapability.get(projectile).ifPresent(projectileModelCap -> modelInfoCap.withMolangVars(object2FloatOpenHashMap -> {
                projectileModelCap.setModel(modelInfoCap.getModelId(), object2FloatOpenHashMap);
                NetworkHandler.sendToTrackingEntity(new S2CSyncProjectileModelPacket(projectile.getId(), projectileModelCap), projectile);
            }));
        });
    }

    public static void syncVehicleModel(Entity entity, ServerPlayer serverPlayer) {
        ModelInfoCapability.get(serverPlayer).ifPresent(modelInfoCap -> {
            if (!NetworkHandler.isPlayerConnected(serverPlayer) && !modelInfoCap.isMandatory()) {
                return;
            }
            VehicleModelCapability.get(entity).ifPresent(vehicleModelCap -> modelInfoCap.getMolangVars().ifPresent(object2FloatOpenHashMap -> {
                vehicleModelCap.setModel(modelInfoCap.getModelId(), object2FloatOpenHashMap);
                NetworkHandler.sendToTrackingEntity(new S2CSyncVehicleModelPacket(entity.getId(), vehicleModelCap), entity);
            }));
        });
    }

    public static Optional<ModelInfoCapability> getModelInfoCap(Player player) {
        return ModelInfoCapability.get(player);
    }

    public static Optional<AuthModelsCapability> getAuthModelsCap(Player player) {
        return AuthModelsCapability.get(player);
    }

    public static Optional<StarModelsCapability> getStarModelsCap(Player player) {
        return StarModelsCapability.get(player);
    }

    /**
     * 1.16.5 capability 注册枢纽。1.16.x 无 {@code CapabilityManager.get(CapabilityToken)}（1.17+ 才有），
     * 须显式 CapabilityManager.INSTANCE.register，再由 @CapabilityInject 注入各 Provider 的 CAP 字段
     * （实证：forge-1.16.x CapabilityManager.java:37 register(Class,IStorage,Callable)、CapabilityInject
     * 字段注入；unimined 1.16.5 mojmap forge jar javap 复核）。客户端专属能力（Player/Projectile/Vehicle，
     * Provider 带 @OnlyIn(Dist.CLIENT)）仅在客户端注册，防专用服务端 @OnlyIn 剥离后类缺失崩载——
     * 与 ForgeCapabilityHooks 的 dist 守卫同款，方法引用惰性解析保证 server 侧不触达。
     * 1.20.1 侧为 no-op（token 机制在 Provider 字段初始化时自取，无需注册）。
     * 1.20.1 侧为 no-op（token 机制在 Provider 字段初始化时自取，无需注册）。
     * 【时机铁律（1.16.5 dev runServer 实测 NPE + forge 1.16.x 源码双证）】不得在 mod 构造期调用：
     * 1.16.x CapabilityManager.callbacks 无字段初始化器，仅由 injectCapabilities（scan 阶段、
     * 先于 FMLCommonSetupEvent）赋值，构造期 register() 即 CapabilityManager.java:65 NPE。
     * 正确时机 = FMLCommonSetupEvent（forge 1.16 官方文档规定），由 CommonEvent.onCommonSetup 调用。
     * CapabilityManager#register 注释声明并行 mod loading 安全。
     */
    public static void registerCapabilities() {
        //? if <1.17 {
        /*AuthModelsCapabilityProvider.registerCapability();
        ModelInfoCapabilityProvider.registerCapability();
        ProjectileModelCapabilityProvider.registerCapability();
        StarModelsCapabilityProvider.registerCapability();
        VehicleModelCapabilityProvider.registerCapability();
        if (!PlatformAPI.isServer()) {
            PlayerCapabilityProvider.registerCapability();
            ProjectileCapabilityProvider.registerCapability();
            VehicleCapabilityProvider.registerCapability();
        }*/
//?}
    }
}
