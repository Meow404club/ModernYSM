package com.elfmcys.yesstevemodel.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
final class ClientOnlyHostBridge {

    private ClientOnlyHostBridge() {
    }

    static boolean isActive() {
        return ClientOnlyMode.isActive();
    }

    static boolean isLocalHost(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getUser() != null && uuid.equals(minecraft.getUser().getProfileId())) {
            return true;
        }
        LocalPlayer player = minecraft.player;
        return player != null && uuid.equals(player.getUUID());
    }
}
