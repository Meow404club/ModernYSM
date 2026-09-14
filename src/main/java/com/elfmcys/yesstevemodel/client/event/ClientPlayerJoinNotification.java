package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
//? if neoforge
/*import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;*/
//? if forge
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if neoforge && <1.20.5
/*import net.neoforged.neoforge.event.TickEvent;*/
//? if forge
import net.minecraftforge.event.TickEvent;

public final class ClientPlayerJoinNotification {

    private static final int HANDSHAKE_PROBE_COUNT = 3;

    private static boolean notified = false;
    private static int handshakeProbeIndex = -1;
    private static int handshakeProbeDelay;
    private static int handshakeElapsed;
    private static int sessionId;

    private ClientPlayerJoinNotification() {
    }

    private static int handshakeTimeoutTicks() {
        if (GeneralConfig.HANDSHAKE_TIMEOUT == null) {
            return 100;
        }
        return Math.max(HANDSHAKE_PROBE_COUNT, (int) Math.round(GeneralConfig.HANDSHAKE_TIMEOUT.get() * 20.0d));
    }

    private static int probeInterval() {
        return Math.max(1, handshakeTimeoutTicks() / (HANDSHAKE_PROBE_COUNT + 1));
    }

    public static void register() {
        // ClientPlayerEvent.CLIENT_PLAYER_JOIN/QUIT → ClientPlayerNetworkEvent.LoggingIn/LoggingOut（均不可取消）
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onLoggingIn);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onLoggingIn);
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onLoggingOut);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onLoggingOut);
        // ClientTickEvent.CLIENT_PRE → TickEvent.ClientTickEvent phase START
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onClientTickEvent);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(ClientPlayerJoinNotification::onClientTickEvent);
    }

    //? if <1.19
    /*private static void onLoggingIn(ClientPlayerNetworkEvent.LoggedInEvent event) {*/
    //? if >=1.19
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        onPlayerJoin(event.getPlayer());
    }

    /**
     * LoggingOut 的 getPlayer() 可为 null（新建集成服/连接远程服时也会触发，
     * forge-api 1.20.1 ClientPlayerNetworkEvent.java:85-125），原逻辑本就不读该参数。
     */
    //? if <1.19
    /*private static void onLoggingOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {*/
    //? if >=1.19
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        onPlayerQuit(event.getPlayer());
    }

    // ClientTickEvent.CLIENT_PRE：neoforge 1.20.5+ = ClientTickEvent.Pre（TickEvent.Phase 拆分）
    //? if neoforge && >=1.20.5 {
    /*private static void onClientTickEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        onClientTick(Minecraft.getInstance());
    }*/
    //? }
    //? if neoforge && <1.20.5
    /*private static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        onClientTick(Minecraft.getInstance());
    }*/
    //? if forge {
    private static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        onClientTick(Minecraft.getInstance());
    }
    //? }

    private static void onPlayerJoin(LocalPlayer player) {
        if (notified) {
            return;
        }
        ClientModelManager.runPendingModelCallback();
        notified = true;
        if (!YesSteveModel.isAvailable()) {
//? if neoforge
/*            ClientModelManager.sendUnavailableMessage();*/
//? if forge
            YesSteveModel.sendUnavailableMessage();
            return;
        }
        if (Minecraft.getInstance().isLocalServer()) {
            return;
        }
        if (ClientOnlyMode.isForced()) {
            ClientOnlyMode.activateStandalone();
            return;
        }
        handshakeProbeIndex = 0;
        handshakeProbeDelay = 0;
        handshakeElapsed = 0;
        int currentSession = ++sessionId;
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(60000L);
                Minecraft.getInstance().execute(() -> {
                    if (currentSession != sessionId || ClientOnlyMode.isActive()) {
                        return;
                    }
                    LocalPlayer localPlayer = Minecraft.getInstance().player;
                    // isAcceptingMessages 1.19.4 起（1.19.3 无该访问器，f1193 实证 → isConnected 同判）（1194 ClientPacketListener.java:2413 = connection.isConnected()），
                    // 1.16.5~1.19.2 直调 Connection.isConnected（等价展开）
                    //? if <1.17
                    /*if (localPlayer != null && localPlayer.connection.getConnection().isConnected() && !NetworkHandler.isConnectionValid(localPlayer.connection.getConnection())) {*/
                    //? if >=1.17 && <1.19.3
                    /*if (localPlayer != null && localPlayer.connection.getConnection().isConnected() && !NetworkHandler.isConnectionValid(localPlayer.connection.getConnection())) {*/
                    //? if >=1.19.3 && <1.19.4
                    /*if (localPlayer != null && localPlayer.connection.getConnection().isConnected() && !NetworkHandler.isConnectionValid(localPlayer.connection.getConnection())) {*/
                    //? if >=1.19.4
                    if (localPlayer != null && localPlayer.connection.isAcceptingMessages() && !NetworkHandler.isConnectionValid(localPlayer.connection.getConnection())) {
                        YsmText.sendSystemMessage(localPlayer, YsmText.translatable("message.yes_steve_model.client.server_not_found"));
                    }
                });
            } catch (InterruptedException ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void onClientTick(Minecraft client) {
        ClientModelManager.applyClientOnlySelection();
        if (handshakeProbeIndex < 0 || client.isLocalServer()) {
            return;
        }
        LocalPlayer player = client.player;
//? if <1.17
        /*if (player == null || !player.connection.getConnection().isConnected()) {*/
//? if >=1.17 && <1.19.3
        /*if (player == null || !player.connection.getConnection().isConnected()) {*/
//? if >=1.19.3 && <1.19.4
        /*if (player == null || !player.connection.getConnection().isConnected()) {*/
//? if >=1.19.4
        if (player == null || !player.connection.isAcceptingMessages()) {
            return;
        }
        if (NetworkHandler.isConnectionValid(player.connection.getConnection())) {
            handshakeProbeIndex = -1;
            return;
        }
        handshakeElapsed++;
        if (handshakeElapsed >= handshakeTimeoutTicks()) {
            handshakeProbeIndex = -1;
            ClientOnlyMode.activateStandalone();
            return;
        }
        if (handshakeProbeIndex >= HANDSHAKE_PROBE_COUNT) {
            return;
        }
        if (handshakeProbeDelay > 0) {
            handshakeProbeDelay--;
            return;
        }
        NetworkHandler.sendVersionCheck(player.connection.getConnection());
        handshakeProbeIndex++;
        handshakeProbeDelay = probeInterval();
    }

    private static void onPlayerQuit(LocalPlayer player) {
        handshakeProbeIndex = -1;
        sessionId++;
        if (notified) {
            notified = false;
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            ClientModelManager.resetSync();
        }
    }
}
