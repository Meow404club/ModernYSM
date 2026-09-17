package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.client.event.ShieldBlockCooldownEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.20.5+ 断裂：@Mod.EventBusSubscriber 删除（→ fml.common.EventBusSubscriber）；
 * 本文件为 1.21.1 代（ShieldBlockEvent 已更名 LivingShieldBlockEvent）；
 * LivingEvent.LivingTickEvent 删除（neoforge-1.20.6 LivingEvent.java 仅余 Jump/Visibility）；
 * 1.21.1 ShieldBlockEvent 更名 LivingShieldBlockEvent（neoforge-1.21.1:26），本文件为 1.21.1 代
 * → net.neoforged.neoforge.event.tick.EntityTickEvent.Post（全体实体逐 tick）+
 * instanceof LivingEntity 守卫还原 LivingTick 的触发域（非生物实体不再误入）。
 */
@EventBusSubscriber
public final class ShieldBlockCooldownForgeHook {
    private ShieldBlockCooldownForgeHook() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        ShieldBlockCooldownEvent.onShieldBlock(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            ShieldBlockCooldownEvent.onLivingTick(livingEntity);
        }
    }
}
