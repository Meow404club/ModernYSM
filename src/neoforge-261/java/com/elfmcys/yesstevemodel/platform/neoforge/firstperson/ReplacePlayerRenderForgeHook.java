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
 * 26.1 三线 ReplacePlayerRenderForgeHook 孪生（fpm-26x-pr659 逆向追加单）。
 * 异包（platform/neoforge/firstperson）原因=212/2610 树先例：本类以 exclude
 * 剔除 2111 树原件（build.moddev.gradle.kts fpm261 块），孪生必须异包才能与
 * exclude 共存；@EventBusSubscriber 注解自注册，包名无关。
 *
 * 与 2111 原件（platform/neoforge/event/ReplacePlayerRenderForgeHook.java）的
 * 唯一分歧=旗标采样链消费窗：路由前 FirstPersonCompat.beginState(state) 置
 * 当前 state 的 extract 期快照，路由后 endState() 复位——submit 期 lazy 动画
 * 求值（AllHead setHidden 消费点 PlayerCapability:137 + first_person_mod_hide
 * molang 门）经 isFirstPersonActive()/shouldHideHead() 读到的是快照而非直查
 * （官方契约：extract 期采样、eval/submit 期消费快照，O0O0O0OOooOO00oOo0OooOOO
 * .java 快照工厂 + O0OoOoO0oo0O00O0ooo0o0OO.java:105-110）。其余逐行同原件
 * （state 反查、26.x 事件头参换代注记见原件 javadoc）。
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
