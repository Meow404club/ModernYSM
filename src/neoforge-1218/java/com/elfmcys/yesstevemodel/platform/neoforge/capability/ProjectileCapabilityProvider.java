package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；客户端 molang 状态能力）。口径见 PlayerCapabilityProvider 孪生头注。
 */
public class ProjectileCapabilityProvider {

    public static final EntityCapability<ProjectileCapability, Void> PROJECTILE_CAP =
            EntityCapability.createVoid(Rl.of("yes_steve_model", "projectile_animatable"), ProjectileCapability.class);

    private static final Map<Projectile, ProjectileCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static ProjectileCapabilityProvider forEntity(Projectile projectile) {
        return INSTANCES.computeIfAbsent(projectile, ProjectileCapabilityProvider::new);
    }

    private ProjectileCapability capability;

    private Projectile projectile;

    private ProjectileCapabilityProvider(Projectile projectile) {
        this.projectile = projectile;
    }

    public ProjectileCapability getCapability() {
        return getOrCreateCapability();
    }

    private ProjectileCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new ProjectileCapability(this.projectile);
            this.projectile = null;
        }
        return this.capability;
    }
}
