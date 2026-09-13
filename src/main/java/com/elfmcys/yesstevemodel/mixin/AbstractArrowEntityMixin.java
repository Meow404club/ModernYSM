package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.accessors.ProjectileStateAccessor;
//? if >=1.19.4 {
import net.minecraft.core.registries.BuiltInRegistries;
//?}
// 1.16.5~1.19.2 走 Registry.ITEM（DefaultedRegistry，getKey 同语义；1165 Registry.java:140/1182:204）
//? if <1.19.4 {
import net.minecraft.core.Registry;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractArrow.class})
public class AbstractArrowEntityMixin implements ProjectileStateAccessor {

    @Unique
    private String ownerMainHandItem = StringPool.EMPTY;

    @Shadow
    public boolean inGround;

    @Shadow
    public int inGroundTime;

    @Override
    @Unique
    public boolean isInGround() {
        return this.inGround;
    }

    @Override
    @Unique
    public int getInGroundTime() {
        return this.inGroundTime;
    }

    @Override
    @Unique
    public String getOwnerItemId() {
        return this.ownerMainHandItem;
    }

    @Inject(at = {@At("RETURN")}, method = {"setOwner(Lnet/minecraft/world/entity/Entity;)V"})
    private void onSetOwner(Entity entity, CallbackInfo callbackInfo) {
        ResourceLocation key;
        // 1.16.5 无 BuiltInRegistries（1.19.3+），等价入口 Registry.ITEM（1.16.5 Registry.java:140，
        // DefaultedRegistry<Item>）；getKey 语义一致
        //? if >=1.19.4 {
        if (YesSteveModel.isAvailable() && (entity instanceof LivingEntity) && (key = BuiltInRegistries.ITEM.getKey(((LivingEntity) entity).getMainHandItem().getItem())) != null) {
        //?} else {
        /*if (YesSteveModel.isAvailable() && (entity instanceof LivingEntity) && (key = Registry.ITEM.getKey(((LivingEntity) entity).getMainHandItem().getItem())) != null) {
        *///?}
            this.ownerMainHandItem = key.toString();
        }
    }
}
