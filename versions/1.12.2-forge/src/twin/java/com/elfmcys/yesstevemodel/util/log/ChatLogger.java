package com.elfmcys.yesstevemodel.util.log;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 1.12.2 原生孪生 ChatLogger（legacy-1222-l1-render）。
 *
 * 共享版绑 net.minecraft.network.chat.Component（1.19+ 文本换代），1.12.2 是
 * ITextComponent/TextComponentString（vanilla-mc-1.12.2 实证）。消费面仅 INSTANCE/
 * logFormatted/logComponent（MolangParser、NativeModelRenderer 打点）。
 */
public class ChatLogger {
    public static final ChatLogger INSTANCE = new ChatLogger();

    private ChatLogger() {
    }

    public void logFormatted(String str, Object... objArr) {
        logComponent(new TextComponentString(String.format(str, objArr)));
    }

    public void logComponent(ITextComponent component) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player != null) {
            minecraft.player.sendMessage(component);
        } else {
            System.out.println("[YSM] " + component.getUnformattedText());
        }
    }
}
