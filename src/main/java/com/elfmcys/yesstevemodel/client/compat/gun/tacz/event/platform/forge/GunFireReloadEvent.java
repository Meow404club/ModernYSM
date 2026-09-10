package com.elfmcys.yesstevemodel.client.compat.gun.tacz.event.platform.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.platform.forge.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.platform.forge.TouhouLittleMaidCompat;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GunFireReloadEvent {
    @SubscribeEvent
    public void onGunFire(GunFireEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        shooter.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            cap.setExtraRenderFlag(true);
        });
        TouhouLittleMaidCompat.syncMaidState(shooter);
    }

    @SubscribeEvent
    public void onGunMelee(GunMeleeEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        shooter.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            cap.setExtraRenderFlag(true);
        });
        TouhouLittleMaidCompat.syncMaidState(shooter);
    }

    @SubscribeEvent
    public void onGunReload(GunReloadEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        entity.getCapability(PlayerCapabilityProvider.PLAYER_CAP).ifPresent(cap -> {
            cap.setExtraRenderFlag(true);
        });
        TouhouLittleMaidCompat.syncMaidState(entity);
    }
}