package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

import net.minecraft.entity.player.EntityPlayer;

/**
 * 1.12.2 世界动画驱动（wave-d-anim1 起委托 {@link LegacyWorldAnimationController}）。
 *
 * <p>本类保留职责=从 1.12.2 EntityPlayer 公开字段按 partialTick 插值行走相位
 * （vanilla-mc-1.12.2 RenderLivingBase.doRender:110-117 同款插值：limbSwingAmount
 * 步幅强度/limbSwing 相位），供状态机的 walk/swim/sneak 移幅门与程序化回退使用；
 * 状态裁决/转场/采样全部在 {@link LegacyWorldAnimationController}（19 状态优先级表
 * +BEGINNING/RUNNING/ENDING 三相，主线 AnimationRegister/AnimationControllerInstance
 * 对位）。GUI 预览链不经此处（LegacyCardPreview 直用 LegacyAnimationSampler）。
 */
public final class LegacyAnimationDriver {

    private LegacyAnimationDriver() {
    }

    public static void tick(EntityPlayer player, float partialTick, GeoModel model,
                            float[] params, ClientModelInfo bundle) {
        float limbSwing = 0.0f;
        float limbSwingAmount = 0.0f;
        if (player != null) {
            float prev = player.prevLimbSwingAmount;
            float cur = player.limbSwingAmount;
            limbSwingAmount = prev + (cur - prev) * partialTick;
            limbSwing = player.limbSwing - cur * (1.0f - partialTick);
            if (limbSwingAmount > 1.0f) {
                limbSwingAmount = 1.0f;
            }
        }
        LegacyWorldAnimationController.tick(player, limbSwing, limbSwingAmount,
                model, params, bundle);
    }
}
