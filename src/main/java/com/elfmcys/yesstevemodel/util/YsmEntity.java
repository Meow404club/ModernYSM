package com.elfmcys.yesstevemodel.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 版本中性 Entity 访问器门面（全部单行直调，无状态）。
 * <p>实据 1.16.5 mojmap jar javap：
 * {@code public boolean isOnGround()}（无 onGround）、{@code public float yRot}（无 getYRot）、
 * {@code public Level level} 字段（无 level()）、{@code public boolean removed} 字段（无 isRemoved）、
 * {@code public final Abilities abilities} 字段（无 getAbilities()）、
 * 无 getFirstPassenger/getControllingPassenger 现代形。
 */
public final class YsmEntity {
    private YsmEntity() {
    }

    public static boolean onGround(Entity entity) {
        //? if <1.17 {
        /*return entity.isOnGround();
         *///?} else {
        return entity.onGround();
        //?}
    }

    public static float getYRot(Entity entity) {
        //? if <1.17 {
        /*return entity.yRot;
         *///?} else {
        return entity.getYRot();
        //?}
    }

    public static Level level(Entity entity) {
        //? if <1.17 {
        /*return entity.level;
         *///?} else {
        return entity.level();
        //?}
    }

    public static boolean isRemoved(Entity entity) {
        //? if <1.17 {
        /*return entity.removed;
         *///?} else {
        return entity.isRemoved();
        //?}
    }

    public static Abilities abilities(Player player) {
        //? if <1.17 {
        /*return player.abilities;
         *///?} else {
        return player.getAbilities();
        //?}
    }

    /** 1.16.5 无 getFirstPassenger：passengers 列表首位即 1.20.1 语义（getFirstPassenger=getPassengers 首位）。 */
    public static Entity firstPassenger(Entity vehicle) {
        //? if <1.17 {
        /*return vehicle.getPassengers().isEmpty() ? null : vehicle.getPassengers().get(0);
         *///?} else {
        return vehicle.getFirstPassenger();
        //?}
    }

    /** 1.16.5 无 walkAnimation：limbSwingAmount（1.16.5 实体摆动量字段/渲染入参）等价物由调用侧条件化，此处仅同物品比较。 */
    public static boolean sameItem(ItemStack a, ItemStack b) {
        //? if <1.17 {
        /*return a.getItem() == b.getItem();
         *///?} else {
        return ItemStack.isSameItem(a, b);
        //?}
    }

    /** LivingEntity 摆动量（1.20.1 walkAnimation.speed(partialTick) ↔ 1.16.5 oBob/limbSwing 聚合，取值口径见调用点注释）。 */
    public static float limbSwingAmount(LivingEntity entity, float partialTick) {
        //? if <1.17 {
        /*return entity.animationSpeedOld + (entity.animationSpeed - entity.animationSpeedOld) * partialTick;
         *///?} else {
        return entity.walkAnimation.speed(partialTick);
        //?}
    }
}
