package com.elfmcys.yesstevemodel.network.message;

//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.util.Objects;

public final class S2CModelUploadResultPacket {
    private final long uploadId;
    private final byte status;
    private final String modelId;
    private final long h1;
    private final long h2;
    private final String message;

    public S2CModelUploadResultPacket(long uploadId, byte status, String modelId, long h1, long h2, String message) {
        this.uploadId = uploadId;
        this.status = status;
        this.modelId = modelId;
        this.h1 = h1;
        this.h2 = h2;
        this.message = message;
    }

    public long uploadId() {
        return this.uploadId;
    }

    public byte status() {
        return this.status;
    }

    public String modelId() {
        return this.modelId;
    }

    public long h1() {
        return this.h1;
    }

    public long h2() {
        return this.h2;
    }

    public String message() {
        return this.message;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof S2CModelUploadResultPacket)) {
            return false;
        }
        S2CModelUploadResultPacket other = (S2CModelUploadResultPacket) obj;
        return this.uploadId == other.uploadId && this.status == other.status && this.h1 == other.h1 && this.h2 == other.h2
                && Objects.equals(this.modelId, other.modelId) && Objects.equals(this.message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.uploadId, this.status, this.modelId, this.h1, this.h2, this.message);
    }

    @Override
    public String toString() {
        return "S2CModelUploadResultPacket[uploadId=" + this.uploadId + ", status=" + this.status + ", modelId=" + this.modelId
                + ", h1=" + this.h1 + ", h2=" + this.h2 + ", message=" + this.message + "]";
    }

    public static void encode(S2CModelUploadResultPacket packet, FriendlyByteBuf buf) {
        buf.writeVarLong(packet.uploadId);
        buf.writeByte(packet.status);
        buf.writeUtf(packet.modelId);
        buf.writeVarLong(packet.h1);
        buf.writeVarLong(packet.h2);
        buf.writeUtf(packet.message);
    }

    public static S2CModelUploadResultPacket decode(FriendlyByteBuf buf) {
        return new S2CModelUploadResultPacket(buf.readVarLong(), buf.readByte(), buf.readUtf(32767), buf.readVarLong(), buf.readVarLong(), buf.readUtf(32767));
    }

    public static void handle(S2CModelUploadResultPacket packet, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> handleOnClient(packet));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(S2CModelUploadResultPacket packet) {
        ModelUploadSession.onResult(packet.uploadId, packet.status, packet.modelId, packet.h1, packet.h2, packet.message);
    }
}
