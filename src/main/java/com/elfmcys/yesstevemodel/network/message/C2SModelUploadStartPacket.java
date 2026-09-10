package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class C2SModelUploadStartPacket {
    private final String modelId;
    private final int totalBytes;
    private final String sha256;

    public C2SModelUploadStartPacket(String modelId, int totalBytes, String sha256) {
        this.modelId = modelId;
        this.totalBytes = totalBytes;
        this.sha256 = sha256;
    }

    public String modelId() {
        return this.modelId;
    }

    public int totalBytes() {
        return this.totalBytes;
    }

    public String sha256() {
        return this.sha256;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof C2SModelUploadStartPacket)) {
            return false;
        }
        C2SModelUploadStartPacket other = (C2SModelUploadStartPacket) obj;
        return this.totalBytes == other.totalBytes && Objects.equals(this.modelId, other.modelId) && Objects.equals(this.sha256, other.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modelId, this.totalBytes, this.sha256);
    }

    @Override
    public String toString() {
        return "C2SModelUploadStartPacket[modelId=" + this.modelId + ", totalBytes=" + this.totalBytes + ", sha256=" + this.sha256 + "]";
    }

    public static void encode(C2SModelUploadStartPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeVarInt(message.totalBytes);
        buf.writeUtf(message.sha256);
    }

    public static C2SModelUploadStartPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadStartPacket(buf.readUtf(), buf.readVarInt(), buf.readUtf());
    }

    public static void handle(C2SModelUploadStartPacket message, PacketContext ctx) {
    }
}
