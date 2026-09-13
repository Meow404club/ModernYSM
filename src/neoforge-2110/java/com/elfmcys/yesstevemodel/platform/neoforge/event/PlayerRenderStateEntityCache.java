// 21.10 代（RAW，源 1218 树克隆）：PlayerRenderer → AvatarRenderer 改名（1.21.9+）
package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
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
 *（RegisterRenderStateModifiersEvent.registerEntityModifier，在 AvatarRenderer.extractRenderState
 * 后回调）stash 实体，WeakHashMap 弱引用防泄漏。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class PlayerRenderStateEntityCache {
    private static final WeakHashMap<LivingEntityRenderState, AbstractClientPlayer> BY_STATE = new WeakHashMap<>();

    private PlayerRenderStateEntityCache() {
    }

    @SubscribeEvent
    public static void onRegisterModifiers(RegisterRenderStateModifiersEvent event) {
        // AvatarRenderer 泛型化（1.21.9 AvatarRenderer<T extends Avatar & ClientAvatarEntity>）
        // → raw TypeToken.of(...) 推断失败（2110 编译实证）→ 匿名子类显式定位 E/S
        // 21.10 校验器要求 user 类型实参为各 bound 的超型（RegisterRenderStateModifiersEvent
        // .java:114-124 ensureParametersMatchBounds），AvatarRenderer&lt;AbstractClientPlayer&gt;
        // 不满足 → 通配符 &lt;?&gt; 让 E/S 落到 bound，lambda 内收窄转型（运行时仅玩家/人偶触发）
        event.registerEntityModifier(
                new com.google.common.reflect.TypeToken<net.minecraft.client.renderer.entity.player.AvatarRenderer<?>>() {},
                (entity, state) -> BY_STATE.put((net.minecraft.client.renderer.entity.state.LivingEntityRenderState) state, (net.minecraft.client.player.AbstractClientPlayer) entity));
    }

    public static AbstractClientPlayer get(LivingEntityRenderState state) {
        return BY_STATE.get(state);
    }
}
