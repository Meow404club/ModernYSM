package com.elfmcys.yesstevemodel.platform.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileModelCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.VehicleCapabilityProvider;
import com.elfmcys.yesstevemodel.platform.neoforge.capability.VehicleModelCapabilityProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import rip.ysm.api.PlatformAPI;

/**
 * neoforge 主类孪生（同 FQCN；RAW 源集）。forge 原类的差异：
 * - mod 事件总线由 @Mod 构造器注入 IEventBus（neoforge 无 FMLJavaModLoadingContext，loader 2.0.17 javap 实证）；
 * - 能力注册 = RegisterCapabilitiesEvent.registerEntity（20.4 能力重铸）。注册矩阵照搬 forge
 *   ForgeCapabilityHooks.onAttachCapabilities 的 attach 守卫语义（20.4.251 merged jar javap：
 *   registerEntity 泛型约束 <T,C,E extends Entity>(cap, EntityType<E>, ICapabilityProvider<? super E,C,T>)；
 *   全实体类型枚举注册为 docs capabilities.md "for all objects" 官方口径）：
 *   MODEL_INFO=玩家仅服务世界 / AUTH+STAR=玩家双侧 / PROJECTILE_MODEL=弹射物仅服务世界 /
 *   VEHICLE_MODEL=其余实体仅服务世界 / 客户端 trio（PLAYER/VEHICLE/PROJECTILE）=客户端世界专属。
 *   客户端类（AbstractClientPlayer 等）的 instanceof 全部置于 isClientSide 短路之后，
 *   专用服务端永不解析这些类（同 forge 原类守卫次序，防 NCDFE）。
 * 事件总线静态持有 + getModEventBus() 钩子与 forge 原类一致（YesSteveModel.initConfig 消费）。
 */
@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelForge {
    private static volatile IEventBus modEventBus;

    public YesSteveModelForge(IEventBus modEventBus) {
        YesSteveModelForge.modEventBus = modEventBus;
        modEventBus.addListener(YesSteveModelForge::onRegisterCapabilities);
        YesSteveModel.init();
    }

    /** mod 事件总线（构造期赋值，此后只读）；供注册类域显式接线复用。 */
    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        registerAll(event, ModelInfoCapabilityProvider.MODEL_INFO_CAP, (entity, side) ->
                entity instanceof Player player && !entity.level().isClientSide
                        ? ModelInfoCapabilityProvider.forEntity(player).getOrCreateCapability() : null);
        registerAll(event, AuthModelsCapabilityProvider.AUTH_MODELS_CAP, (entity, side) ->
                entity instanceof Player player
                        ? AuthModelsCapabilityProvider.forEntity(player).getOrCreateCapability() : null);
        registerAll(event, StarModelsCapabilityProvider.STAR_MODELS_CAP, (entity, side) ->
                entity instanceof Player player
                        ? StarModelsCapabilityProvider.forEntity(player).getOrCreateCapability() : null);
        registerAll(event, ProjectileModelCapabilityProvider.PROJECTILE_MODEL, (entity, side) ->
                entity instanceof Projectile projectile && !entity.level().isClientSide
                        ? ProjectileModelCapabilityProvider.forEntity(projectile).getOrCreateCapability() : null);
        registerAll(event, VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP, (entity, side) ->
                !entity.level().isClientSide && !(entity instanceof Player) && !(entity instanceof Projectile)
                        ? VehicleModelCapabilityProvider.forEntity(entity).getOrCreateCapability() : null);
        if (!PlatformAPI.isServer()) {
            registerAll(event, PlayerCapabilityProvider.PLAYER_CAP, (entity, side) ->
                    entity.level().isClientSide && entity instanceof AbstractClientPlayer clientPlayer
                            ? PlayerCapabilityProvider.forEntity(clientPlayer).getCapability() : null);
            registerAll(event, VehicleCapabilityProvider.VEHICLE_CAP, (entity, side) ->
                    entity.level().isClientSide && !(entity instanceof Player)
                            ? VehicleCapabilityProvider.forEntity(entity).getCapability() : null);
            registerAll(event, ProjectileCapabilityProvider.PROJECTILE_CAP, (entity, side) ->
                    entity.level().isClientSide && entity instanceof Projectile projectile
                            ? ProjectileCapabilityProvider.forEntity(projectile).getCapability() : null);
        }
    }

    /** 全实体类型枚举注册（docs capabilities.md "for all objects" 官方口径；raw 转换仅泛型擦除层面）。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void registerAll(RegisterCapabilitiesEvent event, EntityCapability<T, Void> cap,
                                        ICapabilityProvider<Entity, Void, T> provider) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            event.registerEntity(cap, (EntityType) type, provider);
        }
    }
}
