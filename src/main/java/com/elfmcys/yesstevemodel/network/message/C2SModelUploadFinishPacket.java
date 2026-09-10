package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class C2SModelUploadFinishPacket {
    private final long uploadId;

    public C2SModelUploadFinishPacket(long uploadId) {
        this.uploadId = uploadId;
    }

    public long uploadId() {
        return this.uploadId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof C2SModelUploadFinishPacket)) {
            return false;
        }
        C2SModelUploadFinishPacket other = (C2SModelUploadFinishPacket) obj;
        return this.uploadId == other.uploadId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uploadId);
    }

    @Override
    public String toString() {
        return "C2SModelUploadFinishPacket[uploadId=" + this.uploadId + "]";
    }

    public static void encode(C2SModelUploadFinishPacket message, FriendlyByteBuf buf) {
        buf.writeVarLong(message.uploadId);
    }

    public static C2SModelUploadFinishPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadFinishPacket(buf.readVarLong());
    }

    public static void handle(C2SModelUploadFinishPacket message, PacketContext ctx) {
    }
}
