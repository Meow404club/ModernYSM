package com.elfmcys.yesstevemodel.capability.platform.forge;

import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.platform.forge.capability.StarModelsCapabilityProvider;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StarModelsCapabilityImpl {

    private StarModelsCapabilityImpl() {
    }

    public static Optional<StarModelsCapability> get(Player player) {
        return player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).resolve();
    }
}
