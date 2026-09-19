package com.elfmcys.yesstevemodel.capability;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.entity.GeckoProjectileEntity;
import com.elfmcys.yesstevemodel.molang.runtime.Int2FloatOpenHashMapStruct;
//? if neoforge && >=1.20.3
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileCapabilityProvider;*/
//? if neoforge && <1.20.3
/*import com.elfmcys.yesstevemodel.platform.neoforge1202.capability.ProjectileCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileCapabilityProvider;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ProjectileCapability extends GeckoProjectileEntity {

    public static Optional<ProjectileCapability> get(Entity entity) {
        //? if neoforge && >=1.20.3
        /*return java.util.Optional.ofNullable(entity.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP));*/
        //? if forge && <1.16.2
        /*return java.util.Optional.ofNullable(entity.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).orElse(null));*/
        //? if (forge && >=1.16.2) || (neoforge && <1.20.3)
        return entity.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).resolve();
    }

    public static Optional<ProjectileCapability> get(Projectile projectile) {
        //? if neoforge && >=1.20.3
        /*return java.util.Optional.ofNullable(projectile.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP));*/
        //? if forge && <1.16.2
        /*return java.util.Optional.ofNullable(projectile.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).orElse(null));*/
        //? if (forge && >=1.16.2) || (neoforge && <1.20.3)
        return projectile.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).resolve();
    }

    @Nullable
    private Int2FloatOpenHashMapStruct floatProperties;

    public ProjectileCapability(Projectile projectile) {
        super(projectile);
    }

    public void updateModelId(String str) {
        setModelId(str);
        markModelInitialized();
    }

    public void setFloatProperties(Int2FloatOpenHashMap int2FloatOpenHashMap) {
        if (int2FloatOpenHashMap != null) {
            this.floatProperties = new Int2FloatOpenHashMapStruct(int2FloatOpenHashMap);
        } else {
            this.floatProperties = null;
        }
    }

    @Override
    public void setupAnim(float seekTime, boolean isFirstPerson) {
        super.setupAnim(seekTime, isFirstPerson);
        getEvaluationContext().setRoamingProperties(this.floatProperties);
    }
}