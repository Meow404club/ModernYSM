package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class C2SModelUploadChunkPacket {
    private final long uploadId;
    private final int offset;
    private final byte[] data;

    public C2SModelUploadChunkPacket(long uploadId, int offset, byte[] data) {
        this.uploadId = uploadId;
        this.offset = offset;
        this.data = data;
    }

    public long uploadId() {
        return this.uploadId;
    }

    public int offset() {
        return this.offset;
    }

    public byte[] data() {
        return this.data;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof C2SModelUploadChunkPacket)) {
            return false;
        }
        C2SModelUploadChunkPacket other = (C2SModelUploadChunkPacket) obj;
        return this.uploadId == other.uploadId && this.offset == other.offset && Objects.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uploadId, this.offset, this.data);
    }

    @Override
    public String toString() {
        return "C2SModelUploadChunkPacket[uploadId=" + this.uploadId + ", offset=" + this.offset + ", data=" + this.data + "]";
    }

    public static void encode(C2SModelUploadChunkPacket message, FriendlyByteBuf buf) {
        buf.writeVarLong(message.uploadId);
        buf.writeVarInt(message.offset);
        buf.writeByteArray(message.data);
    }

    public static C2SModelUploadChunkPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadChunkPacket(buf.readVarLong(), buf.readVarInt(), buf.readByteArray());
    }

    public static void handle(C2SModelUploadChunkPacket message, PacketContext ctx) {
    }
}
