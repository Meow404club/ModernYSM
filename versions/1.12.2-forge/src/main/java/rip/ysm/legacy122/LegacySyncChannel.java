package rip.ysm.legacy122;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/**
 * 1.12.2 SimpleChannel 等价面（legacy-1222-l2-full 批②；L3-1 双实体异模型）。
 *
 * 1.12.2 = SimpleNetworkWrapper（forge-1.12.x SimpleNetworkWrapper.java:113：
 * NetworkRegistry.INSTANCE.newSimpleChannel 构造（NetworkRegistry.java:173）/
 * registerMessage(handler,type,discriminator,Side) :157/:183 / sendTo(EntityPlayerMP) :246 /
 * sendToAllTracking(IMessage, Entity) :291——范围登记=按需补发层 1）。
 *
 * L3-1 事件配对：
 * - PlayerLoggedInEvent（forge-1.12.x PlayerEvent.java:86）：按 LegacyConfig.modelIdFor
 *   指派落 SERVER_MODELS，并把新玩家登记 sendToAllTracking 补发给其跟踪者；
 *   同时向新玩家发全量已登记模型（进世界即可渲染他人异模型）；
 * - PlayerLoggedOutEvent（:93）：removeServer 清理，UUID 键下 entityId 复用不串模型。
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

    /** 服务端：向目标玩家同步玩家模型登记（S2C，UUID 键）。 */
    public static void syncTo(EntityPlayerMP player, UUID entityUuid, String modelId) {
        if (channel != null && player != null) {
            channel.sendTo(new LegacyModelSyncPacket(entityUuid, modelId), player);
        }
    }

    /** 服务端：向跟踪该实体的所有玩家补发登记（SimpleNetworkWrapper :291）。 */
    public static void syncToTracking(EntityPlayerMP tracked) {
        if (channel != null && tracked != null) {
            UUID uuid = tracked.getUniqueID();
            String modelId = LegacyModelRegistry.serverModelIdOf(uuid);
            channel.sendToAllTracking(new LegacyModelSyncPacket(uuid, modelId), tracked);
        }
    }

    /** 服务端：向新玩家发全量已登记模型（进世界即知在榜玩家各自的模型）。 */
    private static void syncAllTo(EntityPlayerMP player) {
        for (EntityPlayerMP other : player.getServer().getPlayerList().getPlayers()) {
            UUID uuid = other.getUniqueID();
            syncTo(player, uuid, LegacyModelRegistry.serverModelIdOf(uuid));
        }
    }

    /** 服务端：登录登记 + 双向同步（配对：PlayerLoggedOutEvent 清理）。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)
                || event.player.world == null || event.player.world.isRemote) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID uuid = player.getUniqueID();
        String modelId = rip.ysm.LegacyConfig.modelIdFor(uuid);
        LegacyModelRegistry.assignServer(uuid, modelId);
        syncAllTo(player);
        syncToTracking(player);
        System.out.printf(
                "[ysm-legacy122] assign on login: uuid=%s entityId=%d model=%s%n",
                uuid, player.getEntityId(), modelId);
    }

    /** 服务端：登出清理（与登录配对——重连复用 entityId 不会串模型）。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        UUID uuid = event.player.getUniqueID();
        LegacyModelRegistry.removeServer(uuid);
        System.out.printf("[ysm-legacy122] cleanup on logout: uuid=%s entityId=%d%n",
                uuid, event.player.getEntityId());
    }
}
