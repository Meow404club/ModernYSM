package com.elfmcys.yesstevemodel.capability;

//? if neoforge
/*import com.elfmcys.yesstevemodel.platform.neoforge.capability.ProjectileModelCapabilityProvider;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.capability.ProjectileModelCapabilityProvider;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;

public class ProjectileModelCapability {

    public static Optional<ProjectileModelCapability> get(Entity entity) {
        //? if neoforge
        /*return java.util.Optional.ofNullable(entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL));*/
        //? if forge
        //? if <1.16.2
        /*return java.util.Optional.ofNullable(entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).orElse(null));*/
        //? if >=1.16.2
        return entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).resolve();
    }

    public static Optional<ProjectileModelCapability> get(Projectile projectile) {
        //? if neoforge
        /*return java.util.Optional.ofNullable(projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL));*/
        //? if forge
        //? if <1.16.2
        /*return java.util.Optional.ofNullable(projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).orElse(null));*/
        //? if >=1.16.2
        return projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).resolve();
    }

    private String ownerModelId = "default";

    private boolean initialized = false;

    private Object2FloatOpenHashMap<String> molangVars = new Object2FloatOpenHashMap<>();

    public void setModel(String str, Object2FloatOpenHashMap<String> object2FloatOpenHashMap) {
        this.ownerModelId = str;
        this.initialized = true;
        this.molangVars = object2FloatOpenHashMap;
    }

    public void copyFrom(ProjectileModelCapability other) {
        this.ownerModelId = other.ownerModelId;
        this.initialized = other.initialized;
        this.molangVars = other.molangVars;
    }

    public String getOwnerModelId() {
        return this.ownerModelId;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public Object2FloatOpenHashMap<String> getMolangVars() {
        return this.molangVars;
    }

    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("owner_model_id", this.ownerModelId);
        compoundTag.putBoolean("initialized", this.initialized);
        CompoundTag compoundTag2 = new CompoundTag();
        this.molangVars.object2FloatEntrySet().fastForEach(entry -> {
            compoundTag2.putFloat(entry.getKey(), entry.getFloatValue());
        });
        compoundTag.put("molang_vars_server_bound", compoundTag2);
        return compoundTag;
    }

    public void deserializeNBT(CompoundTag compoundTag) {
        // 1.21.5 CompoundTag getter Optional 化（同 ModelInfoCapability 注）
        //? if <21.5
        this.ownerModelId = compoundTag.getString("owner_model_id");
        //? if >=21.5
        /*this.ownerModelId = compoundTag.getStringOr("owner_model_id", "");*/
        //? if <21.5
        this.initialized = compoundTag.getBoolean("initialized");
        //? if >=21.5
        /*this.initialized = compoundTag.getBooleanOr("initialized", false);*/
        this.molangVars.clear();
        //? if <21.5
        CompoundTag compound = compoundTag.getCompound("molang_vars_server_bound");
        //? if >=21.5
        /*CompoundTag compound = compoundTag.getCompoundOrEmpty("molang_vars_server_bound");*/
        //? if <21.5
        for (String str : compound.getAllKeys()) {
        //? if >=21.5
        /*for (String str : compound.keySet()) {*/
            //? if <21.5
            this.molangVars.put(str, compound.getFloat(str));
            //? if >=21.5
            /*this.molangVars.put(str, compound.getFloatOr(str, 0.0F));*/
        }
    }
}