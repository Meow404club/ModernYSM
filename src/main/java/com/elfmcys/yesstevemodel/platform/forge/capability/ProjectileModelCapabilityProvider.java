package com.elfmcys.yesstevemodel.platform.forge.capability;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
//? if <1.17 {
/*import net.minecraft.nbt.Tag;*/
//?}
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
// 1.16.5 无 CapabilityManager.get(CapabilityToken)（1.17+ 才有），改 @CapabilityInject 注入
//? if >=1.17 {
import net.minecraftforge.common.capabilities.CapabilityToken;
//?} else {
/*import net.minecraftforge.common.capabilities.CapabilityInject;*/
//?}
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectileModelCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    //? if >=1.17 {
    public static Capability<ProjectileModelCapability> PROJECTILE_MODEL = CapabilityManager.get(new CapabilityToken<ProjectileModelCapability>() {
    });
    //?} else {
    /*@CapabilityInject(ProjectileModelCapability.class)
    public static Capability<ProjectileModelCapability> PROJECTILE_MODEL = null;*/
//?}

    private ProjectileModelCapability capability = null;

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return PROJECTILE_MODEL.orEmpty(capability, LazyOptional.of(this::getOrCreateCapability));
    }

    @NotNull
    private ProjectileModelCapability getOrCreateCapability() {
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
    //? if <1.17 {
    /*// 1.16.5 专用注册：1.16.x 无 CapabilityManager.get(CapabilityToken)（1.17+ 才有），
    // 走旧机制 CapabilityManager.INSTANCE.register + @CapabilityInject 字段注入
    // （实证：forge-1.16.x CapabilityManager.java:37 register(Class,IStorage,Callable) /
    // CapabilityInject.java 字段注入语义；unimined 1.16.5 mojmap jar javap 复核）。
    // IStorage 为占位空实现——本域 NBT 持久化走 ICapabilitySerializable（provider 自身），
    // 不经过 Capability.IStorage；默认实例工厂不会被调用（域内从不调 getDefaultInstance）。
    // 调用方：CapabilityEvent.register()（mod 构造期，CapabilityManager#register 声明并行 loading 安全）。
    public static void registerCapability() {
        CapabilityManager.INSTANCE.register(ProjectileModelCapability.class, new Capability.IStorage<ProjectileModelCapability>() {
            @Override
            public Tag writeNBT(Capability<ProjectileModelCapability> capability, ProjectileModelCapability instance, Direction side) {
                return new CompoundTag();
            }

            @Override
            public void readNBT(Capability<ProjectileModelCapability> capability, ProjectileModelCapability instance, Direction side, Tag nbt) {
            }
        }, () -> null);
    }*/
//?}
}
