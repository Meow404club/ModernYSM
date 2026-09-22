package rip.ysm.legacy122;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/**
 * S2C 实体模型同步包（legacy-1222-l2-full 批②；L3-1 改 UUID 键）。
 *
 * 1.12.2 SimpleChannel 等价面 = SimpleNetworkWrapper（forge-1.12.x
 * SimpleNetworkWrapper.java:113 实证：NetworkRegistry.INSTANCE.newChannel 构造 /
 * registerMessage(handler,type,discriminator,Side) / sendTo(EntityPlayerMP)）。
 * L3-1 字节面：8 字节 most/least（UUID，Entity.getUniqueID vanilla-mc-1.12.2
 * Entity.java:2172）+ UTF 模型 id——entityId 重连复用不串模型。
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

    // 包内共享字节面（L3-2 列表/选择包同构复用）
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
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacyModelRegistry.applySync(uuid, modelId);
                    System.out.printf(
                            "[ysm-legacy122] model sync: uuid=%s model=%s%n", uuid, modelId);
                }
            });
            return null;
        }
    }
}
