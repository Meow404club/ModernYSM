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
        // User.getProfileId 1.19.2+；1.16.5~1.18.2 getGameProfile().getId() 等价
        //? if <1.19.2
        /*if (minecraft.getUser() != null && uuid.equals(minecraft.getUser().getGameProfile().getId())) {*/
        //? if >=1.19.2
        if (minecraft.getUser() != null && uuid.equals(minecraft.getUser().getProfileId())) {
            return true;
        }
        LocalPlayer player = minecraft.player;
        return player != null && uuid.equals(player.getUUID());
    }
}
