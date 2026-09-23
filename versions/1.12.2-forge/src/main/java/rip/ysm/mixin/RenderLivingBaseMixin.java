package rip.ysm.mixin;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.ysm.legacy122.LegacyRenderHook;

/**
 * RenderLivingBase.renderModel 切面（wave-d-b2-texture-pose 接缝迁移）。
 *
 * 位置注记：本类在 twin 源集（src/twin/java，无 include 白名单、jar 打包必含）——
 * main 源集 include 白名单（build.legacy122.gradle.kts legacy122Include）未列
 * rip/ysm/mixin/**，放 main/src 会被静默剔除（ClassNotFoundException 实证）。
 *
 * Forge 14.23.x RenderPlayerEvent.Pre 在 RenderPlayer.doRender 顶触发（forge 1.12.x
 * patches/minecraft/net/minecraft/client/renderer/entity/RenderPlayer.java.patch
 * 原文：func_76986_a 首行 post Pre，cancel 即 return）——取消吞掉 super.doRender
 * 全链：renderLivingAt 定位/applyRotations（RenderPlayer 覆写：睡床对齐+死亡倾倒+
 * 鞘翅姿态）/prepareScale/setBrightness/renderLayers（盔甲披风鞘翅手持层）/名牌
 * 全部缺失，模型悬空在眼高（harness/out/wd-b1/122-fix-item5-06scale-item2-glow.png
 * 实证：模型浮空、脚下 vanilla 影子才是玩家真实位置）。
 *
 * B2 裁决=方向②改接缝（1.7.10 twin 同点先例 RendererLivingEntityMixin）：注超类
 * RenderLivingBase.renderModel（vanilla-mc-1.12.2 RenderLivingBase.java:184-201，
 * 调用点 doRender:136——prepareScale:99 之后、renderLayers:143 之前）cancellable，
 * vanilla 主链照跑仅模型绘制被取消——定位/睡/死/鞘翅姿态/盔甲披风鞘翅手持层/
 * 名牌（doRender:157 super.doRender passSpecialRender）全部继承，零自补代码。
 * 注超类而非 RenderPlayer：一处覆盖玩家/僵尸等全部 biped，hook 内
 * AbstractClientPlayer 闸门限定玩家。
 */
@Mixin(RenderLivingBase.class)
public abstract class RenderLivingBaseMixin {

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
