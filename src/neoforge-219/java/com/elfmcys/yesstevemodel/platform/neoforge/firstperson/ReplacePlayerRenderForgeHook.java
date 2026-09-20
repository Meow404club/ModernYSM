package com.elfmcys.yesstevemodel.platform.neoforge.firstperson;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import com.elfmcys.yesstevemodel.platform.neoforge.event.PlayerRenderStateEntityCache;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * 21.9~21.11 三线 ReplacePlayerRenderForgeHook 孪生（fpm-selfdrive-219-2111 卡）。
 * 异包（platform/neoforge/firstperson）原因=212/261 树先例：本类以 exclude 剔除
 * 2110/2111 树原件（build.moddev.gradle.kts fpm219 块），孪生必须异包才能与
 * exclude 共存；@EventBusSubscriber 注解自注册，包名无关。
 *
 * 与 2110/2111 原件的唯一分歧=旗标采样链消费窗：路由前 FirstPersonCompat
 * .beginState(state) 置当前 state 的 extract 期快照，路由后 endState() 复位——
 * submit 期 lazy 动画求值（AllHead setHidden 消费点共享 PlayerCapability:142 +
 * first_person_mod_hide molang 门）经 isFirstPersonActive()/shouldHideHead()
 * 读到的是快照而非直查（21.9+ FPM 2.6.0/2.7.2 旗标窗只盖 extract 期
 * WorldRendererMixin.java:79-143 实证，submit 期直查恒 false；spec 明确不复刻
 * 2128 的 render 期直查老语义）。其余逐行同原件（state 反查、FULL_BRIGHT 字面量
 * 口径照 2111 原件，2110/2111 双代共用）。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class ReplacePlayerRenderForgeHook {
    private ReplacePlayerRenderForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        AbstractClientPlayer player = PlayerRenderStateEntityCache.get(event.getRenderState());
        if (player == null) {
            return;
        }
        FirstPersonCompat.beginState(event.getRenderState());
        try {
            // 1.21.10 RenderPlayerEvent.Pre.getMultiBufferSource 删（submit 换代）→ renderBuffers 即时缓冲
            // 1.21.10 Pre 事件头参去 packedLight（2110 RenderLivingEvent.java:98 同构）→
            // 全亮常量替代（事件仅作取消闸，不参与实际置光）
            if (ReplacePlayerRenderEvent.onRenderPlayerPre(player, event.getPartialTick(), event.getPoseStack(), net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(), 15728880 /* LightTexture.FULL_BRIGHT（26.x 类删，常量字面量版本中立；26.1 LightCoordsUtil.FULL_BRIGHT 同值） */)) {
                event.setCanceled(true);
            }
        } finally {
            FirstPersonCompat.endState();
        }
    }
}
