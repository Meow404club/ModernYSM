package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import rip.ysm.api.client.KeyMappingFactory;

public class InputUtil {
    public static boolean isKeyPressed(int keyCode, int scanCode, KeyMapping keyMapping) {
        return KeyMappingFactory.isActiveAndMatches(keyMapping, keyCode, scanCode);
    }

    public static boolean isPlayerReady() {
        Minecraft minecraft = Minecraft.getInstance();
        //? if <26.2
        if (minecraft.getOverlay() != null || minecraft.screen != null || !minecraft.mouseHandler.isMouseGrabbed()) {
        //? if >=26.2
        /*if (minecraft.gui.overlay() != null || minecraft.gui.screen() != null || !minecraft.mouseHandler.isMouseGrabbed()) {*/
            return false;
        }
        return minecraft.isWindowActive();
    }
}