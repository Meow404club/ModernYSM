package rip.ysm.legacy122;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.12.2 线按实体 id 的模型注册表（legacy-1222-l2-full 批②）。
 *
 * 对齐 1.20.1 线 PlayerCapability 面的语义：实体 id → 模型 id。服务端写
 * （syncAll/applySync），客户端读（modelIdOf）；默认 "default"（内置模型），
 * 与 LegacyModelLoader 装载的 builtin id 一致。
 */
public final class LegacyModelRegistry {

    public static final String DEFAULT_MODEL_ID = "default";

    private static final Map<Integer, String> ENTITY_MODELS = new ConcurrentHashMap<>();

    private LegacyModelRegistry() {
    }

    public static String modelIdOf(int entityId) {
        return ENTITY_MODELS.getOrDefault(entityId, DEFAULT_MODEL_ID);
    }

    public static void applySync(int entityId, String modelId) {
        if (modelId == null || modelId.isEmpty() || DEFAULT_MODEL_ID.equals(modelId)) {
            ENTITY_MODELS.remove(entityId);
        } else {
            ENTITY_MODELS.put(entityId, modelId);
        }
    }

    public static void clear() {
        ENTITY_MODELS.clear();
    }
}
