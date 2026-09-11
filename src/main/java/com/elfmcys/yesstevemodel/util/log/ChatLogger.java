package com.elfmcys.yesstevemodel.util.log;

import com.elfmcys.yesstevemodel.util.YsmText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatLogger implements ILogger {

    public static final ChatLogger INSTANCE = new ChatLogger();

    private ChatLogger() {
    }

    @Override
    public void logFormatted(String str, Object... objArr) {
        logComponent(YsmText.literal(String.format(str, objArr)));
    }

    @Override
    public void logComponent(Component component) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            YsmText.sendSystemMessage(Minecraft.getInstance().player, YsmText.translatable("message.yes_steve_model.model.debug_animation.output").append(component));
        });
    }
}