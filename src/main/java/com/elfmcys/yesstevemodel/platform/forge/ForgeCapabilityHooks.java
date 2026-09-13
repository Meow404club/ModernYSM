package com.elfmcys.yesstevemodel.platform.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.platform.forge.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileModelCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.VehicleCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.forge.capability.VehicleModelCapabilityProvider;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import rip.ysm.api.PlatformAPI;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class ForgeCapabilityHooks {

    //? if >=1.21
    /*private static final ResourceLocation MODEL_INFO_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "model_id");*/
    //? if <1.21
    private static final ResourceLocation MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "model_id");
    //? if >=1.21
    /*private static final ResourceLocation PROJECTILE_MODEL_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "projectile_model_id");*/
    //? if <1.21
    private static final ResourceLocation PROJECTILE_MODEL_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_model_id");
    //? if >=1.21
    /*private static final ResourceLocation VEHICLE_MODEL_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "vehicle_model_id");*/
    //? if <1.21
    private static final ResourceLocation VEHICLE_MODEL_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "vehicle_model_id");
    //? if >=1.21
    /*private static final ResourceLocation AUTH_MODELS_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "own_models");*/
    //? if <1.21
    private static final ResourceLocation AUTH_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "own_models");
    //? if >=1.21
    /*private static final ResourceLocation STAR_MODELS_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "star_models");*/
    //? if <1.21
    private static final ResourceLocation STAR_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "star_models");
    //? if >=1.21
    /*private static final ResourceLocation PLAYER_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "animatable");*/
    //? if <1.21
    private static final ResourceLocation PLAYER_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "animatable");
    //? if >=1.21
    /*private static final ResourceLocation PROJECTILE_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "projectile_animatable");*/
    //? if <1.21
    private static final ResourceLocation PROJECTILE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_animatable");
    //? if >=1.21
    /*private static final ResourceLocation VEHICLE_CAP = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "vehicle_animatable");*/
    //? if <1.21
    private static final ResourceLocation VEHICLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "vehicle_animatable");

    private ForgeCapabilityHooks() {
    }

    /**
     * 1.16.5 条件差：1.16.x Entity 的 world 访问是 {@code level} 字段（1.20+ 才有 {@code level()} 访问器），
     * unimined 1.16.5 mojmap jar javap 实证 {@code public Level level}。
     */
    private static boolean isClientWorld(Entity entity) {
        //? if <1.17
        /*return entity.level.isClientSide;*/
        //? if >=1.17 && <1.18.2
        /*return entity.level.isClientSide();*/
        //? if >=1.18.2 && <1.20
        /*return entity.getLevel().isClientSide();*/
        //? if >=1.20
        return entity.level().isClientSide();
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (!isClientWorld(entity) && !player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).isPresent() && !event.getCapabilities().containsKey(MODEL_INFO_CAP)) {
                event.addCapability(MODEL_INFO_CAP, new ModelInfoCapabilityProvider());
            }
            if (!player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(AUTH_MODELS_CAP)) {
                event.addCapability(AUTH_MODELS_CAP, new AuthModelsCapabilityProvider());
            }
            if (!player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(STAR_MODELS_CAP)) {
                event.addCapability(STAR_MODELS_CAP, new StarModelsCapabilityProvider());
            }
        } else if (entity instanceof Projectile) {
            if (!isClientWorld(entity) && !entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).isPresent() && !event.getCapabilities().containsKey(PROJECTILE_MODEL_CAP)) {
                event.addCapability(PROJECTILE_MODEL_CAP, new ProjectileModelCapabilityProvider());
            }
        } else if (!isClientWorld(entity) && !entity.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).isPresent() && !event.getCapabilities().containsKey(VEHICLE_MODEL_CAP)) {
            event.addCapability(VEHICLE_MODEL_CAP, new VehicleModelCapabilityProvider());
        }
        if (!PlatformAPI.isServer() && isClientWorld(entity)) {
            if (entity instanceof AbstractClientPlayer) {
                AbstractClientPlayer abstractClientPlayer = (AbstractClientPlayer) entity;
                if (!abstractClientPlayer.getCapability(PlayerCapabilityProvider.PLAYER_CAP).isPresent() && !event.getCapabilities().containsKey(PLAYER_CAP)) {
                    event.addCapability(PLAYER_CAP, new PlayerCapabilityProvider(abstractClientPlayer));
                    return;
                }
            }
            if (!entity.getCapability(VehicleCapabilityProvider.VEHICLE_CAP).isPresent() && !event.getCapabilities().containsKey(VEHICLE_CAP)) {
                event.addCapability(VEHICLE_CAP, new VehicleCapabilityProvider(entity));
                if (entity instanceof Projectile && !event.getCapabilities().containsKey(PROJECTILE_CAP)) {
                    event.addCapability(PROJECTILE_CAP, new ProjectileCapabilityProvider((Projectile) entity));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking startTracking) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity target = startTracking.getTarget();
        if (target instanceof ServerPlayer) {
            ServerPlayer trackPlayer = (ServerPlayer) target;
            // StartTracking.getEntity() 协变 Player 覆写 1.19.2 起（1182 返回 Entity）
            //? if >=1.19.2
            Player entity = startTracking.getEntity();
            //? if <1.19.2
            /*Player entity = (Player) startTracking.getEntity();*/

            CapabilityEvent.getModelInfoCap(trackPlayer).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerConnected(trackPlayer) && !cap.isMandatory()) {
                    return;
                }
                Optional<S2CSetModelAndTexturePacket> optional = cap.createSyncMessage(trackPlayer, false);
                Consumer<? super S2CSetModelAndTexturePacket> consumer = message -> NetworkHandler.sendToClientPlayer(message, entity);
                Objects.requireNonNull(cap);
                if (optional.isPresent()) {
                    consumer.accept(optional.get());
                } else {
                    cap.markDirty();
                }
            });
            return;
        }
        if (target instanceof Projectile) {
            Projectile projectile = (Projectile) target;
            projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    //? if >=1.19.2
                    NetworkHandler.sendToClientPlayer(new S2CSyncProjectileModelPacket(projectile.getId(), cap), startTracking.getEntity());
                    //? if <1.19.2
                    /*NetworkHandler.sendToClientPlayer(new S2CSyncProjectileModelPacket(projectile.getId(), cap), (Player) startTracking.getEntity());*/
                }
            });
        } else if (target != null) {
            target.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    //? if >=1.19.2
                    NetworkHandler.sendToClientPlayer(new S2CSyncVehicleModelPacket(target.getId(), cap), startTracking.getEntity());
                    //? if <1.19.2
                    /*NetworkHandler.sendToClientPlayer(new S2CSyncVehicleModelPacket(target.getId(), cap), (Player) startTracking.getEntity());*/
                }
            });
        }
    }
}
