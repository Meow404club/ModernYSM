package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；src/neoforge/java RAW 源集不经 stonecutter，三线 20.4/20.6/21.1 API 同面）。
 * NeoForge 20.4 能力重铸后 CapabilityToken/LazyOptional/ICapabilitySerializable 全数移除：
 * 令牌 = EntityCapability.createVoid（20.4.251 merged jar javap 实证）；
 * 注册 = YesSteveModelForge 孪生的 RegisterCapabilitiesEvent.registerEntity；
 * 20.4 重铸后 provider 为无状态查询函数 → 实体实例状态由 WeakHashMap 逐实体缓存承载；
 * 持久化 = 20.4 重铸取消实体能力自动序列化 → 玩家侧能力由 ForgeCapabilityHooks 孪生在
 * PlayerEvent.SaveToFile/LoadFromFile 与 getPersistentData()（"NeoForgeData" 随实体 NBT 落盘）互拷。
 */
public class ModelInfoCapabilityProvider {

    public static final EntityCapability<ModelInfoCapability, Void> MODEL_INFO_CAP =
            EntityCapability.createVoid(Rl.of(YesSteveModel.MOD_ID, "model_id"), ModelInfoCapability.class);

    private static final Map<Entity, ModelInfoCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static ModelInfoCapabilityProvider forEntity(Player player) {
        return INSTANCES.computeIfAbsent(player, p -> new ModelInfoCapabilityProvider());
    }

    private ModelInfoCapability capability = null;

    public ModelInfoCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new ModelInfoCapability();
        }
        return this.capability;
    }

    public CompoundTag serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        getOrCreateCapability().deserializeNBT(tag);
    }
}
