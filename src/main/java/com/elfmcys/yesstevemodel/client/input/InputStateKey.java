package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.InputUtil;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import rip.ysm.api.PlatformAPI;

public class InputStateKey {

    public static volatile boolean[] keyStates = new boolean[349];

    public static volatile boolean[] mouseStates = new boolean[8];

    private InputStateKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // KEY_PRESSED → InputEvent.Key（不可取消）；MOUSE_CLICKED_PRE → InputEvent.MouseButton.Pre（可取消，原 handler 恒 pass，故不 setCanceled）
        MinecraftForge.EVENT_BUS.addListener(InputStateKey::onKeyEvent);
        MinecraftForge.EVENT_BUS.addListener(InputStateKey::onMouseEvent);
    }

    //? if >=1.17 {
    private static void onKeyEvent(InputEvent.Key event) {
    //?} else {
    /*private static void onKeyEvent(InputEvent.KeyInputEvent event) {*/
//?}
        onKeyInput(event.getKey(), event.getAction());
    }

    //? if >=1.17 {
    private static void onMouseEvent(InputEvent.MouseButton.Pre event) {
    //?} else {
    /*// 1.16.5 无 MouseButton.Pre，可取消的 pre-click 事件为 RawMouseEvent（getButton/getAction 同名）
    private static void onMouseEvent(InputEvent.RawMouseEvent event) {*/
//?}
        onMouseInput(event.getButton(), event.getAction());
    }

    private static void onKeyInput(int keyCode, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 32 <= keyCode && keyCode <= 348) {
            if (action == 1) {
                keyStates[keyCode] = true;
            } else if (action == 0) {
                keyStates[keyCode] = false;
            }
        }
    }

    private static void onMouseInput(int button, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 0 <= button && button <= 7) {
            if (action == 1) {
                mouseStates[button] = true;
            } else if (action == 0) {
                mouseStates[button] = false;
            }
        }
    }
}
