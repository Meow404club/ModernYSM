package com.elfmcys.yesstevemodel.config;

//? if neoforge
/*import net.neoforged.neoforge.common.ModConfigSpec;*/
//? if forge
import net.minecraftforge.common.ForgeConfigSpec;

public class GeneralConfig {

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISCLAIMER_SHOW;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISCLAIMER_SHOW;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue PRINT_ANIMATION_ROULETTE_MSG;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue PRINT_ANIMATION_ROULETTE_MSG;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_SELF_MODEL;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_MODEL;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_OTHER_MODEL;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_OTHER_MODEL;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_SELF_HANDS;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_HANDS;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_PROJECTILE_MODEL;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_PROJECTILE_MODEL;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_VEHICLE_MODEL;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_VEHICLE_MODEL;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_EXTERNAL_FP_ANIM;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_EXTERNAL_FP_ANIM;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue USE_COMPATIBILITY_RENDERER;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue USE_COMPATIBILITY_RENDERER;

    //? if neoforge
    /*public static ModConfigSpec.DoubleValue SOUND_VOLUME;*/
    //? if forge
    public static ForgeConfigSpec.DoubleValue SOUND_VOLUME;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue SHOW_MODEL_ID_FIRST;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue SHOW_MODEL_ID_FIRST;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue SOPHISTICATEDBACKPACK;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue SOPHISTICATEDBACKPACK;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue PARCOOL;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue PARCOOL;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue USE_GPU_RENDERER;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue USE_GPU_RENDERER;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue USE_GPU_IRIS_DIRECT;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue USE_GPU_IRIS_DIRECT;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue LAZY_MODEL_LOADING;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue LAZY_MODEL_LOADING;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue FORCE_CLIENT_MODE;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue FORCE_CLIENT_MODE;

    //? if neoforge
    /*public static ModConfigSpec.DoubleValue HANDSHAKE_TIMEOUT;*/
    //? if forge
    public static ForgeConfigSpec.DoubleValue HANDSHAKE_TIMEOUT;

    //? if neoforge
    /*public static ModConfigSpec.DoubleValue SEARCH_SUGGESTION_COUNT;*/
    //? if forge
    public static ForgeConfigSpec.DoubleValue SEARCH_SUGGESTION_COUNT;

    //? if neoforge
    /*public static ModConfigSpec.EnumValue<RouletteSettingsMode> ROULETTE_SETTINGS_MODE;*/
    //? if forge
    public static ForgeConfigSpec.EnumValue<RouletteSettingsMode> ROULETTE_SETTINGS_MODE;

    //? if neoforge
    /*public static ModConfigSpec.EnumValue<RouletteMode> ROULETTE_MODE;*/
    //? if forge
    public static ForgeConfigSpec.EnumValue<RouletteMode> ROULETTE_MODE;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue BLUR_GUI;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue BLUR_GUI;

    //? if neoforge
    /*public static ModConfigSpec.EnumValue<TextureScreenMode> TEXTURE_SCREEN_MODE;*/
    //? if forge
    public static ForgeConfigSpec.EnumValue<TextureScreenMode> TEXTURE_SCREEN_MODE;

    //? if neoforge
    /*public static ModConfigSpec.EnumValue<ModelInfoScreenMode> MODEL_INFO_SCREEN_MODE;*/
    //? if forge
    public static ForgeConfigSpec.EnumValue<ModelInfoScreenMode> MODEL_INFO_SCREEN_MODE;

    public enum RouletteSettingsMode {
        MODERN,
        CLASSIC
    }

    public enum RouletteMode {
        MODERN,
        CLASSIC
    }

    public enum TextureScreenMode {
        MODERN,
        CLASSIC
    }

    public enum ModelInfoScreenMode {
        MODERN,
        CLASSIC
    }

    public static boolean effectiveModernRoulette() {
        if (ROULETTE_MODE == null || ROULETTE_SETTINGS_MODE == null) return false;
        return ROULETTE_MODE.get() == RouletteMode.MODERN && ROULETTE_SETTINGS_MODE.get() == RouletteSettingsMode.MODERN;
    }

