package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。口径见 ModelInfoCapabilityProvider 孪生头注；
 * 弹射物为瞬态实体，会话级持久化（20.4 重铸后非玩家实体无保存事件钩子，语义降级点已入交接账）。
 */
public class ProjectileModelCapabilityProvider {

    public static final EntityCapability<ProjectileModelCapability, Void> PROJECTILE_MODEL =
            EntityCapability.createVoid(Rl.of(YesSteveModel.MOD_ID, "projectile_model_id"), ProjectileModelCapability.class);

    private static final Map<Entity, ProjectileModelCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static ProjectileModelCapabilityProvider forEntity(Projectile projectile) {
        return INSTANCES.computeIfAbsent(projectile, p -> new ProjectileModelCapabilityProvider());
    }

    private ProjectileModelCapability capability = null;

    public ProjectileModelCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new ProjectileModelCapability();
        }
        return this.capability;
    }

    public CompoundTag serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }

    public void deserializeNBT(CompoundTag compoundTag) {
        getOrCreateCapability().deserializeNBT(compoundTag);
    }
}
