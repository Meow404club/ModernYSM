package com.elfmcys.yesstevemodel.platform.forge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.ModScreenEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModScreenForgeHook {

    private ModScreenForgeHook() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onProcessIMC(InterModProcessEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        InterModComms.getMessages(YesSteveModel.MOD_ID).findFirst().ifPresent(message -> {
            // IMCMessage 取值器 1.19+ method()/messageSupplier() ↔ 1.16.5 getMethod()/getMessageSupplier()（javap 实证）
            //? if <1.17 {
            /*if (ModScreenEvent.IMC_METHOD.equals(message.getMethod())) {
                Object screenObj = message.getMessageSupplier().get();
                 *///?} else {
            if (ModScreenEvent.IMC_METHOD.equals(message.method())) {
                Object screenObj = message.messageSupplier().get();
                //?}
                if (screenObj instanceof Screen) {
                    Screen screen = (Screen) screenObj;
                    ModScreenEvent.setReceivedScreen(screen);
                }
            }
        });
    }
}
