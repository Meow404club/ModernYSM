package com.elfmcys.yesstevemodel.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/**
 * 版本中性文本/反馈门面（对位 rip.ysm.gui.YsmGui.text/trans 的非 GUI 侧同款）：
 * <ul>
 *   <li>1.20.1 Component.literal/translatable 工厂（1.19.4+）↔ 1.16.5 new TextComponent/TranslatableComponent；
 *       实据 1.16.5 mojmap jar javap：Component 无静态工厂，TextComponent(String)/TranslatableComponent(String,Object...) 构造器在。</li>
 *   <li>CommandSourceStack.sendSuccess：1.20.1 Supplier&lt;Component&gt; 形参 ↔ 1.16.5 直传 Component
 *       （1.16.5 javap：sendSuccess(Component,boolean)）。</li>
 *   <li>Player.sendSystemMessage（1.19+）↔ 1.16.5 displayClientMessage(msg,false)（两侧语义=聊天栏）。</li>
 * </ul>
 */
public final class YsmText {
    private YsmText() {
    }

    public static MutableComponent literal(String s) {
        // Component.literal/translatable 接口静态工厂 1.19.2 起（1192 Component.java:144/152；
        // 1182 无）→ 1.16.5~1.18.2 用 TextComponent/TranslatableComponent 构造
        //? if <1.19.2 {
        /*return new net.minecraft.network.chat.TextComponent(s);
         *///?} else {
        return Component.literal(s);
        //?}
    }

    public static MutableComponent translatable(String key, Object... args) {
        //? if <1.19.2 {
        /*return new net.minecraft.network.chat.TranslatableComponent(key, args);
         *///?} else {
        return Component.translatable(key, args);
        //?}
    }

    public static void sendSuccess(CommandSourceStack source, Component msg, boolean broadcastToOps) {
        // sendSuccess(Supplier) 1.19.4 起；1.16.5~1.19.2 为 (Component, boolean)（1192 同 1165 形）
        //? if <1.17 {
        /*source.sendSuccess(msg, broadcastToOps);
         *///?}
        //? if >=1.17 && <1.19.4 {
        /*source.sendSuccess(msg, broadcastToOps);
         *///?}
        //? if >=1.20 {
        source.sendSuccess(() -> msg, broadcastToOps);
        //?}
    }

    public static void sendSystemMessage(Player player, Component msg) {
        // Player.sendSystemMessage 1.19+；1.16.5~1.18.2 displayClientMessage(msg,false)（聊天栏同语义）
        //? if <1.19.2 {
        /*player.displayClientMessage(msg, false);
         *///?} else {
        player.sendSystemMessage(msg);
        //?}
    }

    /** 1.20.1 CommandSourceStack.isPlayer()（1.16.5 无）↔ getEntity() instanceof ServerPlayer（两版同语义）。 */
    public static boolean isPlayer(CommandSourceStack source) {
        // CommandSourceStack.isPlayer 1.19.2+（1182 无）
        //? if <1.19.2 {
        /*return source.getEntity() instanceof net.minecraft.server.level.ServerPlayer;
         *///?} else {
        return source.isPlayer();
        //?}
    }

    /** 1.20.1 CommandSourceStack.sendSystemMessage(Component)（1.16.5 无）↔ sendSuccess(msg,false)（仅回源，不广播）。 */
    public static void sendSourceMessage(CommandSourceStack source, Component msg) {
        //? if <1.19.2 {
        /*source.sendSuccess(msg, false);
         *///?} else {
        source.sendSystemMessage(msg);
        //?}
    }
}
