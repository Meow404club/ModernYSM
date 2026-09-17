package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.20.5+ 专用：Arrow.effects 字段删除（potion 效果随拾取物数据组件化，
 * vanilla-1.20.6 Arrow.java:38 getPotionContents()=私有读取 DataComponents.POTION_CONTENTS）。
 * 原 ArrowEntityAccessor（@Accessor("effects")）在 1.20.5+ 无目标字段=APPLY 失败崩启动，
 * mixins.json 对 >=1.20.5 替换为本 Invoker。1.21.1 getAllEffects 同签名，共用同一形态。
 */
@Mixin(Arrow.class)
public interface ArrowPotionAccessor {
    @Invoker("getPotionContents")
    PotionContents ysm$getPotionContents();
}