    //? if neoforge
    /*public static ModConfigSpec buildSpec() {*/
    //? if forge
    public static ForgeConfigSpec buildSpec() {
        //? if neoforge
        /*ModConfigSpec.Builder builder = new ModConfigSpec.Builder();*/
        //? if forge
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        defineGeneral(builder);
        ExtraPlayerRenderConfig.define(builder);
        LoadingStateConfig.define(builder);
        return builder.build();
    }

    //? if neoforge
    /*public static void defineGeneral(ModConfigSpec.Builder builder) {*/
    //? if forge
    public static void defineGeneral(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        builder.comment("Whether to display disclaimer GUI");
        DISCLAIMER_SHOW = builder.define("DisclaimerShow", true);
        builder.comment("Whether to print animation roulette play message");
        PRINT_ANIMATION_ROULETTE_MSG = builder.define("PrintAnimationRouletteMsg", false);
        builder.comment("Prevents rendering of self player's model");
        DISABLE_SELF_MODEL = builder.define("DisableSelfModel", false);
        builder.comment("Prevents rendering of other player's model");
        DISABLE_OTHER_MODEL = builder.define("DisableOtherModel", false);
        builder.comment("Prevents rendering of self player's hand");
        DISABLE_SELF_HANDS = builder.define("DisableSelfHands", false);
        builder.comment("Prevents rendering of projectile model");
        DISABLE_PROJECTILE_MODEL = builder.define("DisableProjectileModel", false);
        builder.comment("Prevents rendering of vehicle model");
        DISABLE_VEHICLE_MODEL = builder.define("DisableVehicleModel", false);
        builder.comment("Disable first person animation from other mods.");
        DISABLE_EXTERNAL_FP_ANIM = builder.define("DisableExternalFirstPersonAnim", false);
        builder.comment("If rendering errors occur, try turning on this.");
        USE_COMPATIBILITY_RENDERER = builder.define("UseCompatibilityRenderer", false);
        builder.comment("Test renderer.");
        USE_GPU_RENDERER = builder.define("UseGpuRenderer", true);
        builder.comment("EXPERIMENTAL: draw YSM model directly through the Iris/Oculus shader pipeline (52B extended vertex layout). Leave OFF unless asked to test a fix; default off = identical behavior.");
        USE_GPU_IRIS_DIRECT = builder.define("UseGpuIrisDirectRenderer", false);
        LAZY_MODEL_LOADING = builder.define("LazyModelLoading", true);
        builder.comment("Always use client-only mode");
        FORCE_CLIENT_MODE = builder.define("ForceClientMode", false);
        builder.comment("Seconds to wait for the server to answer the handshake.");
        HANDSHAKE_TIMEOUT = builder.defineInRange("HandshakeTimeout", 5.0d, 1.0d, 60.0d);
        builder.comment("Maximum entries shown at search list.");
        SEARCH_SUGGESTION_COUNT = builder.defineInRange("SearchSuggestionCount", 8.0d, 1.0d, 30.0d);
        ROULETTE_SETTINGS_MODE = builder.defineEnum("RouletteSettingsMode", RouletteSettingsMode.MODERN);
        ROULETTE_MODE = builder.defineEnum("RouletteMode", RouletteMode.CLASSIC);
        BLUR_GUI = builder.define("BlurGui", true);
        TEXTURE_SCREEN_MODE = builder.defineEnum("TextureScreenMode", TextureScreenMode.MODERN);
        MODEL_INFO_SCREEN_MODE = builder.defineEnum("ModelInfoScreenMode", ModelInfoScreenMode.MODERN);
        builder.comment("The amount of volume when the animation is played.");
        SOUND_VOLUME = builder.defineInRange("SoundVolume", 100.0d, 0.0d, 100.0d);
        builder.comment("Whether to display model ID first in the model selection screen, instead of the model name filled in by the model author.");
        SHOW_MODEL_ID_FIRST = builder.define("ShowModelIdFirst", false);
        builder.pop();
        builder.push("Integration");
        SOPHISTICATEDBACKPACK = builder.define("SophisticatedBackpack", true);
        PARCOOL = builder.define("Parcool", true);
        builder.pop();
    }
}
