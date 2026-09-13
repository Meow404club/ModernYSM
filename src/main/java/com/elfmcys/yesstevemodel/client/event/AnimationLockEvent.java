package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SPlayAnimationPacket;
import net.minecraft.client.Minecraft;
// 1.21.2 输入重构：Input 变 common record（world.entity.player），客户端实际类型=
// ClientInput（impulse 字段挂 ClientInput，vanilla-1.21.3 ClientInput.java:9-12/
// LocalPlayer.java:121 实证）
//? if <1.21.2
import net.minecraft.client.player.Input;
//? if >=1.21.2 {
/*import net.minecraft.client.player.ClientInput;
 *///?}
import net.minecraft.client.player.LocalPlayer;
//? if neoforge
/*import net.neoforged.neoforge.client.event.InputEvent;*/
//? if forge
import net.minecraftforge.client.event.InputEvent;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if neoforge && <1.20.5
/*import net.neoforged.neoforge.event.TickEvent;*/
//? if forge
import net.minecraftforge.event.TickEvent;

public class AnimationLockEvent {

    private static boolean animationLocked = false;

    private AnimationLockEvent() {
    }

    public static void register() {
        // ClientRawInputEvent.KEY_PRESSED → InputEvent.Key（不可取消，原恒 pass）
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(AnimationLockEvent::onKeyEvent);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(AnimationLockEvent::onKeyEvent);
        // ClientTickEvent.CLIENT_POST → TickEvent.ClientTickEvent phase END
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(AnimationLockEvent::onClientTickEvent);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(AnimationLockEvent::onClientTickEvent);
    }

    //? if <1.19.2
    /*private static void onKeyEvent(InputEvent.KeyInputEvent event) {*/
    //? if >=1.19.2
    private static void onKeyEvent(InputEvent.Key event) {
        // 1.21.9 KeyMapping.matches(int,int) → matches(KeyEvent)（2110 KeyMapping.java:255）
        //? if >=21.9
        /*if (YesSteveModel.isAvailable() && event.getAction() == 1 && AnimationRouletteKey.KEY_LOCK.matches(new net.minecraft.client.input.KeyEvent(event.getKey(), event.getScanCode(), 0))) {*/
        //? if <21.9
        if (YesSteveModel.isAvailable() && event.getAction() == 1 && AnimationRouletteKey.KEY_LOCK.matches(event.getKey(), event.getScanCode())) {
            animationLocked = !animationLocked;
        }
    }

    // ClientTickEvent.CLIENT_POST：neoforge 1.20.5+ = ClientTickEvent.Post（TickEvent.Phase 拆分）
    //? if neoforge && >=1.20.5 {
    /*private static void onClientTickEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        onClientTick(Minecraft.getInstance());
    }*/
    //? }
    //? if neoforge && <1.20.5
    /*private static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        onClientTick(Minecraft.getInstance());
    }*/
    //? if forge {
    private static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        onClientTick(Minecraft.getInstance());
    }
    //? }

    private static void onClientTick(Minecraft client) {
        LocalPlayer localPlayer;
        if (YesSteveModel.isAvailable() && !animationLocked && (localPlayer = client.player) != null && isPlayerMoving(localPlayer)) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                if (cap.isModelSwitching()) {
                    cap.clearModelSwitch();
                    if (NetworkHandler.isClientConnected()) {
                        NetworkHandler.sendToServer(C2SPlayAnimationPacket.createDefault());
                    }
                }
            });
        }
    }

    public static boolean isPlayerMoving(LocalPlayer localPlayer) {
        //? if >=21.3 && <21.5 {
        /*ClientInput input = localPlayer.input;
        return input != null && (isSignificantImpulse(input.leftImpulse) || isSignificantImpulse(input.forwardImpulse)
                || input.keyPresses.jump() || input.keyPresses.shift());*/
        //?}
        // 1.21.5 ClientInput 冲量字段删除（21.5 ClientInput 仅 keyPresses/moveVector，ClientInput.java:9-24）：
        // leftImpulse/forwardImpulse 语义 = moveVector x/y（ClientInput.getMoveVector），判定阈值同 1.0E-5
        //? if >=21.5 {
        /*ClientInput input = localPlayer.input;
        return input != null && (isSignificantImpulse(input.getMoveVector().x) || isSignificantImpulse(input.getMoveVector().y)
                || input.keyPresses.jump() || input.keyPresses.shift());*/
        //?}
        //? if <1.21.2
        Input input = localPlayer.input;
        //? if <1.21.2
        return input != null && (isSignificantImpulse(input.leftImpulse) || isSignificantImpulse(input.forwardImpulse) || input.jumping || input.shiftKeyDown);
    }

    private static boolean isSignificantImpulse(float impulse) {
        return Math.abs(impulse) > 1.0E-5f;
    }

    public static void toggleLock() {
        animationLocked = !animationLocked;
    }

    public static boolean isLocked() {
        return animationLocked;
    }
}
