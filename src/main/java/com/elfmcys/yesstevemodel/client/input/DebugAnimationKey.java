package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

public final class DebugAnimationKey {

    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.debug_animation.desc", InputConstants.Type.KEYSYM, 66, "key.category.yes_steve_model");

    private DebugAnimationKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // architectury ClientRawInputEvent.KEY_PRESSED 在 forge 端即 InputEvent.Key（不可取消，原 EventResult 被丢弃）
        MinecraftForge.EVENT_BUS.addListener(DebugAnimationKey::onKeyInput);
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_MAPPING)) {
            if (!AnimationDebugOverlay.isDebugActive()) {
                AnimationDebugOverlay.tryUpdateFromHitResult();
            } else {
                AnimationDebugOverlay.clearActiveModel();
            }
        }
    }
}
