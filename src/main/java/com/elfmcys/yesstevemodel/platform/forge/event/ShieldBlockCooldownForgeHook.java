package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ShieldBlockCooldownEvent;
//? if >=1.18.2 {
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class ShieldBlockCooldownForgeHook {

    private ShieldBlockCooldownForgeHook() {
    }

    // ShieldBlockEvent 1.16.5/1.17.1 forge jar 均不存在（unzip 全文检索 0 命中，1.18+ 才引入）：
    // <1.18.2 仅保留冷却 tick 路径；盾牌格挡触发的冷却起点缺失 → 功能差已记 tasks.feature-debts-1165
    //? if <1.19 {
    /*@SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingUpdateEvent event) {
        ShieldBlockCooldownEvent.onLivingTick((LivingEntity) event.getEntity());
    }
     *///?}
    //? if >=1.19 {
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        ShieldBlockCooldownEvent.onShieldBlock(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ShieldBlockCooldownEvent.onLivingTick(event.getEntity());
    }
    //?}
}
