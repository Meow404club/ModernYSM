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
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.gui.ModernAnimationRouletteScreen;

public final class AnimationRouletteKey {

    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.animation_roulette.desc", InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");

    public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameAlt("key.yes_steve_model.lock_roulette.desc", InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");

    private AnimationRouletteKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        // architectury ClientRawInputEvent.KEY_PRESSED 在 forge 端即 InputEvent.Key（不可取消，原 EventResult 被丢弃）
        MinecraftForge.EVENT_BUS.addListener(AnimationRouletteKey::onKeyInput);
    }

    //? if >=1.19.2 {
    private static void onKeyInput(InputEvent.Key event) {
        handleKeyInput(event.getKey(), event.getScanCode(), event.getAction());
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
