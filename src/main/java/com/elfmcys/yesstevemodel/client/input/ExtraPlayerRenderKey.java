package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

public final class ExtraPlayerRenderKey {

    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.open_extra_player_render.desc", InputConstants.Type.KEYSYM, 80, "key.category.yes_steve_model");

    private ExtraPlayerRenderKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // architectury ClientRawInputEvent.KEY_PRESSED 在 forge 端即 InputEvent.Key（不可取消，原 EventResult 被丢弃）
        MinecraftForge.EVENT_BUS.addListener(ExtraPlayerRenderKey::onKeyInput);
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
            Minecraft.getInstance().setScreen(new ExtraPlayerRenderScreen());
        }
    }
}
