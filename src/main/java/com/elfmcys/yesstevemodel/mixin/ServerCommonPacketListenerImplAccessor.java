package com.elfmcys.yesstevemodel.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 1.20.2 起 connection 字段上移至父类 ServerCommonPacketListenerImpl（public final）；
// neoforge 三线改指父类。注意不可用注解内联交换：1.20.1 为 vcsVersion 直通编译原文，
// 行内双分支两态同现即语法错（d8fcfbd 回归实证）——用整行双分支，注释态对 vcs 无害。
//? if neoforge
/* @Mixin(net.minecraft.server.network.ServerCommonPacketListenerImpl.class) */
//? if forge
@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
public interface ServerCommonPacketListenerImplAccessor {
    @Accessor("connection")
    Connection ysm$getConnection();
}
