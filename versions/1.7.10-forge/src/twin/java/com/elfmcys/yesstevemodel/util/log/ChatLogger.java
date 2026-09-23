package com.elfmcys.yesstevemodel.util.log;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/**
 * 1.7.10 原生孪生 ChatLogger（legacy1710-l2a-model-load）。
 *
 * 共享版绑 net.minecraft.network.chat.Component（1.19+ 文本换代），1.12.2 是
 * ITextComponent/TextComponentString，1.7.10 是 IChatComponent/ChatComponentText
 *（vanilla-mc-1710 IChatComponent.java:16 getUnformattedText /
 * EntityPlayer.java:1203 addChatComponentMessage 实证）。消费面仅 INSTANCE/
 * logFormatted/logComponent（MolangParser 等打点）。
 */
public class ChatLogger {
    public static final ChatLogger INSTANCE = new ChatLogger();

    private ChatLogger() {
    }

    public void logFormatted(String str, Object... objArr) {
        logComponent(new ChatComponentText(String.format(str, objArr)));
    }

    public void logComponent(IChatComponent component) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != null) {
            minecraft.thePlayer.addChatComponentMessage(component);
        } else {
            System.out.println("[YSM] " + component.getUnformattedText());
        }
    }
}
