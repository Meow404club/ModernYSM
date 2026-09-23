package rip.ysm.legacy1710;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7.10 线模型注册表（legacy1710-l2a-model-load）。
 *
 * 1.12.2 LegacyModelRegistry 的 1.7.10 twin：UUID 键（entityId 重连会被复用，
 * UUID 才是玩家稳定身份）。L2a 落渲染读侧（modelIdOf 查表+缺省回退 default）；
 * SERVER_MODELS/assignServer 等服务端面由 L2b 网络卡消费。
 */
public final class LegacyModelRegistry {

    public static final String DEFAULT_MODEL_ID = "default";

    private static final Map<UUID, String> SERVER_MODELS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> CLIENT_MODELS = new ConcurrentHashMap<>();

    private LegacyModelRegistry() {
    }

    /** 渲染读侧（客户端）：按 UUID 查登记，缺省回退 default。 */
    public static String modelIdOf(UUID uuid) {
        String id = uuid == null ? null : CLIENT_MODELS.get(uuid);
        return id == null ? DEFAULT_MODEL_ID : id;
    }

    /** 服务端指派读取（渲染侧判断异模型 + 日志采证链同源）。 */
    public static String serverModelIdOf(UUID uuid) {
        String id = uuid == null ? null : SERVER_MODELS.get(uuid);
        return id == null ? DEFAULT_MODEL_ID : id;
    }

    /** 服务端：登录登记（LegacyConfig.modelIdFor 指派）。 */
    public static void assignServer(UUID uuid, String modelId) {
        if (uuid == null) {
            return;
        }
        if (modelId == null || modelId.isEmpty() || DEFAULT_MODEL_ID.equals(modelId)) {
            SERVER_MODELS.remove(uuid);
        } else {
            SERVER_MODELS.put(uuid, modelId);
        }
    }

    /** 服务端：登出清理（与登录配对；UUID 键下重连重登即重指派）。 */
    public static void removeServer(UUID uuid) {
        if (uuid != null) {
            SERVER_MODELS.remove(uuid);
        }
    }

    /** 客户端：S2C 同步落表。 */
    public static void applySync(UUID uuid, String modelId) {
        if (uuid == null) {
            return;
        }
        if (modelId == null || modelId.isEmpty() || DEFAULT_MODEL_ID.equals(modelId)) {
            CLIENT_MODELS.remove(uuid);
        } else {
            CLIENT_MODELS.put(uuid, modelId);
        }
    }
}
