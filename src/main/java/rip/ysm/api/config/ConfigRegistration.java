package rip.ysm.api.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 配置注册（Forge 原生）。architectury 织入门面退役（M1 mig-registry-config）：
 * ADR 勘误确认 forge 通道从未引入 forge_config_api_port，GeneralConfig/ServerConfig 本就
 * 直用 ForgeConfigSpec，故 impl（platform/forge/ConfigRegistrationImpl）就地并入门面，
 * 直调 FML {@link ModLoadingContext#registerConfig(ModConfig.Type, net.minecraftforge.fml.config.IConfigSpec)}。
 * <p>
 * modId 参数保留以稳定 YesSteveModel.initConfig 调用点签名；Forge 侧文件名/mod 归属
 * 由构造期 activeContainer 解析（默认 {@code <modid>-<type>.toml}），与原 impl 行为一致。
 */
public final class ConfigRegistration {

    private ConfigRegistration() {
    }

    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }
}
