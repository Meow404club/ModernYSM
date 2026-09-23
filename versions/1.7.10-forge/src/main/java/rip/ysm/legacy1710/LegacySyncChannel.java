package rip.ysm.legacy1710;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 1.7.10 SimpleChannel 等价面（legacy1710-l2b-network；蓝本 122 LegacySyncChannel）。
 *
 * 1.7.10 = cpw.mods.fml SimpleNetworkWrapper（forge-1.7.10 SimpleNetworkWrapper.java:85：
 * NetworkRegistry.INSTANCE.newSimpleChannel（NetworkRegistry.java:164）/
 * registerMessage(handler,type,discriminator,Side) :104 / sendTo(EntityPlayerMP) :192 /
 * sendToServer :233）。三硬差异：
 * ①通道名 ≤20 字符（vanilla S3FPacketCustomPayload.java:29 readStringFromBuffer(20)）
 *   →定名 openysm_model_sync（18 字符，init 内断言）；
 * ②无 sendToAllTracking→sendToAllAround(TargetPoint,512)（FMLOutboundHandler:176
 *   ALLAROUNDPOINT 按维度+距离；512=EntityTracker.java:54 玩家跟踪距离，过近似）；
 * ③PlayerEvent 在 FML 总线（FMLCommonHandler:548 firePlayer*）→
 *   FMLCommonHandler.instance().bus().register(实例)（cpw EventBus :45 仅 register(Object)）。
 *
 * 事件配对：
 * - PlayerLoggedInEvent：按 LegacyConfig.modelIdFor 指派落 SERVER_MODELS，向新玩家发
 *   全量已登记模型+可用列表，并向其周围补发其登记（进世界即可渲染/被渲染异模型）；
 * - PlayerLoggedOutEvent：removeServer 清理，UUID 键下 entityId 复用不串模型。
 */
public final class LegacySyncChannel {

    /** ≤20 字符硬约束（vanilla custom payload readStringFromBuffer(20)）。 */
    private static final String CHANNEL = "openysm_model_sync";
    private static final int DISCRIMINATOR = 1;
    /** 玩家跟踪距离近似（vanilla EntityTracker.java:54 trackEntity(...,512,...)）。 */
    private static final double TRACKING_RANGE = 512.0D;

    private static SimpleNetworkWrapper channel;
    /** 客户端可用模型缓存（ListPacket 落；L3a GUI 读）。 */
    private static volatile List<String> availableModels = Collections.emptyList();

    private LegacySyncChannel() {
    }

    public static void init() {
        if (channel != null) {
            return;
        }
        if (CHANNEL.length() > 20) {
            throw new IllegalStateException(
                    "channel name >20 chars (vanilla S3F readStringFromBuffer(20)): " + CHANNEL);
        }
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        channel.registerMessage(LegacyModelSyncPacket.Handler.class,
                LegacyModelSyncPacket.class, DISCRIMINATOR, Side.CLIENT);
        // 可用模型列表（S2C）+ 玩家选择（C2S）
        channel.registerMessage(LegacyModelListPacket.Handler.class,
                LegacyModelListPacket.class, DISCRIMINATOR + 1, Side.CLIENT);
        channel.registerMessage(LegacyModelSelectPacket.Handler.class,
                LegacyModelSelectPacket.class, DISCRIMINATOR + 2, Side.SERVER);
        // PlayerEvent 走 FML 总线（1.7.10 双总线代）；cpw EventBus 仅 register(Object)，
        // static @SubscribeEvent 方法经实例注册照常触发（M-U3 实证）
        FMLCommonHandler.instance().bus().register(new LegacySyncChannel());
        System.out.println("[ysm-legacy1710] sync channel registered: " + CHANNEL);
    }

    /** 客户端：ListPacket 落缓存（L3a GUI 读）。 */
    static void setAvailable(List<String> ids) {
        availableModels = ids == null ? Collections.<String>emptyList() : ids;
    }

    /** 客户端可用模型缓存读侧（L3a GUI）。 */
    public static List<String> availableModels() {
        return availableModels;
    }

    /** 服务端：向目标玩家同步玩家模型登记（S2C，UUID 键）。 */
    public static void syncTo(EntityPlayerMP player, UUID entityUuid, String modelId) {
        if (channel != null && player != null) {
            channel.sendTo(new LegacyModelSyncPacket(entityUuid, modelId), player);
        }
    }

    /**
     * 服务端：向跟踪该实体的玩家补发登记。1.7.10 无 sendToAllTracking→
     * ALLAROUNDPOINT 512 过近似（按维度+距离，非实际跟踪集）。
     */
    public static void syncToAround(EntityPlayerMP tracked) {
        if (channel != null && tracked != null) {
            UUID uuid = tracked.getUniqueID();
            String modelId = LegacyModelRegistry.serverModelIdOf(uuid);
            channel.sendToAllAround(new LegacyModelSyncPacket(uuid, modelId),
                    new NetworkRegistry.TargetPoint(tracked.dimension,
                            tracked.posX, tracked.posY, tracked.posZ, TRACKING_RANGE));
        }
    }

    /** 服务端：下发可用模型列表（登录时随登记同步）。 */
    public static void sendAvailable(EntityPlayerMP player) {
        if (channel != null && player != null) {
            channel.sendTo(new LegacyModelListPacket(LegacyModelLoader.listBuiltinModels()), player);
        }
    }

    /** 客户端：GUI 选择回传（C2S；单机走集成服回环）。 */
    public static void requestSelect(String modelId) {
        if (channel != null) {
            channel.sendToServer(new LegacyModelSelectPacket(modelId));
        }
    }

    /** 服务端：向新玩家发全量已登记模型（进世界即知在榜玩家各自的模型）。 */
    private static void syncAllTo(EntityPlayerMP player) {
        for (EntityPlayerMP other : (List<EntityPlayerMP>) FMLCommonHandler.instance()
                .getMinecraftServerInstance().getConfigurationManager().playerEntityList) {
            UUID uuid = other.getUniqueID();
            syncTo(player, uuid, LegacyModelRegistry.serverModelIdOf(uuid));
        }
    }

    /**
     * 服务端：登录登记 + 双向同步（配对：PlayerLoggedOutEvent 清理）。
     * 实例方法（非 static）：cpw ASMEventHandler 生成的调用类固定
     * INVOKEVIRTUAL+CHECKCAST 到监听器实例（ASMEventHandler.java:108-120），
     * static 方法经实例注册会在事件发布点抛 IncompatibleClassChangeError
     * （run1 集成服实证）——与 M-U3「监听器类须 public」同为 cpw 代际坑。
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)
                || event.player.worldObj == null || event.player.worldObj.isRemote) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID uuid = player.getUniqueID();
        String modelId = rip.ysm.LegacyConfig.modelIdFor(uuid);
        LegacyModelRegistry.assignServer(uuid, modelId);
        syncAllTo(player);
        syncToAround(player);
        sendAvailable(player);
        System.out.printf(
                "[ysm-legacy1710] assign on login: uuid=%s entityId=%d model=%s%n",
                uuid, player.getEntityId(), modelId);
    }

    /** 服务端：登出清理（与登录配对——重连复用 entityId 不会串模型）。 */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        UUID uuid = event.player.getUniqueID();
        LegacyModelRegistry.removeServer(uuid);
        System.out.printf("[ysm-legacy1710] cleanup on logout: uuid=%s entityId=%d%n",
                uuid, event.player.getEntityId());
    }
}
