package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;

public class ItemTagsConstants {

    public static final YsmTag.ItemTag AXES = createTag("axes");

    public static final YsmTag.ItemTag HOES = createTag("hoes");

    public static final YsmTag.ItemTag PICKAXES = createTag("pickaxes");

    public static final YsmTag.ItemTag SHOVELS = createTag("shovels");

    public static final YsmTag.ItemTag SWORDS = createTag("swords");

    public static final YsmTag.ItemTag THROWABLE_POTION = createTag("throwable_potion");

    public static final YsmTag.ItemTag BOWS = createTag("bows");

    public static final YsmTag.ItemTag CROSSBOWS = createTag("crossbows");

    public static final YsmTag.ItemTag FISHING_RODS = createTag("fishing_rods");

    public static final YsmTag.ItemTag SHIELDS = createTag("shields");

    public static final YsmTag.ItemTag TRIDENTS = createTag("tridents");

    public static final YsmTag.ItemTag SLASHBLADE = createTag("slashblade");

    private static YsmTag.ItemTag createTag(String str) {
        return YsmTag.itemTag(new ResourceLocation(YesSteveModel.MOD_ID, str));
    }
}