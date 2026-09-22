package rip.ysm.legacy122;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C 可用模型列表包（L3-2 GUI）。
 *
 * 服务端登录时下发 LegacyModelLoader.listBuiltinModels() 枚举面（两级 id
 * "wine_fox/01_taisho_maid"），客户端缓存供 GUI 列出。字节层复用
 * LegacyModelSyncPacket 的 varint+UTF（SimpleNetworkWrapper 判别符 2）。
 */
public class LegacyModelListPacket implements IMessage {

    private List<String> modelIds;

    public LegacyModelListPacket() {
    }

    public LegacyModelListPacket(List<String> modelIds) {
        this.modelIds = modelIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = LegacyModelSyncPacket.readVarint(buf);
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(LegacyModelSyncPacket.readString(buf));
        }
        this.modelIds = ids;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        List<String> ids = this.modelIds == null ? java.util.Collections.<String>emptyList() : this.modelIds;
        LegacyModelSyncPacket.writeVarint(buf, ids.size());
        for (String id : ids) {
            LegacyModelSyncPacket.writeString(buf, id == null ? "" : id);
        }
    }

    /** 客户端 handler：缓存列表供 GUI 读（主线程调度）。 */
    public static class Handler implements IMessageHandler<LegacyModelListPacket, IMessage> {
        @Override
        public IMessage onMessage(final LegacyModelListPacket message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacyModelSelectScreen.setAvailable(message.modelIds);
                    System.out.printf("[ysm-legacy122] available models: %d entries%n",
                            message.modelIds == null ? 0 : message.modelIds.size());
                }
            });
            return null;
        }
    }
}
