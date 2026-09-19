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
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.HashSet;
import java.util.Set;

public class S2CSyncStarModelsPacket {

    private final Set<String> starModels;

    public S2CSyncStarModelsPacket(Set<String> starModels) {
        this.starModels = starModels;
    }

    public static void encode(S2CSyncStarModelsPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.starModels.size());
        for (String starModel : message.starModels) {
            buf.writeUtf(starModel);
        }
    }

    public static S2CSyncStarModelsPacket decode(FriendlyByteBuf buf) {
        int varInt = buf.readVarInt();
        HashSet<String> tmp = Sets.newHashSet();
        for (int i = 0; i < varInt; i++) {
            tmp.add(buf.readUtf(32767));
        }
        return new S2CSyncStarModelsPacket(tmp);
    }

    public static void handle(S2CSyncStarModelsPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> handleCapability(message));
        }
    }

    // server-dist 缺口修复：同 S2CSyncAuthModelsPacket——payload 注册在 dedicated server 加载
    // 本类，原体 LocalPlayer→Player 收窄入参在类校验期解析掩码类 → CNFE；>=21.7 主体隔离至
    // client.ClientModelManager（委托签名全公共类型），<21.7 strip era 与 forge 维持原体
    @OnlyIn(Dist.CLIENT)
    public static void handleCapability(S2CSyncStarModelsPacket message) {
//? if neoforge && >=21.7 {
/*        ClientModelManager.handleStarModelsSync(message.starModels);*/
//?} else {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            StarModelsCapability.get(minecraft.player).ifPresent(cap -> cap.setStarModels(message.starModels));
        }
//?}
    }
}