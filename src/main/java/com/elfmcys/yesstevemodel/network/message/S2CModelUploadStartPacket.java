package com.elfmcys.yesstevemodel.network.message;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class S2CModelUploadStartPacket {
    private final long uploadId;
    private final byte status;
    private final int chunkSize;
    private final int maxTotalBytes;
    private final int chunksPerTick;
    private final String message;

    public S2CModelUploadStartPacket(long uploadId, byte status, int chunkSize, int maxTotalBytes, int chunksPerTick, String message) {
        this.uploadId = uploadId;
        this.status = status;
        this.chunkSize = chunkSize;
        this.maxTotalBytes = maxTotalBytes;
        this.chunksPerTick = chunksPerTick;
        this.message = message;
    }

    public long uploadId() {
        return this.uploadId;
    }

    public byte status() {
        return this.status;
    }

    public int chunkSize() {
        return this.chunkSize;
    }

    public int maxTotalBytes() {
        return this.maxTotalBytes;
    }

    public int chunksPerTick() {
        return this.chunksPerTick;
    }

    public String message() {
        return this.message;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof S2CModelUploadStartPacket)) {
            return false;
        }
        S2CModelUploadStartPacket other = (S2CModelUploadStartPacket) obj;
        return this.uploadId == other.uploadId && this.status == other.status && this.chunkSize == other.chunkSize
                && this.maxTotalBytes == other.maxTotalBytes && this.chunksPerTick == other.chunksPerTick
                && Objects.equals(this.message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uploadId, this.status, this.chunkSize, this.maxTotalBytes, this.chunksPerTick, this.message);
    }

    @Override
    public String toString() {
        return "S2CModelUploadStartPacket[uploadId=" + this.uploadId + ", status=" + this.status + ", chunkSize=" + this.chunkSize
                + ", maxTotalBytes=" + this.maxTotalBytes + ", chunksPerTick=" + this.chunksPerTick + ", message=" + this.message + "]";
    }

    public static void encode(S2CModelUploadStartPacket packet, FriendlyByteBuf buf) {
        buf.writeVarLong(packet.uploadId);
        buf.writeByte(packet.status);
        buf.writeVarInt(packet.chunkSize);
        buf.writeVarInt(packet.maxTotalBytes);
        buf.writeVarInt(packet.chunksPerTick);
        buf.writeUtf(packet.message);
    }

    public static S2CModelUploadStartPacket decode(FriendlyByteBuf buf) {
        long uploadId = buf.readVarLong();
        byte status = buf.readByte();
        int chunkSize = buf.readVarInt();
        int maxTotalBytes = buf.readVarInt();
        int chunksPerTick = buf.readVarInt();
        String message = buf.readUtf();
        return new S2CModelUploadStartPacket(uploadId, status, chunkSize, maxTotalBytes, chunksPerTick, message);
    }

    public static void handle(S2CModelUploadStartPacket packet, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> handleOnClient(packet));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(S2CModelUploadStartPacket packet) {
        ModelUploadSession.onStartAck(packet.uploadId, packet.status, packet.chunkSize, packet.maxTotalBytes, packet.chunksPerTick, packet.message);
    }
}
