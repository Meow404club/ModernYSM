package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.api.network.PacketContext;

import java.util.Map;

public class C2SPlayAnimationPacket {

    private final int animationIndex;

    private final String category;

    private final int entityId;

    public C2SPlayAnimationPacket(int animationIndex, String category, int entityId) {
        this.animationIndex = animationIndex;
        this.category = category;
        this.entityId = entityId;
    }

    public C2SPlayAnimationPacket(int animationIndex, String category) {
        this(animationIndex, category, -1);
    }

    public static C2SPlayAnimationPacket createDefault() {
        return new C2SPlayAnimationPacket(-1, StringPool.EMPTY);
    }

    public static C2SPlayAnimationPacket createWithIndex(int entityId) {
        return new C2SPlayAnimationPacket(-1, StringPool.EMPTY, entityId);
    }

    public static void encode(C2SPlayAnimationPacket message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.animationIndex);
        buf.writeUtf(message.category);
        buf.writeVarInt(message.entityId);
    }

    public static C2SPlayAnimationPacket decode(FriendlyByteBuf buf) {
        return new C2SPlayAnimationPacket(buf.readVarInt(), buf.readUtf(), buf.readVarInt());
    }

    public static void handle(C2SPlayAnimationPacket message, PacketContext ctx) {
        if (ctx.isServerSide()) {
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) {
                    return;
                }
                handleCapability(message, sender);
            });
        }
    }

    private static void handleCapability(C2SPlayAnimationPacket message, ServerPlayer sender) {
        if (message.entityId != -1) {
            //? if <1.17
            /*Entity entity = sender.getLevel().getEntity(message.entityId);*/
            //? if >=1.17 && <1.20
            /*Entity entity = ((net.minecraft.server.level.ServerLevel) sender.getLevel()).getEntity(message.entityId);*/
            //? if >=1.20 && <21.8
            Entity entity = sender.serverLevel().getEntity(message.entityId);
            // 1.21.8 ServerPlayer.level() 直返 ServerLevel（ServerPlayer.java:1714，serverLevel 删）
            //? if >=21.8
            /*Entity entity = sender.level().getEntity(message.entityId);*/
            if (TouhouMaidCompat.isMaidEntity(entity)) {
                TouhouMaidCompat.registerAnimationRoulette(entity, message.category, message.animationIndex);
                return;
            }
            return;
        }

        ModelInfoCapability.get(sender).ifPresent(modelInfoCap -> {
            if (message.animationIndex == -1) {
                modelInfoCap.stopAnimation(sender);
            } else {
                ServerModelManager.getModelDefinition(modelInfoCap.getModelId()).ifPresent(serverModelCap -> {
                    OrderedStringMap<String, String> extraAnimations;
                    ModelProperties modelProperties = serverModelCap.getLoadedModelData().getModelProperties();
                    Map<String, OrderedStringMap<String, String>> extraAnimationClassify = modelProperties.getExtraAnimationClassify();
                    if (StringUtils.isNotBlank(message.category) && extraAnimationClassify.containsKey(message.category)) {
                        extraAnimations = extraAnimationClassify.get(message.category);
                    } else {
                        extraAnimations = modelProperties.getExtraAnimation();
                    }
                    if (extraAnimations.size() > message.animationIndex) {
                        modelInfoCap.playAnimation(sender, extraAnimations.getKeyAt(message.animationIndex));
                    }
                });
            }
        });
    }
}