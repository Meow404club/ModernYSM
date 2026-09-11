package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
// MobEffectEvent 为 1.18+ 事件族（1.16.5 jar 无此包）：1.16.5 对位 PotionEvent
//? if <1.17 {
/*import net.minecraftforge.event.entity.living.PotionEvent.PotionAddedEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionExpiryEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
 *///?} else {
import net.minecraftforge.event.entity.living.MobEffectEvent.Added;
import net.minecraftforge.event.entity.living.MobEffectEvent.Expired;
import net.minecraftforge.event.entity.living.MobEffectEvent.Remove;
//?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class MobEffectForgeHook {

    private MobEffectForgeHook() {
    }

    // 1.16.5 取值差：PotionAddedEvent.getPotionEffect()/PotionRemoveEvent.getPotion()（1.20.1 MobEffectEvent 为
    // getEffectInstance()/getEffect()）
    //? if <1.17 {
    /*@SubscribeEvent
    public static void onEffectAdded(PotionAddedEvent event) {
        MobEffectInstance instance = event.getPotionEffect();
        if (instance != null) {
            MobEffectEvent.onEffectAdded(event.getEntity(), instance.getEffect(), instance.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(PotionRemoveEvent event) {
        MobEffectEvent.onEffectRemoved(event.getEntity(), event.getPotion());
    }

    @SubscribeEvent
    public static void onEffectExpired(PotionExpiryEvent event) {
        MobEffectInstance instance = event.getPotionEffect();
        if (instance != null) {
            MobEffectEvent.onEffectRemoved(event.getEntity(), instance.getEffect());
        }
    }
     *///?} else {
    @SubscribeEvent
    public static void onEffectAdded(Added event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectAdded(event.getEntity(), instance.getEffect(), instance.getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(Remove event) {
        MobEffectEvent.onEffectRemoved(event.getEntity(), event.getEffect());
    }

    @SubscribeEvent
    public static void onEffectExpired(Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null) {
            MobEffectEvent.onEffectRemoved(event.getEntity(), instance.getEffect());
        }
    }
    //?}
}
