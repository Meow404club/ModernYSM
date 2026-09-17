package com.elfmcys.yesstevemodel.platform.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileModelCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.VehicleModelCapabilityProvider;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。forge 原类职责拆分：
 * - AttachCapabilitiesEvent 已被 20.4 能力重铸取代 → 注册矩阵移入 YesSteveModelForge 孪生
 *   （RegisterCapabilitiesEvent.registerEntity + 查询期守卫）；
 * - StartTracking 同名事件保留（neoforge net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking，
 *   getTarget/getEntity 同面）；能力查询 LazyOptional→直接判空（20.4 重铸口径）；
 * - 新增：玩家三能力（MODEL_INFO/AUTH/STAR）持久化互拷。forge 侧由 ICapabilitySerializable 自动
 *   序列化，20.4 重铸取消 → SaveToFile 时写入 getPersistentData()（"NeoForgeData" 随实体 NBT 落盘）、
 *   LoadFromFile 时回读；death clone 仍走 CapabilityEvent.onPlayerCloned（与 forge 同构）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class ForgeCapabilityHooks {

    private static final String PERSIST_KEY = "yes_steve_model_caps";

    private ForgeCapabilityHooks() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking startTracking) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity target = startTracking.getTarget();
        if (target instanceof ServerPlayer) {
            ServerPlayer trackPlayer = (ServerPlayer) target;
            Player entity = startTracking.getEntity();

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
            ProjectileModelCapability cap = projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL);
            if (cap != null && cap.isInitialized()) {
                NetworkHandler.sendToClientPlayer(new S2CSyncProjectileModelPacket(projectile.getId(), cap), startTracking.getEntity());
            }
        } else if (target != null) {
            VehicleModelCapability cap = target.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP);
            if (cap != null && cap.isInitialized()) {
                NetworkHandler.sendToClientPlayer(new S2CSyncVehicleModelPacket(target.getId(), cap), startTracking.getEntity());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !YesSteveModel.isAvailable()) {
            return;
        }
        CompoundTag root = new CompoundTag();
        ModelInfoCapability modelInfo = player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP);
        if (modelInfo != null) {
            root.put("model_info", modelInfo.serializeNBT());
        }
        AuthModelsCapability authModels = player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP);
        if (authModels != null) {
            root.put("auth_models", authModels.serializeNBT());
        }
        StarModelsCapability starModels = player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP);
        if (starModels != null) {
            root.put("star_models", starModels.serializeNBT());
        }
        if (!root.isEmpty()) {
            player.getPersistentData().put(PERSIST_KEY, root);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoadFromFile(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !YesSteveModel.isAvailable()) {
            return;
        }
        // 1.21.5 CompoundTag.getCompound 返回 Optional<CompoundTag>（CompoundTag.java:378/381 getCompoundOrEmpty）
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(PERSIST_KEY);
        if (root.isEmpty()) {
            return;
        }
        if (root.contains("model_info")) {
            ModelInfoCapability modelInfo = player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP);
            if (modelInfo != null) {
                modelInfo.deserializeNBT(root.getCompoundOrEmpty("model_info"));
            }
        }
        if (root.contains("auth_models")) {
            AuthModelsCapability authModels = player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP);
            if (authModels != null) {
                authModels.deserializeNBT(root.getListOrEmpty("auth_models"));
            }
        }
        if (root.contains("star_models")) {
            StarModelsCapability starModels = player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP);
            if (starModels != null) {
                starModels.deserializeNBT(root.getListOrEmpty("star_models"));
            }
        }
    }
}
