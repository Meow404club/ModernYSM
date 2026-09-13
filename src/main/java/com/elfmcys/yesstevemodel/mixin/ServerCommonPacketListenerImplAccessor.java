package com.elfmcys.yesstevemodel.mixin;

import net.minecraft.network.Connection;
//? if neoforge
/*import net.minecraft.server.network.ServerCommonPacketListenerImpl;*/
//? if forge
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 1.20.2 起 connection 字段上移至父类 ServerCommonPacketListenerImpl（public final）；
// neoforge 三线改指父类。注解内联交换 raw 安全（1.20.1 展开态=单参数+注释不可见）。
@Mixin(/*? if neoforge {*/ ServerCommonPacketListenerImpl.class /*?} else {*/ ServerGamePacketListenerImpl.class /*?}*/)
public interface ServerCommonPacketListenerImplAccessor {
    @Accessor("connection")
    Connection ysm$getConnection();
}
