package com.elfmcys.yesstevemodel.network.message;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
//? if neoforge && >=21.7 {
/*import com.elfmcys.yesstevemodel.client.ClientModelManager;*/
//?}
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.HashSet;
import java.util.Set;

public class S2CSyncAuthModelsPacket {

    private final Set<String> authModels;

    public S2CSyncAuthModelsPacket(Set<String> authModels) {
        this.authModels = authModels;
    }

    public static void encode(S2CSyncAuthModelsPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.authModels.size());
        for (String modelId : message.authModels) {
            buf.writeUtf(modelId);
        }
    }

    public static S2CSyncAuthModelsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        HashSet<String> tmp = Sets.newHashSet();
        for (int i = 0; i < size; i++) {
            tmp.add(buf.readUtf());
        }
        return new S2CSyncAuthModelsPacket(tmp);
    }

    public static void handle(S2CSyncAuthModelsPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> handleCapability(message));
        }
    }


    // server-dist 缺口修复：本类随 payload 注册在 dedicated server 加载（mask era 类链接期
    // 整类校验），原方法体 minecraft.player(LocalPlayer) 收窄传入 AuthModelsCapability.get(Player)
    // 形参 → 校验器解析 LocalPlayer 层级 → NeoForgeDevDistCleaner 掩码类 CNFE（21.9 runServer
    // 实证 register(6) 行）。>=21.7 主体隔离至 client.ClientModelManager（仅 client 加载，
    // 委托签名全公共类型，本类校验零 client 解析）；<21.7 strip era 与 forge 维持原体
    @OnlyIn(Dist.CLIENT)
    public static void handleCapability(S2CSyncAuthModelsPacket message) {
//? if neoforge && >=21.7 {
/*        ClientModelManager.handleAuthModelsSync(message.authModels);*/
//?} else {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            AuthModelsCapability.get(minecraft.player).ifPresent(cap -> {
                cap.setAuthModels(message.authModels);
            });
        }
//?}
    }
}