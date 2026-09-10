package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.command.OpenYSMClientCommand;
import com.elfmcys.yesstevemodel.command.RootClientCommand;
import com.elfmcys.yesstevemodel.command.RootCommand;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import rip.ysm.api.PlatformAPI;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandRegistry {

    private CommandRegistry() {
    }

    public static final SuggestionProvider<CommandSourceStack> MODEL_IDS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "models"), (commandContext, suggestionsBuilder) -> {
        if (commandContext.getSource() instanceof SharedSuggestionProvider) {
            if (PlatformAPI.isServer()) {
                List<String> serverModelIds = ServerModelManager.getServerModelInfo().keySet().stream().map(CommandRegistry::escapeIfRequired).collect(Collectors.toCollection(ArrayList::new));
                return SharedSuggestionProvider.suggest(serverModelIds, suggestionsBuilder);
            }
            List<String> clientModelIds = ClientModelManager.getModelAssemblyMap().keySet().stream().map(CommandRegistry::escapeIfRequired).collect(Collectors.toCollection(ArrayList::new));
            return SharedSuggestionProvider.suggest(clientModelIds, suggestionsBuilder);
        }
        return Suggestions.empty();
    });

    public static final SuggestionProvider<CommandSourceStack> ANIMATION_NAMES = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "animations"), (commandContext, suggestionsBuilder) -> {
        if (commandContext.getSource() instanceof SharedSuggestionProvider) {
            if (PlatformAPI.isServer()) {
                return Suggestions.empty();
            }
            Object2ReferenceMap<String, Animation> map = ClientModelManager.getLocalModelContext().getAnimationBundle().getMainAnimations();
            HashSet<String> set = Sets.newHashSet();
            set.addAll(map.keySet().stream().map(CommandRegistry::escapeIfRequired).collect(Collectors.toCollection(ArrayList::new)));
            set.add("stop");
            return SharedSuggestionProvider.suggest(set, suggestionsBuilder);
        }
        return Suggestions.empty();
    });

    public static final SuggestionProvider<CommandSourceStack> TEXTURE_IDS = SuggestionProviders.register(new ResourceLocation(YesSteveModel.MOD_ID, "textures"), (commandContext, suggestionsBuilder) -> {
        if (commandContext.getSource() instanceof SharedSuggestionProvider) {
            String str = commandContext.getArgument("model_id", String.class);
            if (PlatformAPI.isServer()) {
                if (ServerModelManager.getServerModelInfo().containsKey(str)) {
                    List<String> list = ServerModelManager.getServerModelInfo().get(str).getModelInfo().getTextures().stream().map(CommandRegistry::escapeIfRequired).collect(Collectors.toCollection(ArrayList::new));
                    list.add(0, "-");
                    return SharedSuggestionProvider.suggest(list, suggestionsBuilder);
                }
            } else if (ClientModelManager.getModelAssemblyMap().containsKey(str)) {
                List<String> list2 = ClientModelManager.getModelContext(str).map(context -> context.getAnimationBundle().getTextures().getKeys().stream().map(CommandRegistry::escapeIfRequired).collect(Collectors.toCollection(ArrayList::new))).orElseGet(Lists::newArrayList);
                list2.add(0, "-");
                return SharedSuggestionProvider.suggest(list2, suggestionsBuilder);
            }
        }
        return Suggestions.empty();
    });

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            OpenYSMClientCommand.registerClientCommands(event.getDispatcher());
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            var dispatcher = event.getDispatcher();
            if (!YesSteveModel.isAvailable()) {
                RootCommand.registerFallbackCommands(dispatcher);
                return;
            }
            RootCommand.registerCommands(dispatcher);
            if (!PlatformAPI.isServer()) {
                RootClientCommand.registerClientCommands(dispatcher);
            }
        });
    }

    private static String escapeIfRequired(String str) {
        if (str.chars().allMatch(i -> StringReader.isAllowedInUnquotedString((char) i))) {
            return str;
        }
        return String.format("\"%s\"", str.replace("\"", "\\\"").replace("'", "\\'"));
    }
}