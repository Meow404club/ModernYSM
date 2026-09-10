package com.elfmcys.yesstevemodel.command;

import com.elfmcys.yesstevemodel.command.subcommands.client.CacheCommand;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;


public class OpenYSMClientCommand {

    public static void registerClientCommands(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.<CommandSourceStack>literal("openysm")
                .requires(source -> true);

        root.then(CacheCommand.register());

        commandDispatcher.register(root);
    }
}
