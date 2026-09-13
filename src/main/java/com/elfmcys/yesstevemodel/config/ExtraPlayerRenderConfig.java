package com.elfmcys.yesstevemodel.config;

//? if neoforge
/*import net.neoforged.neoforge.common.ModConfigSpec;*/
//? if forge
import net.minecraftforge.common.ForgeConfigSpec;

public class ExtraPlayerRenderConfig {

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_PLAYER_RENDER;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_PLAYER_RENDER;

    //? if neoforge
    /*public static ModConfigSpec.BooleanValue DISABLE_PLAYER_RENDER_THIRD_PERSON;*/
    //? if forge
    public static ForgeConfigSpec.BooleanValue DISABLE_PLAYER_RENDER_THIRD_PERSON;

    //? if neoforge
    /*public static ModConfigSpec.IntValue PLAYER_POS_X;*/
    //? if forge
    public static ForgeConfigSpec.IntValue PLAYER_POS_X;

    //? if neoforge
    /*public static ModConfigSpec.IntValue PLAYER_POS_Y;*/
    //? if forge
    public static ForgeConfigSpec.IntValue PLAYER_POS_Y;

    //? if neoforge
    /*public static ModConfigSpec.DoubleValue PLAYER_SCALE;*/
    //? if forge
    public static ForgeConfigSpec.DoubleValue PLAYER_SCALE;

    //? if neoforge
    /*public static ModConfigSpec.DoubleValue PLAYER_YAW_OFFSET;*/
    //? if forge
    public static ForgeConfigSpec.DoubleValue PLAYER_YAW_OFFSET;

    //? if neoforge
    /*public static void define(ModConfigSpec.Builder builder) {*/
    //? if forge
    public static void define(ForgeConfigSpec.Builder builder) {
        builder.push("extra_player_render");
        builder.comment("Whether to display player");
        DISABLE_PLAYER_RENDER = builder.define("DisablePlayerRender", false);
        builder.comment("Hide the HUD model preview while in third person view");
        DISABLE_PLAYER_RENDER_THIRD_PERSON = builder.define("DisablePlayerRenderThirdPerson", false);
        builder.comment("Player position x in screen");
        PLAYER_POS_X = builder.defineInRange("PlayerPosX", 10, 0, Integer.MAX_VALUE);
        builder.comment("Player position y in screen");
        PLAYER_POS_Y = builder.defineInRange("PlayerPosY", 10, 0, Integer.MAX_VALUE);
        builder.comment("Player scale in screen");
        PLAYER_SCALE = builder.defineInRange("PlayerScale", 40.0d, 8.0d, 360.0d);
        builder.comment("Player yaw offset in screen");
        PLAYER_YAW_OFFSET = builder.defineInRange("PlayerYawOffset", 5.0d, Double.MIN_VALUE, Double.MAX_VALUE);
        builder.pop();
    }
}