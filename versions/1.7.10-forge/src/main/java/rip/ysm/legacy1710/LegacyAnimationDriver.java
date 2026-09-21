package rip.ysm.legacy1710;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * 1.7.10 基础动画驱动（legacy-1710-l1-render）。
 *
 * 1.12.2 LegacyAnimationDriver 的 1.7.10 twin：limbSwing（走跑相位）+
 * limbSwingAmount（幅度，1.7.10 EntityLivingBase 公开字段，
 * vanilla-mc-1.7.10 EntityLivingBase.java:71-72 / RendererLivingEntity.doRender:99-100
 * 同款插值）驱动四肢骨骼 rotationX 摆动，写入 boneParams 12 float/骨契约。
 * 完整 molang 状态机 = L2。
 *
 * ponytail: 无动画文件求值，骨架近似：四肢按名字匹配 sin 摆动，躯干/头静止。
 */
public final class LegacyAnimationDriver {

    private LegacyAnimationDriver() {
    }

    public static void tick(EntityPlayer player, float partialTick, long animTick) {
        float[] params = LegacyModelState.currentBoneParams();
        if (params == null) {
            return;
        }

        // 1.7.10 行走相位（RendererLivingEntity.doRender:99-100 同款插值）
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
            if (player.isSprinting()) {
                limbSwingAmount = Math.min(1.0f, limbSwingAmount * 1.35f);
            }
        }

        float swing = MathHelper.sin(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

        int boneCount = LegacyModelState.boneCount();
        for (int i = 0; i < boneCount; i++) {
            int p = i * 12;
            if (p + 11 >= params.length) {
                break;
            }
            String name = LegacyModelState.boneName(i);
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
        if (animTick % 40 == 1) {
            int limbs = 0;
            for (int i = 0; i < boneCount && (i * 12 + 11) < params.length; i++) {
                if (params[i * 12] != 0f) {
                    limbs++;
                }
            }
            System.out.printf("[ysm-legacy1710] animTick=%d limbSwing=%.4f limbSwingAmount=%.4f movingBones=%d bones=%d%n",
                    animTick, limbSwing, limbSwingAmount, limbs, boneCount);
        }
    }
}
