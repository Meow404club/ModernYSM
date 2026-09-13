package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.gui.components.Checkbox;
import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.network.chat.Component;
//? if neoforge
/*import net.neoforged.neoforge.common.ModConfigSpec;*/
//? if forge
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigCheckBoxForge extends Checkbox {

    //? if neoforge
    /*private final ModConfigSpec.BooleanValue forgeConfigSpec;*/
    //? if forge
    private final ForgeConfigSpec.BooleanValue forgeConfigSpec;

    //? if neoforge
    /*public ConfigCheckBoxForge(int x, int y, String str, ModConfigSpec.BooleanValue booleanValue) {*/
    //? if forge
    public ConfigCheckBoxForge(int x, int y, String str, ForgeConfigSpec.BooleanValue booleanValue) {
        //? if neoforge
        /*super(x, y, YsmText.translatable("gui.yes_steve_model.config." + str), net.minecraft.client.Minecraft.getInstance().font, booleanValue.get().booleanValue(), Checkbox.OnValueChange.NOP);*/
        //? if forge
        super(x, y, 400, 20, YsmText.translatable("gui.yes_steve_model.config." + str), booleanValue.get().booleanValue());
        this.forgeConfigSpec = booleanValue;
    }

    public void onPress() {
        super.onPress();
        this.forgeConfigSpec.set(Boolean.valueOf(!this.forgeConfigSpec.get().booleanValue()));
    }
}