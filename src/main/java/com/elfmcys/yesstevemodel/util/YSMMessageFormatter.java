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
        // 1.21.10 authlib 7 GameProfile 记录化：getId() → id()（2110 Minecraft.java:796 同代实证）
        //? if neoforge && >=21.10
        /*return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getGameProfile().id());*/
        //? if neoforge && <21.10
        /*return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getGameProfile().getId());*/
        //? if forge
        return entity != null && !PlatformAPI.isServer() && entity.getUUID().equals(Minecraft.getInstance().getUser().getGameProfile().getId());
    }

    public static boolean hasPermission(@Nullable Entity entity, int level) {
        if (entity == null) {
            return false;
        }
        // 1.21.2 Entity.hasPermissions 删除（Player 保留，vanilla-1.21.3 Player.java:2061）；
        // 1.21.11 Player.hasPermissions 也删（2111 Player.java 零命中）→ 客户端本地玩家兜底放行
        //? if <1.21.2
        return entity.hasPermissions(level) || isCurrentClientPlayer(entity);
        //? if >=1.21.2 && <21.11
        /*return (entity instanceof net.minecraft.world.entity.player.Player p && p.hasPermissions(level)) || isCurrentClientPlayer(entity);*/
        //? if >=21.11
        /*return isCurrentClientPlayer(entity);*/
    }

    public static boolean hasCommandPermission(CommandSourceStack commandSourceStack, int level) {
        // 1.21.11 CommandSourceStack 权限改 PermissionSet 模型（2111 CommandSourceStack.java:378
        // permissions() + PermissionSet.hasPermission(Permission) 实证）；level 2 ≙ GAMEMASTERS 档
        //? if >=21.11 {
        /*if (commandSourceStack.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        return commandSourceStack.getEntity() != null && isCurrentClientPlayer(commandSourceStack.getEntity());
        *///?}
        //? if <21.11 {
        if (commandSourceStack.hasPermission(level)) {
            return true;
        }
        return commandSourceStack.getEntity() != null && isCurrentClientPlayer(commandSourceStack.getEntity());
        //?}
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