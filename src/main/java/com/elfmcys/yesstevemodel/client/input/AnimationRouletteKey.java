package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
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
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.gui.ModernAnimationRouletteScreen;

public final class AnimationRouletteKey {

    //? if <26.3
    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.animation_roulette.desc", InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");
    //? if >=26.3
    /*// 26.3 SDL 替 GLFW：Type.KEYSYM 摘除（Type 枚举仅余 KEYBOARD/MOUSE，值域=SDL scancode，
// /tmp/vanilla-263 InputConstants.java:307 实证）→ KEYBOARD+InputConstants.KEY_* 常量
//（SDL scancode：GLFW_KEY_L(76)≡KEY_L(15) 等字母键映射）
    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.animation_roulette.desc", InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, "key.category.yes_steve_model");*/

    //? if <26.3
    public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameAlt("key.yes_steve_model.lock_roulette.desc", InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");
    //? if >=26.3
    /*public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameAlt("key.yes_steve_model.lock_roulette.desc", InputConstants.Type.KEYBOARD, InputConstants.KEY_L, "key.category.yes_steve_model");*/

    private AnimationRouletteKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // architectury ClientRawInputEvent.KEY_PRESSED 在 forge 端即 InputEvent.Key（不可取消，原 EventResult 被丢弃）
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(AnimationRouletteKey::onKeyInput);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(AnimationRouletteKey::onKeyInput);
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
    /*// 1.16.5 无 InputEvent.Key（1.19.3+），键盘事件为 InputEvent.KeyInputEvent（getKey/getScanCode/getAction 同名同义）
    private static void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKeyInput(event.getKey(), event.getScanCode(), event.getAction());
    }*/
//?}

    private static void handleKeyInput(int keyCode, int scanCode, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_ROULETTE)) {
            if (!NetworkHandler.isClientConnected() || ServerConfig.CAN_SWITCH_MODEL.get()) {
                if (TouhouLittleMaidCompat.isMaidChatAvailable()) {
                    TouhouLittleMaidCompat.openMaidChat();
                } else if (Minecraft.getInstance().player != null) {
                    PlayerCapability.get(Minecraft.getInstance().player).ifPresent(cap -> {
                        String modelId = cap.getModelId();
                        ModelAssembly modelAssembly = cap.getModelAssembly();
                        if (modelAssembly != null && !modelAssembly.getModelData().getModelProperties().getExtraAnimation().isEmpty()) {
                            if (Minecraft.getInstance().screen == null) {
                                if (GeneralConfig.effectiveModernRoulette()) {
                                    Minecraft.getInstance().setScreen(new ModernAnimationRouletteScreen(modelId, modelAssembly, cap));
                                } else {
                                    Minecraft.getInstance().setScreen(new AnimationRouletteScreen(modelId, modelAssembly, cap));
                                }
                            } else if (Minecraft.getInstance().screen instanceof AnimationRouletteScreen || Minecraft.getInstance().screen instanceof ModernAnimationRouletteScreen) {
                                Minecraft.getInstance().setScreen(null);
                            }
                        }
                    });
                }
            }
        }
    }
}
