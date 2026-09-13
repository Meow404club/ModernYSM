package com.elfmcys.yesstevemodel.client.event;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.gui.DownloadScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ModScreenEvent {

    public static final String IMC_METHOD = "DownloadScreen";

    @Nullable
    private static Screen receivedScreen;

    private ModScreenEvent() {
    }

    public static void setReceivedScreen(@Nullable Screen screen) {
        receivedScreen = screen;
    }

    public static void openScreen(PlayerModelScreen modelScreen) {
        Screen screen = receivedScreen != null ? receivedScreen : new DownloadScreen(modelScreen);
        Minecraft.getInstance().setScreen(screen);
    }
}
