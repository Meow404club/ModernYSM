package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

/**
 * 1.12.2 基础动画驱动（legacy-1222-l1-render commit 4）。
 *
 * L1 验收=待机/走/跑的形变可见：limbSwing（走跑相位）+ limbSwingAmount（幅度，
 * 1.12.2 EntityPlayer 公开字段，vanilla-mc-1.12.2 RenderLivingBase.doRender:110-117
 * 同款插值）驱动四肢骨骼 rotationX/Z 摆动，写入 boneParams 12 float/骨契约
 * （offset0-2 旋转/3-5 平移/6-8 缩放/9-10 隐藏旗标——NativeModelRenderer 消费侧同款）。
 * 完整 molang 状态机= AnimatableEntity.tick 链（L2）。
 *
 * ponytail: 无动画文件求值（molang/AnimationController 链 L2 接入），骨架近似：
 * 四肢按名字匹配 sin 摆动，躯干/头静止。
 */
public final class LegacyAnimationDriver {

    private LegacyAnimationDriver() {
    }

    public static void tick(EntityPlayer player, float partialTick) {
        GeoModel model = LegacyModelState.mainModel();
        float[] params = LegacyModelState.currentBoneParams();
        if (model == null || params == null || model.bakedBones == null) {
            return;
        }
        long tick = LegacyModelState.nextAnimTick();

        // 1.12.2 行走相位（RenderLivingBase.doRender 同款插值）：
        // limbSwingAmount=步幅强度（0=静止），limbSwing=相位
        float limbSwingAmount = 0.0f;
        float limbSwing = 0.0f;
        if (player != null) {
            float prev = player.prevLimbSwingAmount;
            float cur = player.limbSwingAmount;
            limbSwingAmount = prev + (cur - prev) * partialTick;
            limbSwing = player.limbSwing - cur * (1.0f - partialTick);
            if (limbSwingAmount > 1.0f) {
                limbSwingAmount = 1.0f;
            }
            float speed = limbSwingAmount * 2.0f;
            if (player.isSprinting()) {
                // 跑步加幅（EntityPlayerSP.mc 为 protected，走 EntityPlayer.isSprinting 公开面）
                limbSwingAmount = Math.min(1.0f, limbSwingAmount * 1.35f);
            }
            // ponytail: speed 未单独参与相位（vanilla 以 limbSwing 累计），保留变量名对位
            speed = speed * 0.0f + speed;
        }

        float swing = MathHelper.sin(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

        for (int i = 0; i < model.bakedBones.size(); i++) {
            int p = i * 12;
            if (p + 11 >= params.length) {
                break;
            }
            String name = model.bakedBones.get(i).name;
            // 缺省静止：旋转清零、平移/缩放置中性
            params[p] = 0f; params[p + 1] = 0f; params[p + 2] = 0f;
            params[p + 3] = 0f; params[p + 4] = 0f; params[p + 5] = 0f;
            params[p + 6] = 1f; params[p + 7] = 1f; params[p + 8] = 1f;
            params[p + 9] = 0f; params[p + 10] = 0f; params[p + 11] = 0f;

            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            // Bedrock 命名惯例：leftArm/rightArm/leftLeg/rightLeg（大小写不敏感匹配）
            if (lower.contains("rightarm") || lower.contains("armright")) {
                params[p] = -swing;
            } else if (lower.contains("leftarm") || lower.contains("armleft")) {
                params[p] = swing;
            } else if (lower.contains("rightleg") || lower.contains("legright")) {
                params[p] = swing;
            } else if (lower.contains("leftleg") || lower.contains("legleft")) {
                params[p] = -swing;
            }
        }

        // 验收打点：动画 tick 计数+骨骼数值原文行（40 帧一次防刷屏）
        if (tick % 40 == 1) {
            int limbs = 0;
            for (int i = 0; i < model.bakedBones.size() && (i * 12 + 11) < params.length; i++) {
                if (params[i * 12] != 0f) {
                    limbs++;
                }
            }
            System.out.printf("[ysm-legacy122] animTick=%d limbSwing=%.4f limbSwingAmount=%.4f movingBones=%d bones=%d%n",
                    tick, limbSwing, limbSwingAmount, limbs, model.bakedBones.size());
        }
    }
}
