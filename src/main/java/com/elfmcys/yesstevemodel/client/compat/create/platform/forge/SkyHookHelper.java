package com.elfmcys.yesstevemodel.client.compat.create.platform.forge;

import com.elfmcys.yesstevemodel.platform.forge.mixin.client.create.PlayerSkyhookRendererAccessor;
import net.minecraft.world.entity.player.Player;

public class SkyHookHelper {
    public static boolean isPlayerOnSkyHook(Player player) {
        return PlayerSkyhookRendererAccessor.hangingPlayers().contains(player.getUUID());
    }
}