package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。forge 原类 1.20 展开态的活跃面 =
 * MobEffectEvent.{Added,Expired,Remove}（neoforge 同名同方法面，20.4.251 javap 实证）；
 * forge &lt;1.20 分支（PotionEvent）不进孪生。
 */
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class MobEffectForgeHook {
    private MobEffectForgeHook() {
    }

    @SubscribeEvent
    public static void onEffectAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectAdded((LivingEntity) event.getEntity(), instance.getEffect(), instance.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        MobEffectEvent.onEffectRemoved(event.getEntity(), event.getEffect());
    }

    @SubscribeEvent
    public static void onEffectExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectRemoved((LivingEntity) event.getEntity(), instance.getEffect());
        }
    }
}
