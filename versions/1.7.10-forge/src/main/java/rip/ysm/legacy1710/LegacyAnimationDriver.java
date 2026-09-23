package rip.ysm.legacy1710;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * 1.7.10 基础动画驱动（legacy-1710-l1-render；L2a 升级按模型驱动）。
 *
 * 1.12.2 LegacyAnimationDriver 的 1.7.10 twin：limbSwing（走跑相位）+
 * limbSwingAmount（幅度，1.7.10 EntityLivingBase 公开字段，
 * vanilla-mc-1.7.10 EntityLivingBase.java:71-72 / RendererLivingEntity.doRender:99-100
 * 同款插值）驱动四肢骨骼 rotationX 摆动，写入 boneParams 12 float/骨契约。
 * GUI 预览动画不走本类（LegacyAnimationSampler 按 preview_animation 采样驱动，
 * 122 线 M-U2 r3 同分野）；世界内行走驱动=本类。
 *
 * ponytail: 无动画文件求值，骨架近似：四肢按名字匹配 sin 摆动，躯干/头静止。
 */
public final class LegacyAnimationDriver {

    private LegacyAnimationDriver() {
    }

    /** 按模型+骨参数面驱动（L2a：被渲染玩家可异模型，模型由 RenderHook 指定）。 */
    public static void tick(EntityPlayer player, float partialTick, LegacyBakedModel model, float[] params) {
        if (model == null || params == null || model.bones.isEmpty()) {
            return;
        }
        long tick = LegacyModelState.nextAnimTick();

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

        for (int i = 0; i < model.bones.size(); i++) {
            int p = i * 12;
            if (p + 11 >= params.length) {
                break;
            }
            String name = model.bones.get(i).name;
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
            for (int i = 0; i < model.bones.size() && (i * 12 + 11) < params.length; i++) {
                if (params[i * 12] != 0f) {
                    limbs++;
                }
            }
            System.out.printf("[ysm-legacy1710] animTick=%d partialTick=%.4f limbSwing=%.4f limbSwingAmount=%.4f movingBones=%d bones=%d%n",
                    tick, partialTick, limbSwing, limbSwingAmount, limbs, model.bones.size());
        }
    }
}
