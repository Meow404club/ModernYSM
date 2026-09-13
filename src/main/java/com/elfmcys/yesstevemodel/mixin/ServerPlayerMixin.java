package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @At 注入点随版本分叉：
// 1.19.4 起 ServerPlayer 不再覆写 startRiding（vanilla-mc-1194 ServerPlayer.java 无该方法，
// javap 1194 merged jar 实证）→ ServerPlayer 目标失效；1165~1192 覆写体无 positionRider
// 调用（1171/1182 merged jar javap：体=瞬移分支 + invokespecial Player.startRiding）→
// <1.19.4 注入 super 调用返回后（挂载完成、乘客已入列，语义等价）；1194 段改注
// Entity.startRiding TAIL（this=乘客，isPassenger 守卫过滤失败分支）；1.20.1 恢复基线
// ServerPlayer + positionRider 形（在产线产物零变化）。
//? if <1.19.4 {
/*
@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        Entity entity2;
        if (YesSteveModel.isAvailable() && !entity.getPassengers().isEmpty() && entity.getPassengers().get(0) == (entity2 = (ServerPlayer) (Object) this)) {
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
}
 *///?}
//? if >=1.19.4 && <1.20 {
/*
@Mixin({Entity.class})
public abstract class ServerPlayerMixin {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At("TAIL")})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        if (YesSteveModel.isAvailable() && (Object) this instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (player.getVehicle() != null) {
                CapabilityEvent.syncVehicleModel(entity, player);
            }
        }
    }
}
 *///?}
//? if >=1.20 && <21.9 {
@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        Entity entity2;
        if (YesSteveModel.isAvailable() && entity.getFirstPassenger() == (entity2 = (ServerPlayer) (Object) this)) {
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
}
//?}
// 1.21.9 ServerPlayer 不再覆写 startRiding（2110 ServerPlayer 零命中，同 1.19.4 前例）且
// Entity.startRiding 增第三布尔参（2111 Entity.java:2349 startRiding(Entity,boolean,boolean)）
// → 目标切 Entity.startRiding TAIL（this=乘客，isPassenger 守卫过滤失败分支，1.19.4 段同款）
//? if >=21.9 {
/*
@Mixin({Entity.class})
public abstract class ServerPlayerMixin {
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"}, at = {@At("TAIL")})
    private void onStartRiding(Entity entity, boolean force, boolean captureDecoy, CallbackInfoReturnable<Boolean> ci) {
        if (YesSteveModel.isAvailable() && (Object) this instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (player.getVehicle() != null) {
                CapabilityEvent.syncVehicleModel(entity, player);
            }
        }
    }
}
*///?}
