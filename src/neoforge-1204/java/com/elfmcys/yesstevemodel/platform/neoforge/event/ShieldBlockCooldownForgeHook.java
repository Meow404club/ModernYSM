package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.client.event.ShieldBlockCooldownEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.ShieldBlockEvent;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。ShieldBlockEvent 与 LivingTickEvent 同名同方法面
 * （20.4.251 javap 实证）；forge &lt;1.20 的 LivingUpdateEvent 分支不进孪生。
 */
@Mod.EventBusSubscriber
public final class ShieldBlockCooldownForgeHook {
    private ShieldBlockCooldownForgeHook() {
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        ShieldBlockCooldownEvent.onShieldBlock(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ShieldBlockCooldownEvent.onLivingTick(event.getEntity());
    }
}
