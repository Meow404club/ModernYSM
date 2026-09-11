package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
    // @At 注入点随版本分叉（m2 实测）：1.20.1 ServerPlayer.startRiding 覆写体内直调
    // positionRider；1.16.5 的覆写体只有「已骑乘瞬移」分支 + super.startRiding 转发
    //（javap -c 实证，addPassenger/positionRider 均不在本方法体）→ 注入点改 super 调用
    // 返回后（挂载完成、乘客已入列，语义等价；super 返回 false 时守卫条件自然不成立）。
    //? if >=1.17 {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        Entity entity2;
        if (YesSteveModel.isAvailable() && entity.getFirstPassenger() == (entity2 = (ServerPlayer) (Object) this)) {
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
    //?} else {
    /*@Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        Entity entity2;
        if (YesSteveModel.isAvailable() && !entity.getPassengers().isEmpty() && entity.getPassengers().get(0) == (entity2 = (ServerPlayer) (Object) this)) {
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
    *///?}
}