package rip.ysm.legacy122;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.12.2 SimpleChannel 等价面（legacy-1222-l2-full 批②）。
 *
 * 1.12.2 = SimpleNetworkWrapper（forge-1.12.x SimpleNetworkWrapper.java:113：
 * NetworkRegistry.INSTANCE.newSimpleChannel 构造（NetworkRegistry.java:173）/
 * registerMessage(handler,type,discriminator,Side) :157/:183 / sendTo(EntityPlayerMP) :246 /
 * sendToAll :233），
 * 与 1202 孪生树 YSMChannelImpl（net.minecraftforge.network SimpleChannel）协议
 * 字节层同构（varint 实体 id + UTF 模型 id），单消息 S2C 同步。
 *
 * 服务端触发：玩家进世界（PlayerLoggedInEvent）向该玩家广播所有已渲染实体
 * （单默认模型线=发 default 占位登记，模型 id 由批④配置面切换）；实体渲染时
 * renderEntityWithYawPitch 前的按需补发由 LegacyRenderHook 查询注册表缺省兜底。
 */
public final class LegacySyncChannel {

    private static final String CHANNEL = "openysm:legacy122_model_sync";
    private static final int DISCRIMINATOR = 1;

    private static SimpleNetworkWrapper channel;

    private LegacySyncChannel() {
    }

    public static void init() {
        if (channel != null) {
            return;
        }
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        channel.registerMessage(LegacyModelSyncPacket.Handler.class,
                LegacyModelSyncPacket.class, DISCRIMINATOR, Side.CLIENT);
        System.out.println("[ysm-legacy122] sync channel registered: " + CHANNEL);
    }

    /** 服务端：向目标玩家同步实体模型登记（S2C）。 */
    public static void syncTo(EntityPlayerMP player, int entityId, String modelId) {
        if (channel != null && player != null) {
            channel.sendTo(new LegacyModelSyncPacket(entityId, modelId), player);
        }
    }

    /**
     * 服务端：玩家进世界即向其登记本实体模型（单默认模型线=每实体一行）。
     * EntityJoinWorldEvent（forge 1.12.x EntityJoinWorldEvent.java @HasResult 实证）
     * 只在服务端侧发 S2C——双实体不同模型即各发各的登记行。
     */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onEntityJoinWorld(
            net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote
                || !(event.getEntity() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
        syncTo(player, player.getEntityId(), LegacyModelRegistry.modelIdOf(player.getEntityId()));
        System.out.printf("[ysm-legacy122] sync on join: entity=%d model=%s%n",
                player.getEntityId(), LegacyModelRegistry.modelIdOf(player.getEntityId()));
    }

    /** 服务端：向所有在线玩家广播（单默认模型批量登记）。 */
    public static void broadcast(EntityPlayer player, String modelId) {
        if (channel != null && player instanceof EntityPlayerMP) {
            channel.sendTo(new LegacyModelSyncPacket(player.getEntityId(), modelId),
                    (EntityPlayerMP) player);
        }
    }
}
