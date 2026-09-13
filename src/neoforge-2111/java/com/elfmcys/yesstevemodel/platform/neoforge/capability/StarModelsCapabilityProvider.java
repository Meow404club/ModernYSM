package com.elfmcys.yesstevemodel.platform.neoforge.capability;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
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
public class StarModelsCapabilityProvider {

    public static final EntityCapability<StarModelsCapability, Void> STAR_MODELS_CAP =
            EntityCapability.createVoid(Rl.of(YesSteveModel.MOD_ID, "star_models"), StarModelsCapability.class);

    private static final Map<Entity, StarModelsCapabilityProvider> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static StarModelsCapabilityProvider forEntity(Player player) {
        return INSTANCES.computeIfAbsent(player, p -> new StarModelsCapabilityProvider());
    }

    private StarModelsCapability capability = null;

    public StarModelsCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new StarModelsCapability();
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
