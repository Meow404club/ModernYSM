package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.platform.neoforge.event212.PlayerRenderStateEntityCache;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 21.2 混合形态独占（批二 c-2；RAW 源集，与 src/neoforge-212 cache 孪生配套）。
 * 21.2 vanilla 渲染链 extractRenderState(entity, state, partialTick) 后仅 state 下行，
 * 而 neoforge 侧 RegisterRenderStateModifiersEvent 21.3 才引入 → 在 vanilla 稳定点
 * PlayerRenderer.extractRenderState（1.21.2 :169 覆写）HEAD stash 实体，
 * 供 ReplacePlayerRenderForgeHook（1213 树版，同 FQCN 消费）按 state 反查。
 * 时序：extract 先于 LivingEntityRenderer.render 内事件 fire，缓存必命中。
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRenderStateStashMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
        at = @At("HEAD")
    )
    private void ysm$stashPlayerRenderState(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        PlayerRenderStateEntityCache.put(state, player);
    }
}
