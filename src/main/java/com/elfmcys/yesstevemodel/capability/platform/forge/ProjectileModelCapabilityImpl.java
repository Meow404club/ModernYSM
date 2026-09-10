package com.elfmcys.yesstevemodel.capability.platform.forge;

import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileModelCapabilityProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;

public final class ProjectileModelCapabilityImpl {

    private ProjectileModelCapabilityImpl() {
    }

    public static Optional<ProjectileModelCapability> get(Entity entity) {
        return entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).resolve();
    }

    public static Optional<ProjectileModelCapability> get(Projectile projectile) {
        return projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).resolve();
    }
}
