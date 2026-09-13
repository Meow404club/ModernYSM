package rip.ysm.api.config;

//? if neoforge
/*import net.neoforged.neoforge.common.ModConfigSpec;*/
//? if forge
import net.minecraftforge.common.ForgeConfigSpec;
//? if neoforge
/*import net.neoforged.fml.ModLoadingContext;*/
//? if forge
import net.minecraftforge.fml.ModLoadingContext;
//? if neoforge
/*import net.neoforged.fml.config.ModConfig;*/
//? if forge
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

    // 1.21 ModLoadingContext 删除（docs version-1.20.6 config.md:105 起改名
    // "registered in the mod constructor via ModContainer#registerConfig"）→ >=1.21 委托主类孪生
    // 构造期捕获的 ModContainer；1.20.4/1.20.6 ModLoadingContext 仍在（deprecated）
    //? if neoforge && >=1.21 {
    /*public static void register(String modId, ModConfig.Type type, ModConfigSpec spec) {
        com.elfmcys.yesstevemodel.platform.neoforge.YesSteveModelForge.modContainer().registerConfig(type, spec);
    }*/
    //?}
    //? if neoforge && <1.21 {
    /*public static void register(String modId, ModConfig.Type type, ModConfigSpec spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }*/
    //?}
    //? if forge {
    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        ModLoadingContext.get().registerConfig(type, spec);
    }
    //? }
}
