package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
//? if >=1.19.4 {
import net.minecraft.client.gui.components.Tooltip;
//?}
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import rip.ysm.gui.YsmGui;
import rip.ysm.util.YsmCollections;

import java.util.List;

public class PauseScreenButtonBuilder {
    public static boolean isAndroid() {
        return YesSteveModel.isOnAndroid();
    }

    // Button 双轴构造收敛：1.20.1 Button.builder().bounds().build()（1.17+）↔
    // 1.16.5 new Button(x,y,w,h,label,onPress)（1.16.5 Button.java:12）；Tooltip 为 1.17+ API
    private static Button makeButton(int x, int y, int width, int height, Component label, Runnable action) {
        // Button.builder 1.19.0 起可用（f119 sources）；1.19.3 起 6 参构造删除（1.19.3 merged jar）
        //? if <1.19.3 {
        /*return new Button(x, y, width, height, label, button -> action.run());
         *///?} else {
        return Button.builder(label, button -> action.run()).bounds(x, y, width, height).build();
        //?}
    }

    @Nullable
    public static List<Button> createButtons(PauseScreen pauseScreen) {
        if (isAndroid()) {
            Minecraft minecraft = Minecraft.getInstance();
            Button buttonBuild = makeButton((pauseScreen.width / 2) - 69, pauseScreen.height - 35, 138, 30, YsmGui.trans("gui.yes_steve_model.skin"), () -> {
                if (GeneralConfig.DISCLAIMER_SHOW.get()) {
                    minecraft.setScreen(new DisclaimerScreen());
                } else {
                    minecraft.setScreen(new PlayerModelScreen());
                }
            });
            Button buttonBuild2 = makeButton((pauseScreen.width / 2) - 120, pauseScreen.height - 35, 50, 30, YsmGui.text("🔧"), () -> {
                minecraft.setScreen(new ExtraPlayerRenderScreen());
            });
            Button buttonBuild3 = makeButton((pauseScreen.width / 2) + 69, pauseScreen.height - 35, 50, 30, YsmGui.text("😄"), () -> {
                if (minecraft.player != null) {
                    PlayerCapability.get(minecraft.player).ifPresent(cap -> {
                        String str = cap.getModelId();
                        ModelAssembly modelAssembly = cap.getModelAssembly();
                        if (modelAssembly != null && !modelAssembly.getModelData().getModelProperties().getExtraAnimation().isEmpty()) {
                            minecraft.setScreen(new AnimationRouletteScreen(str, modelAssembly, cap));
                        }
                    });
                }
            });
            //? if >=1.19.4 {
            buttonBuild.setTooltip(Tooltip.create(YsmGui.trans("key.yes_steve_model.player_model.desc")));
            buttonBuild2.setTooltip(Tooltip.create(YsmGui.trans("key.yes_steve_model.open_extra_player_render.desc")));
            buttonBuild3.setTooltip(Tooltip.create(YsmGui.trans("key.yes_steve_model.animation_roulette.desc")));
            //?}
            return YsmCollections.immutableListOf(buttonBuild, buttonBuild2, buttonBuild3);
        }
        return null;
    }
}