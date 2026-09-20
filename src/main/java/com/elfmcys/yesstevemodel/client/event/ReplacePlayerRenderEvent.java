package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.YsmEntity;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if <26.2
import net.minecraft.client.renderer.MultiBufferSource;
// 26.2 submit-dag 换代：collector 形 twin
//? if >=26.2
/*import net.minecraft.client.renderer.SubmitNodeCollector;*/
import net.minecraft.world.entity.player.Player;
//? if (>=21.2 && <26.2)
/*import com.elfmcys.yesstevemodel.platform.neoforge.firstperson.FirstPersonCompat;*/
//? if <21.2 || >=26.2
import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.playeranimator.PlayerAnimatorCompat;
import rip.ysm.compat.realcamera.RealCameraCompat;

public class ReplacePlayerRenderEvent {

    private ReplacePlayerRenderEvent() {
    }

    //? if <26.2 {
    public static boolean onRenderPlayerPre(Player entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!YesSteveModel.isAvailable()) {
            return false;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (entity.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get().booleanValue()) {
            return false;
        }
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || entity.isSpectator()) {
            return false;
        }
        boolean[] cancelled = {false};
        PlayerCapability.get(entity).ifPresent(cap -> {
            if (cap.isModelActive()) {
                if (!CameraUtil.isFirstPerson(cap)
                        || FirstPersonCompat.isFirstPersonActive()
                        || RealCameraCompat.isActive()
                        || GeneralConfig.DISABLE_EXTERNAL_FP_ANIM.get().booleanValue()
                        || !PlayerAnimatorCompat.isPlayerAnimated(localPlayer)) {
                    cancelled[0] = true;
                    RendererManager.getPlayerRenderer().render(entity, YsmEntity.getYRot(entity), partialTick, poseStack, bufferSource, packedLight);
                }
            }
        });
        return cancelled[0];
    }
    //?}

    // 26.2 collector 形 twin（RenderPlayerEvent 携带 SubmitNodeCollector，取消闸语义不变）
    //? if >=26.2 {
    /*public static boolean onRenderPlayerPre(Player entity, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight) {
        if (!YesSteveModel.isAvailable()) {
            return false;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (entity.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get().booleanValue()) {
            return false;
        }
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || entity.isSpectator()) {
            return false;
        }
        boolean[] cancelled = {false};
        PlayerCapability.get(entity).ifPresent(cap -> {
            if (cap.isModelActive()) {
                if (!CameraUtil.isFirstPerson(cap)
                        || FirstPersonCompat.isFirstPersonActive()
                        || RealCameraCompat.isActive()
                        || GeneralConfig.DISABLE_EXTERNAL_FP_ANIM.get().booleanValue()
                        || !PlayerAnimatorCompat.isPlayerAnimated(localPlayer)) {
                    cancelled[0] = true;
                    RendererManager.getPlayerRenderer().render(entity, YsmEntity.getYRot(entity), partialTick, poseStack, bufferSource, packedLight);
                }
            }
        });
        return cancelled[0];
    }*/
    //?}
}
