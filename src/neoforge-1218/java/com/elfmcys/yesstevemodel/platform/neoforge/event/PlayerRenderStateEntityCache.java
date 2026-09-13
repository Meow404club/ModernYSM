// 21.8 代（RAW，源 1213 树克隆）：1.21.6+ EventBusSubscriber 删 bus 属性（单总线自动路由）
package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.WeakHashMap;

/**
 * render-state → 玩家实体 反查缓存（1.21.2+ 代独有，RAW 源集）。
 * NeoForge 1.21.2 起 vanilla 渲染链 createRenderState(entity,partialTick) 后仅 state 下行，
 * 事件面（RenderPlayerEvent/RenderLivingEvent）不再携带实体 → 经官方 extractor 机制
 *（RegisterRenderStateModifiersEvent.registerEntityModifier，在 PlayerRenderer.extractRenderState
 * 后回调）stash 实体，WeakHashMap 弱引用防泄漏。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class PlayerRenderStateEntityCache {
    private static final WeakHashMap<LivingEntityRenderState, AbstractClientPlayer> BY_STATE = new WeakHashMap<>();

    private PlayerRenderStateEntityCache() {
    }

    @SubscribeEvent
    public static void onRegisterModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(com.google.common.reflect.TypeToken.of(PlayerRenderer.class), (entity, state) -> BY_STATE.put(state, entity));
    }

    public static AbstractClientPlayer get(LivingEntityRenderState state) {
        return BY_STATE.get(state);
    }
}
