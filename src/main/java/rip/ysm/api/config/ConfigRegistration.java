package rip.ysm.api.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import rip.ysm.api.config.platform.forge.ConfigRegistrationImpl;

public final class ConfigRegistration {

    private ConfigRegistration() {
    }

    /**
     * M0 无织入降级：YesSteveModel.initConfig 构造期调用，stub 体按 ADR 替换
     * 矩阵①直调平移进树的 forge 实现（多版本条件化留给 M1 mig-* 卡）。
     */
    @ExpectPlatform
    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        ConfigRegistrationImpl.register(modId, type, spec);
    }
}
