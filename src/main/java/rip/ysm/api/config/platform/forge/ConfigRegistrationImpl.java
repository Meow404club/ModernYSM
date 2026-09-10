package rip.ysm.api.config.platform.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class ConfigRegistrationImpl {

    private ConfigRegistrationImpl() {
    }

    @SuppressWarnings({"deprecated","removal"})
    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }
}
