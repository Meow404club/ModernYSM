package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

public final class ExtraPlayerRenderKey {

    // 26.3 SDL 替 GLFW：KEYSYM→KEYBOARD（值域=SDL scancode，InputConstants.KEY_P=19 同键位）
    //? if <26.3
    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.open_extra_player_render.desc", InputConstants.Type.KEYSYM, 80, "key.category.yes_steve_model");
    //? if >=26.3
    /*public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.open_extra_player_render.desc", InputConstants.Type.KEYBOARD, InputConstants.KEY_P, "key.category.yes_steve_model");*/

    private ExtraPlayerRenderKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // architectury ClientRawInputEvent.KEY_PRESSED 在 forge 端即 InputEvent.Key（不可取消，原 EventResult 被丢弃）
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(ExtraPlayerRenderKey::onKeyInput);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(ExtraPlayerRenderKey::onKeyInput);
    }

    //? if >=1.19 {
    private static void onKeyInput(InputEvent.Key event) {
        // 26.3 InputEvent.Key getScanCode→getKeycode（SDL 改名，nf-26.3 InputEvent.java:268）
        //? if <26.3
        handleKeyInput(event.getKey(), event.getScanCode(), event.getAction());
        //? if >=26.3
        /*handleKeyInput(event.getKey(), event.getKeycode(), event.getAction());*/
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
