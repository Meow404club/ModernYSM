package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import rip.ysm.util.Rl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。口径见 ModelInfoCapabilityProvider 孪生头注。
 */
public class AuthModelsCapabilityProvider {

    public static final EntityCapability<AuthModelsCapability, Void> AUTH_MODELS_CAP =
            EntityCapability.createVoid(Rl.of(YesSteveModel.MOD_ID, "own_models"), AuthModelsCapability.class);

    private static final Map<Entity, AuthModelsCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static AuthModelsCapabilityProvider forEntity(Player player) {
        return INSTANCES.computeIfAbsent(player, p -> new AuthModelsCapabilityProvider());
    }

    private AuthModelsCapability capability = null;

    public AuthModelsCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new AuthModelsCapability();
        }
        return this.capability;
    }

    public void deserializeNBT(ListTag listTag) {
        getOrCreateCapability().deserializeNBT(listTag);
    }

    public ListTag serializeNBT() {
        return getOrCreateCapability().serializeNBT();
    }
}
