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
//? if >=21.11
/*import net.minecraft.world.entity.projectile.arrow.AbstractArrow;*/
//? if <21.11
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=21.11
/*@Mixin({AbstractArrow.class})*/
//? if <21.11
@Mixin({AbstractArrow.class})
public class AbstractArrowEntityMixin implements ProjectileStateAccessor {

    @Unique
    private String ownerMainHandItem = StringPool.EMPTY;

    // AbstractArrow.inGround 布尔字段 1.21.2 起封装（21.3/21.4/21.5/21.8/21.10 sources 实证：
    // 字段零命中，protected isInGround() 在 21.3 AbstractArrow.java:331；1.21.1 sources:60 仍有
    // protected boolean inGround；全部 forge 线 1.16.5~1.20.1 为 public 字段）
    // → <21.3 保 @Shadow 字段；>=21.3 用 inGroundTime>0 近似「已落地」（tick 语义：落地帧自增/
    // 离地帧清零，滞后一 tick，功能债入账）。
    // ⚠ @Unique isInGround() 与目标 protected isInGround() 同签名 → Mixin 丢弃
    //（21.10/21.11 runClient WARN 实证 "Discarding @Unique public method isInGround"）→
    // >=21.3 接口方法永不满足，调用方（YSMBinding/ProjectileAnimationPredicate）>=21.3 门控绕行
    //? if <21.3 {
    @Shadow
    public boolean inGround;
    //?}
    @Shadow
    public int inGroundTime;

    @Override
    @Unique
    public boolean isInGround() {
        //? if >=21.3
        /*return this.inGroundTime > 0;*/
        //? if <21.3
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
