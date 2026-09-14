package com.elfmcys.yesstevemodel.platform.neoforge.event212;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import java.util.WeakHashMap;

/**
 * render-state → 玩家实体 反查缓存（21.2 混合形态独占，批二 c-2）。
 * 异包孪生（event212，批二 a「异包策略」先例）：21.3+ 版在 src/neoforge-1213/java
 * （原包 event，走 neoforge renderstate 注册机制），21.2 无
 * net.neoforged.neoforge.client.renderstate 包与 RegisterRenderStateModifiersEvent
 *（21.3 才引入，21.2.1-beta-sources 零命中）→ 反查改由
 * com.elfmcys.yesstevemodel.mixin.client.PlayerRenderStateStashMixin 在 vanilla
 * PlayerRenderer.extractRenderState（1.21.2 :169）HEAD stash 实体。
 * WeakHashMap 弱引用防泄漏（与 1213 版同口径）。
 */
public final class PlayerRenderStateEntityCache {
    private static final WeakHashMap<LivingEntityRenderState, AbstractClientPlayer> BY_STATE = new WeakHashMap<>();

    private PlayerRenderStateEntityCache() {
    }

    public static void put(LivingEntityRenderState state, AbstractClientPlayer player) {
        BY_STATE.put(state, player);
    }

    public static AbstractClientPlayer get(LivingEntityRenderState state) {
        return BY_STATE.get(state);
    }
}
