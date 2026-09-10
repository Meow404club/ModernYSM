package com.elfmcys.yesstevemodel.capability.platform.forge;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.platform.forge.capability.ModelInfoCapabilityProvider;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ModelInfoCapabilityImpl {

    private ModelInfoCapabilityImpl() {
    }

    public static Optional<ModelInfoCapability> get(Player player) {
        return player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).resolve();
    }
}
