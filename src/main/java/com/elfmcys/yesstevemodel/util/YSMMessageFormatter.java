package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
//? if <1.17 {
/*import net.minecraftforge.fml.server.ServerLifecycleHooks;
 *///?}
//? if >=1.17 && <1.18.2 {
/*import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
 *///?}
//? if forge && >=1.18.2
import net.minecraftforge.server.ServerLifecycleHooks;
//? if neoforge
/*import net.neoforged.neoforge.server.ServerLifecycleHooks;*/
import rip.ysm.api.PlatformAPI;

public class YSMMessageFormatter {

    private static final String PREFIX = "§6§l【§aYSM§6§l】§r";

    public static Component withPrefix(Component component) {
        return YsmText.literal(PREFIX).append(component);
    }

    public static boolean isCurrentClientPlayer(Entity entity) {
        //? if neoforge
        /*return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getGameProfile().getId());*/
        //? if forge
        return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getUser().getGameProfile().getId());
    }

    public static boolean hasPermission(@Nullable Entity entity, int level) {
        if (entity == null) {
            return false;
        }
        // 1.21.2 Entity.hasPermissions 删除（Player 保留，vanilla-1.21.3 Player.java:2061）
        //? if <1.21.2
        return entity.hasPermissions(level) || isCurrentClientPlayer(entity);
        //? if >=1.21.2
        /*return (entity instanceof net.minecraft.world.entity.player.Player p && p.hasPermissions(level)) || isCurrentClientPlayer(entity);*/
    }

    public static boolean hasCommandPermission(CommandSourceStack commandSourceStack, int level) {
        if (commandSourceStack.hasPermission(level)) {
            return true;
        }
        return commandSourceStack.getEntity() != null && isCurrentClientPlayer(commandSourceStack.getEntity());
    }

    public static void sendServerMessage(@Nullable CommandSourceStack commandSourceStack, Component component, boolean broadcastToOps) {
        // ServerLifecycleHooks 1.17 起在 net.minecraftforge.server（1.16.5 在 net.minecraftforge.fml.server，
        // javap 实证）；import 条件化置于文件头
        //? if <1.17 {
        /*MinecraftServer currentServer = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
         *///?}
        //? if >=1.17 && <1.18.2 {
        /*MinecraftServer currentServer = net.minecraftforge.fmllegacy.server.ServerLifecycleHooks.getCurrentServer();
         *///?}
        //? if forge && >=1.18.2
        MinecraftServer currentServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        //? if neoforge
        /*MinecraftServer currentServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();*/
        if (currentServer == null) {
            return;
        }
        currentServer.execute(() -> {
            ServerPlayer player;
            CommandSourceStack sourceStack = null;
            if (commandSourceStack != null && (commandSourceStack.getEntity() instanceof ServerPlayer) && (player = currentServer.getPlayerList().getPlayer(commandSourceStack.getEntity().getUUID())) != null) {
                sourceStack = player.createCommandSourceStack();
            }
            if (sourceStack == null) {
                sourceStack = currentServer.createCommandSourceStack();
            }
            YsmText.sendSuccess(sourceStack, component, broadcastToOps);
        });
    }
}