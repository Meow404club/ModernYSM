package rip.ysm.legacy1710.mixin;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.ysm.legacy1710.LegacyRenderHook;

/**
 * RendererLivingEntity.renderModel 切面（legacy-1710-l1-render）。
 *
 * 1.7.10 无 RenderPlayerEvent（1.12.2 接缝不可用），直接注 RendererLivingEntity
 * 的模型绘制点（vanilla-mc-1.7.10 RendererLivingEntity.java:223-227：renderModel
 * = bindEntityTexture + mainModel.render）。模型就绪且实体为玩家时 cancel 掉
 * vanilla 绘制，LegacyRenderHook.takeover 在同一 GL 状态内画 YSM 模型。
 * 注超类而非 RenderPlayer：切面一处覆盖玩家/僵尸等全部 biped——L1 验收看玩家，
 * hook 内 AbstractClientPlayer 闸门限定玩家。
 */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    @Inject(
            method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ysm$takeoverRender(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch, float scale,
                                    CallbackInfo ci) {
        if (LegacyRenderHook.takeover(entity, ageInTicks)) {
            ci.cancel();
        }
    }
}
