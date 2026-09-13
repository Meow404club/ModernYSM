package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；客户端 molang 状态能力）。20.4 重铸后 provider 为无状态查询函数，
 * 实体实例状态改由 WeakHashMap 逐实体缓存承载（实体卸载随 GC 回收）；
 * 仅客户端世界提供（forge 侧 attach 期 client-world 守卫语义，移入查询 lambda，见 ForgeCapabilityHooks 孪生）。
 */
public class PlayerCapabilityProvider {

    public static final EntityCapability<PlayerCapability, Void> PLAYER_CAP =
            EntityCapability.createVoid(Rl.of("yes_steve_model", "animatable"), PlayerCapability.class);

    private static final Map<AbstractClientPlayer, PlayerCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static PlayerCapabilityProvider forEntity(AbstractClientPlayer player) {
        return INSTANCES.computeIfAbsent(player, PlayerCapabilityProvider::new);
    }

    private PlayerCapability capability;

    private AbstractClientPlayer player;

    private PlayerCapabilityProvider(AbstractClientPlayer abstractClientPlayer) {
        this.player = abstractClientPlayer;
    }

    public PlayerCapability getCapability() {
        return getOrCreateCapability();
    }

    private PlayerCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new PlayerCapability(this.player);
            this.player = null;
        }
        return this.capability;
    }
}
