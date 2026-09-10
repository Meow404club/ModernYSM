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
        //? if <1.17 {
        /*return new net.minecraft.network.chat.TextComponent(s);
         *///?} else {
        return Component.literal(s);
        //?}
    }

    public static MutableComponent translatable(String key, Object... args) {
        //? if <1.17 {
        /*return new net.minecraft.network.chat.TranslatableComponent(key, args);
         *///?} else {
        return Component.translatable(key, args);
        //?}
    }

    public static void sendSuccess(CommandSourceStack source, Component msg, boolean broadcastToOps) {
        //? if <1.17 {
        /*source.sendSuccess(msg, broadcastToOps);
         *///?} else {
        source.sendSuccess(() -> msg, broadcastToOps);
        //?}
    }

    public static void sendSystemMessage(Player player, Component msg) {
        //? if <1.17 {
        /*player.displayClientMessage(msg, false);
         *///?} else {
        player.sendSystemMessage(msg);
        //?}
    }
}
