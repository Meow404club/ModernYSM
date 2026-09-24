package rip.ysm.legacy1710;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * C2S 模型选择包（蓝本 122 LegacyModelSelectPacket）。
 *
 * 客户端 requestSelect→sendToServer（判别符 3）；服务端校验（model 在
 * listBuiltinModels 枚举内或 default）→ LegacyConfig.assign 持久化（uuid=model 行）
 * → LegacyModelRegistry.assignServer 指派 → 自身 syncTo + syncToAround 广播。
 *
 * 1.7.10 三硬差异之一：C2S handler 跑 netty 线程（NetworkDispatcher:299 直通，
 * 1.7.10 无 MinecraftServer.addScheduledTask）→ handleSelect 内联执行，只触
 * classpath 纯读/CHM registry/ConcurrentHashMap config/netty channel 写，线程安全。
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

    /** 服务端 handler：校验+持久化+指派+广播（netty 线程内联，见类注）。 */
    public static class Handler implements IMessageHandler<LegacyModelSelectPacket, IMessage> {
        @Override
        public IMessage onMessage(final LegacyModelSelectPacket message, MessageContext ctx) {
            if (ctx.side != Side.SERVER) {
                return null;
            }
            // 1.7.10 字段直读（NetHandlerPlayServer.java:90 playerEntity；122 为 getter player）
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            handleSelect(player, message.modelId);
            return null;
        }
    }

    private static void handleSelect(EntityPlayerMP player, String modelId) {
        if (modelId == null) {
            modelId = "";
        }
        // CAN_SWITCH_MODEL 门禁（wave-d-b4）：主线 C2SRequestSwitchModelPacket.handle:35
        // 同语义——开关关闭时静默拒收切换请求（拒绝日志=采证锚点）
        if (!rip.ysm.LegacyConfig.canSwitchModel()) {
            System.out.printf(
                    "[ysm-legacy1710] model select rejected (can_switch_model=false): uuid=%s model=%s%n",
                    player.getUniqueID(), modelId);
            return;
        }
        boolean isDefault = LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId);
        if (!modelId.isEmpty() && !isDefault
                && !LegacyModelLoader.listBuiltinModels().contains(modelId)) {
            System.out.printf(
                    "[ysm-legacy1710] model select rejected (unknown id): uuid=%s model=%s%n",
                    player.getUniqueID(), modelId);
            return;
        }
        java.util.UUID uuid = player.getUniqueID();
        rip.ysm.LegacyConfig.assign(uuid, modelId);
        LegacyModelRegistry.assignServer(uuid, modelId);
        // 自身（登录路径 syncTo 语义）+ 跟踪者广播；客户端收包 applySync 后
        // 渲染读侧惰性装载新模型，无需额外重载消息
        LegacySyncChannel.syncTo(player, uuid, LegacyModelRegistry.serverModelIdOf(uuid));
        LegacySyncChannel.syncToAround(player);
        System.out.printf(
                "[ysm-legacy1710] model selected: uuid=%s entityId=%d model=%s%n",
                uuid, player.getEntityId(), modelId.isEmpty() ? LegacyModelRegistry.DEFAULT_MODEL_ID : modelId);
    }
}
