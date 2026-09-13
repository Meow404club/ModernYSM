package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.audio.ObjectPool;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import com.elfmcys.yesstevemodel.client.upload.UploadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if neoforge && <1.20.5
/*import net.neoforged.neoforge.event.TickEvent;*/
//? if forge
import net.minecraftforge.event.TickEvent;

public final class ClientTickEvent {

    private static int tickCount;

    private static int refreshRate = 60;

    private ClientTickEvent() {
    }

    public static void register() {
        // architectury ClientTickEvent.CLIENT_PRE 在 forge 端 = TickEvent.ClientTickEvent phase START
        //? if neoforge && <1.20.5
        /*NeoForge.EVENT_BUS.addListener(ClientTickEvent::onClientTickEvent);*/
        //? if neoforge && >=1.20.5
        /*NeoForge.EVENT_BUS.addListener(ClientTickEvent::onClientPreTickEvent);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(ClientTickEvent::onClientTick);
    }

    // 同简单名冲突：neoforge 1.20.5+ 的 ClientTickEvent 不能单类型 import（与本类同名），
    // 处理器签名一律 FQN
    //? if neoforge && >=1.20.5 {
    /*private static void onClientPreTickEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        onClientPreTick(Minecraft.getInstance());
    }*/
    //? }
    //? if neoforge && <1.20.5
    /*private static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        onClientPreTick(Minecraft.getInstance());
    }*/
    //? if forge {
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        onClientPreTick(Minecraft.getInstance());
    }
    //? }

    private static void onClientPreTick(Minecraft client) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        tickCount++;
        UploadManager.processPendingUploads();
        ModelUploadSession.tickCurrent();
        ClientModelManager.updateModelLoadingMode();
        ClientModelManager.flushPendingModels();
        ObjectPool.cleanup();
        refreshRate = client.getWindow().getRefreshRate();
        LocalPlayer localPlayer = client.player;
        if (localPlayer != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> cap.tickAnimations());
        }
    }

    public static int getTickCount() {
        return tickCount;
    }

    public static int getRefreshRate() {
        return refreshRate;
    }
}
