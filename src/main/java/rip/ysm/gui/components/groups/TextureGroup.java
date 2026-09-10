package rip.ysm.gui.components.groups;

import rip.ysm.gui.YsmGui;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class TextureGroup extends CategoryGroup {
    public TextureGroup() {
        super("_textures");
    }

    @Override
    public Component getTitle() {
        String key = "gui.yes_steve_model.animation.category._textures";
        if (I18n.exists(key)) return YsmGui.trans(key);
        return YsmGui.text("Textures");
    }
}
