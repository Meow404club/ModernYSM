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
    @Inject(method = {"startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER)})
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> ci) {
        Entity entity2;
        // 1.16.5 Entity 无 getFirstPassenger()（1.17+ 引入）；getFirstPassenger 语义=
        // passengers 空则 null 否则 passengers[0]，空表时 null==this 恒 false，与
        // !isEmpty 短路写法等价（1.16.5 Entity.getPassengers():903）
        //? if >=1.17 {
        if (YesSteveModel.isAvailable() && entity.getFirstPassenger() == (entity2 = (ServerPlayer) (Object) this)) {
        //?} else {
        /*if (YesSteveModel.isAvailable() && !entity.getPassengers().isEmpty() && entity.getPassengers().get(0) == (entity2 = (ServerPlayer) (Object) this)) {
        *///?}
            CapabilityEvent.syncVehicleModel(entity, (ServerPlayer) entity2);
        }
    }
}