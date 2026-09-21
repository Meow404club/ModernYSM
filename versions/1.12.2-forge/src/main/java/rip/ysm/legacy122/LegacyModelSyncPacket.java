package rip.ysm.legacy122;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * S2C 实体模型同步包（legacy-1222-l2-full 批②）。
 *
 * 1.12.2 SimpleChannel 等价面 = SimpleNetworkWrapper（forge-1.12.x
 * SimpleNetworkWrapper.java:113 实证：NetworkRegistry.INSTANCE.newChannel 构造 /
 * registerMessage(handler,type,discriminator,Side) / sendTo(EntityPlayerMP)），
 * 1202 孪生树 YSMChannelImpl（SimpleChannel 语义）的字节层同构：varint 实体 id +
 * UTF 模型 id。单默认模型广播=登录/进世界时全实体批量发。
 */
public class LegacyModelSyncPacket implements IMessage {

    private int entityId;
    private String modelId;

    public LegacyModelSyncPacket() {
    }

    public LegacyModelSyncPacket(int entityId, String modelId) {
        this.entityId = entityId;
        this.modelId = modelId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = readVarint(buf);
        this.modelId = readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeVarint(buf, this.entityId);
        writeString(buf, this.modelId == null ? "" : this.modelId);
    }

    private static int readVarint(ByteBuf buf) {
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

    private static void writeVarint(ByteBuf buf, int value) {
        while ((value & ~0x7f) != 0) {
            buf.writeByte((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private static String readString(ByteBuf buf) {
        int len = readVarint(buf);
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarint(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /** 客户端 handler：落 LegacyModelRegistry（主线程调度的读面）。 */
    public static class Handler implements IMessageHandler<LegacyModelSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(LegacyModelSyncPacket message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }
            final int entityId = message.entityId;
            final String modelId = message.modelId;
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacyModelRegistry.applySync(entityId, modelId);
                    System.out.printf(
                            "[ysm-legacy122] model sync: entity=%d model=%s%n", entityId, modelId);
                }
            });
            return null;
        }
    }
}
