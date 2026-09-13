package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.20.5+ 两处断裂：①@Mod.EventBusSubscriber 删除 → net.neoforged.fml.common.EventBusSubscriber
 * （1.20.6 文档 version-1.20.6/concepts/events.md:159 + oldtest 用法实证）；②MobEffectInstance.getEffect
 * /MobEffectEvent.Remove.getEffect 返回 Holder&lt;MobEffect&gt;（1.20.6 MobEffectInstance.java:193、
 * MobEffectEvent.java:65）→ .value() 还原 MobEffect 进共享 MobEffectEvent 门面。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class MobEffectForgeHook {
    private MobEffectForgeHook() {
    }

    @SubscribeEvent
    public static void onEffectAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectAdded((LivingEntity) event.getEntity(), instance.getEffect().value(), instance.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        MobEffectEvent.onEffectRemoved(event.getEntity(), event.getEffect().value());
    }

    @SubscribeEvent
    public static void onEffectExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectRemoved((LivingEntity) event.getEntity(), instance.getEffect().value());
        }
    }
}
