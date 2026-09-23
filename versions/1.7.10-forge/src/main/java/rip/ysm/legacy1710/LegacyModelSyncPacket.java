package rip.ysm.legacy1710;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * S2C 实体模型同步包（legacy1710-l2b-network；蓝本 122 LegacyModelSyncPacket）。
 *
 * 1.7.10 SimpleChannel 等价面 = cpw.mods.fml SimpleNetworkWrapper（forge-1.7.10
 * SimpleNetworkWrapper.java:85：NetworkRegistry.INSTANCE.newChannel 构造 /
 * registerMessage(handler,type,discriminator,Side) :104 / sendTo(EntityPlayerMP) :192）。
 * 字节面与 122 同构：8 字节 most/least（UUID）+ varint+UTF 模型 id——entityId
 * 重连复用不串模型，UUID 键。
 */
public class LegacyModelSyncPacket implements IMessage {

    private UUID entityUuid;
    private String modelId;

    public LegacyModelSyncPacket() {
    }

    public LegacyModelSyncPacket(UUID entityUuid, String modelId) {
        this.entityUuid = entityUuid;
        this.modelId = modelId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityUuid = new UUID(buf.readLong(), buf.readLong());
        this.modelId = readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        UUID uuid = this.entityUuid == null ? new UUID(0L, 0L) : this.entityUuid;
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        writeString(buf, this.modelId == null ? "" : this.modelId);
    }

    // 包内共享字节面（列表/选择包同构复用；122 varint+UTF 直拷）
    static String readString(ByteBuf buf) {
        int len = readVarint(buf);
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    static void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarint(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    static int readVarint(ByteBuf buf) {
        int value = 0;
        int shift = 0;
        while (true) {
            byte b = buf.readByte();
            value |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
        }
    }

    static void writeVarint(ByteBuf buf, int value) {
        while ((value & ~0x7f) != 0) {
            buf.writeByte((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    /** 客户端 handler：落 LegacyModelRegistry（主线程调度的读面）。 */
    public static class Handler implements IMessageHandler<LegacyModelSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(LegacyModelSyncPacket message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }
            final UUID uuid = message.entityUuid;
            final String modelId = message.modelId;
            // S2C 落主线程：1.7.10 Minecraft.addScheduledTask 存在（Minecraft.java:2160）
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacyModelRegistry.applySync(uuid, modelId);
                    System.out.printf(
                            "[ysm-legacy1710] model sync: uuid=%s model=%s%n", uuid, modelId);
                }
            });
            return null;
        }
    }
}
