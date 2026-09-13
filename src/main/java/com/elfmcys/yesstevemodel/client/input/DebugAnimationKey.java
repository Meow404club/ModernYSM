package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if neoforge
/*import net.neoforged.neoforge.client.event.InputEvent;*/
//? if forge
import net.minecraftforge.client.event.InputEvent;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
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
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(DebugAnimationKey::onKeyInput);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(DebugAnimationKey::onKeyInput);
    }

    //? if >=1.19.2 {
    private static void onKeyInput(InputEvent.Key event) {
        handleKeyInput(event.getKey(), event.getScanCode(), event.getAction());
    }
    //?} else {
    /*// 1.16.5 无 InputEvent.Key（1.19.3+），键盘事件为 InputEvent.KeyInputEvent（访问器同名同义）
    private static void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKeyInput(event.getKey(), event.getScanCode(), event.getAction());
    }*/
//?}

    private static void handleKeyInput(int keyCode, int scanCode, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_MAPPING)) {
            if (!AnimationDebugOverlay.isDebugActive()) {
                AnimationDebugOverlay.tryUpdateFromHitResult();
            } else {
                AnimationDebugOverlay.clearActiveModel();
            }
        }
    }
}
