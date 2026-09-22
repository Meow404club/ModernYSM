package rip.ysm.legacy122;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * C2S 模型选择包（L3-2 GUI）。
 *
 * 客户端 GUI 点击→sendToServer（SimpleNetworkWrapper :318，判别符 3）；服务端
 * 校验（model 在 listBuiltinModels 枚举内或 default）→ LegacyConfig.assign
 * 持久化（uuid=model 行）→ LegacyModelRegistry.assignServer 指派 → 自身
 * syncTo + syncToTracking 广播（L3-1 同步面复用，客户端收包后渲染惰性装载）。
 */
public class LegacyModelSelectPacket implements IMessage {

    private String modelId;

    public LegacyModelSelectPacket() {
    }

    public LegacyModelSelectPacket(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.modelId = LegacyModelSyncPacket.readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        LegacyModelSyncPacket.writeString(buf, this.modelId == null ? "" : this.modelId);
    }

    /** 服务端 handler：校验+持久化+指派+广播（主线程调度）。 */
    public static class Handler implements IMessageHandler<LegacyModelSelectPacket, IMessage> {
        @Override
        public IMessage onMessage(final LegacyModelSelectPacket message, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handleSelect(player, message.modelId);
                }
            });
            return null;
        }
    }

    private static void handleSelect(EntityPlayerMP player, String modelId) {
        if (modelId == null) {
            modelId = "";
        }
        boolean isDefault = LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId);
        if (!modelId.isEmpty() && !isDefault
                && !LegacyModelLoader.listBuiltinModels().contains(modelId)) {
            System.out.printf(
                    "[ysm-legacy122] model select rejected (unknown id): uuid=%s model=%s%n",
                    player.getUniqueID(), modelId);
            return;
        }
        java.util.UUID uuid = player.getUniqueID();
        rip.ysm.LegacyConfig.assign(uuid, modelId);
        LegacyModelRegistry.assignServer(uuid, modelId);
        // 自身（登录路径 syncTo 语义）+ 跟踪者广播；客户端收包 applySync 后
        // 渲染读侧惰性装载新模型，无需额外重载消息
        LegacySyncChannel.syncTo(player, uuid, LegacyModelRegistry.serverModelIdOf(uuid));
        LegacySyncChannel.syncToTracking(player);
        System.out.printf(
                "[ysm-legacy122] model selected: uuid=%s entityId=%d model=%s%n",
                uuid, player.getEntityId(), modelId.isEmpty() ? LegacyModelRegistry.DEFAULT_MODEL_ID : modelId);
    }
}
