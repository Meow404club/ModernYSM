package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.util.YsmEntity;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class C2SCompleteFeedbackPacket {
    private final FeedbackData feedbackData;

    public C2SCompleteFeedbackPacket(FeedbackData feedbackData) {
        this.feedbackData = feedbackData;
    }

    public FeedbackData feedbackData() {
        return this.feedbackData;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof C2SCompleteFeedbackPacket)) {
            return false;
        }
        C2SCompleteFeedbackPacket other = (C2SCompleteFeedbackPacket) obj;
        return Objects.equals(this.feedbackData, other.feedbackData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.feedbackData);
    }

    @Override
    public String toString() {
        return "C2SCompleteFeedbackPacket[feedbackData=" + this.feedbackData + "]";
    }

    public static void encode(C2SCompleteFeedbackPacket message, FriendlyByteBuf buf) {
        FeedbackData.writeToBuf(message.feedbackData, buf);
    }

    public static C2SCompleteFeedbackPacket decode(FriendlyByteBuf buf) {
        return new C2SCompleteFeedbackPacket(FeedbackData.readFromBuf(buf, false));
    }

    public static void handle(C2SCompleteFeedbackPacket message, PacketContext ctx) {
        if (ctx.isServerSide() && ctx.getSender() != null) {
            ServerPlayer sender = ctx.getSender();
            //? if <1.17
            /*ctx.enqueueWork(() -> handleOnServer(message, sender.getLevel()));*/
            // ServerPlayer.serverLevel() 1.19.4 起；1.17~1.19.2 getLevel() 强转等价
            //? if >=1.17 && <1.20
            /*ctx.enqueueWork(() -> handleOnServer(message, (net.minecraft.server.level.ServerLevel) sender.getLevel()));*/
            //? if >=1.20 && <21.6
            ctx.enqueueWork(() -> handleOnServer(message, sender.serverLevel()));
            // 1.21.6 ServerPlayer.level() 直返 ServerLevel（ServerPlayer.java:1712，serverLevel 删）
            //? if >=21.6
            /*ctx.enqueueWork(() -> handleOnServer(message, sender.level()));*/
        }
    }

    public static void handleOnServer(C2SCompleteFeedbackPacket message, ServerLevel serverLevel) {
        Entity entity = serverLevel.getEntity(message.feedbackData.flags());
        if (TouhouMaidCompat.isMaidEntity(entity)) {
            TouhouMaidCompat.applyFeedback(entity, message.feedbackData);
        } else if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) entity;
            ModelInfoCapability.get(serverPlayer).ifPresent(cap -> {
                cap.applyFeedback(serverPlayer, message.feedbackData);
                if (serverPlayer.getVehicle() != null && YsmEntity.firstPassenger(serverPlayer.getVehicle()) == serverPlayer) {
                    VehicleModelCapability.get(serverPlayer.getVehicle()).ifPresent(vehicleCap -> {
                        cap.getMolangVars().ifPresent(map -> vehicleCap.setModel(cap.getModelId(), map));
                    });
                }
            });
        }
    }
}
