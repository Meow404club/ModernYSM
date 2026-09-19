package com.elfmcys.yesstevemodel.platform.neoforge1202.capability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
// 1.16.5 无 CapabilityManager.get(CapabilityToken)（1.17+ 才有），改 @CapabilityInject 注入
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerCapabilityProvider implements ICapabilityProvider {

    public static Capability<PlayerCapability> PLAYER_CAP = CapabilityManager.get(new CapabilityToken<PlayerCapability>() {
    });

    private PlayerCapability capability;

    private AbstractClientPlayer player;

    public PlayerCapabilityProvider(AbstractClientPlayer abstractClientPlayer) {
        this.player = abstractClientPlayer;
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return getCapability(capability);
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability) {
        return PLAYER_CAP.orEmpty(capability, LazyOptional.of(this::getOrCreateCapability));
    }

    @NotNull
    private PlayerCapability getOrCreateCapability() {
        if (this.capability == null) {
            this.capability = new PlayerCapability(this.player);
            this.player = null;
        }
        return this.capability;
    }
}
