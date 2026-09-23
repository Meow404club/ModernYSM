package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 1.7.10 世界动画驱动（wave-d-anim1-world-state-machine-1710 起委托
 * {@link LegacyWorldAnimationController}）。
 *
 * <p>本类保留职责=从 1.7.10 EntityPlayer 公开字段按 partialTick 插值行走相位
 * （vanilla-mc-1710 RendererLivingEntity.doRender:99-100 同款插值：limbSwingAmount
 * 步幅强度/limbSwing 相位；字段面 EntityLivingBase.java:70-72 public
 * prevLimbSwingAmount/limbSwingAmount/limbSwing），供状态机的 walk/swim/sneak
 * 移幅门与程序化回退使用；状态裁决/转场/采样全部在
 * {@link LegacyWorldAnimationController}（19 状态优先级表+BEGINNING/RUNNING/ENDING
 * 三相，主线 AnimationRegister/AnimationControllerInstance 对位）。原 limbSwing
 * sin 摆四肢路径收编为控制器内程序化回退（无动画数据模型专用）。
 * GUI 预览链不经此处（LegacyCardPreview 直用 LegacyAnimationSampler）。
 */
public final class LegacyAnimationDriver {

    private LegacyAnimationDriver() {
    }

    public static void tick(EntityPlayer player, float partialTick, LegacyBakedModel model,
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
