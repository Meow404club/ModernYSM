package com.elfmcys.yesstevemodel.platform.neoforge1202.capability;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
// 1.16.5 无 CapabilityManager.get(CapabilityToken)（1.17+ 才有），改 @CapabilityInject 注入
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ProjectileCapabilityProvider implements ICapabilityProvider {

    public static Capability<ProjectileCapability> PROJECTILE_CAP = CapabilityManager.get(new CapabilityToken<ProjectileCapability>() {
    });

    private ProjectileCapability capability;

    private Projectile projectile;

    public ProjectileCapabilityProvider(Projectile projectile) {
        this.projectile = projectile;
    }

    public ProjectileCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new ProjectileCapability(this.projectile);
            this.projectile = null;
        }
        return this.capability;
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return PROJECTILE_CAP.orEmpty(capability, LazyOptional.of(this::getOrCreateCapability));
    }
}
