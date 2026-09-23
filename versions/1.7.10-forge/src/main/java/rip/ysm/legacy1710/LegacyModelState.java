package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 1.7.10 线渲染状态（legacy-1710-l1-render → L2a 升级 122 形态）。
 *
 * 1.12.2 LegacyModelState 的 1.7.10 twin：持默认模型烘焙几何（LegacyBakedModel，
 * GeoModel 经 LegacyGeoModelConverter 机械映射）+ 每骨动画参数面（12 float/骨
 * 契约）。L2a 按模型 id 的装载缓存（MODELS/TEXTURES/PARAMS/BUNDLES）——渲染读侧
 * 按 UUID 查 LegacyModelRegistry 得模型 id，再从本表取模型；未装载的 id 由
 * LegacyModelLoader.loadModel 惰性装载后入表。default 走 mainModel 主面。
 */
public final class LegacyModelState {

    private static ClientModelInfo mainBundle;
    private static LegacyBakedModel mainModel;
    private static float[] boneParams;
    private static ResourceLocation texture;
    private static OuterFileTexture boundTexture;

    // 按模型 id 缓存（default 之外的模型装载后入表，params 每模型一份）
    private static final Map<String, LegacyBakedModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, OuterFileTexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, float[]> PARAMS = new ConcurrentHashMap<>();
    // 保留解析 bundle——preview_animation/height_scale/动画文件全在里面，预览采样器按 id 取用
    private static final Map<String, ClientModelInfo> BUNDLES = new ConcurrentHashMap<>();

    private LegacyModelState() {
    }

    // L1 程序化模型入口（装载失败回退路径：LegacyTestModel + 占位皮肤）
    public static void setModel(LegacyBakedModel model, ResourceLocation tex) {
        setBundle(null, model, null);
        texture = model == null ? null : tex;
    }

    // L2 真实装载入口（LegacyModelLoader）：真实贴图直持 OuterFileTexture（ensureUploaded
    // 后走 GL11 直绑），null 回退占位皮肤 ResourceLocation
    public static void setBundle(ClientModelInfo info, LegacyBakedModel model, Object realTexture) {
        mainBundle = info;
        mainModel = model;
        boneParams = model == null ? null : new float[model.bones.size() * 12];
        boundTexture = realTexture instanceof OuterFileTexture
                ? (OuterFileTexture) realTexture : null;
        texture = boundTexture != null
                ? null : new ResourceLocation("yes_steve_model", "textures/entity/default.png");
        if (info != null) {
            BUNDLES.put(LegacyModelRegistry.DEFAULT_MODEL_ID, info);
        }
        // ponytail: 直接 JUL，不为一条日志维护 twin 编译序（122 线同款）
        Logger.getLogger("yes_steve_model").info(String.format(
                "[ysm-legacy1710] state set: model=%b bones=%d realTex=%b",
                mainModel != null, model == null ? -1 : model.bones.size(),
                realTexture != null));
    }

    /** 装载结果入表（LegacyModelLoader.loadModel 成功后调）。 */
    public static void registerModel(String modelId, ClientModelInfo info,
                                     LegacyBakedModel model, OuterFileTexture tex) {
        MODELS.put(modelId, model);
        TEXTURES.put(modelId, tex);
        PARAMS.put(modelId, model == null ? null : new float[model.bones.size() * 12]);
        BUNDLES.put(modelId, info);
        Logger.getLogger("yes_steve_model").info(String.format(
                "[ysm-legacy1710] model registered: id=%s bones=%d realTex=%b",
                modelId, model == null ? -1 : model.bones.size(), tex != null));
    }

    /** 按模型 id 取已装载模型；default 走 mainModel 主面。 */
    public static LegacyBakedModel modelOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return mainModel;
        }
        return MODELS.get(modelId);
    }

    /** 按模型取骨参数面（非本表模型返回 null）。 */
    public static float[] paramsOf(String modelId, LegacyBakedModel model) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return model == mainModel ? boneParams : null;
        }
        float[] params = PARAMS.get(modelId);
        return model != null && params != null && params.length == model.bones.size() * 12
                ? params : null;
    }

    /** 已装载模型 id 集（default 走主面不入表；热重载面枚举用，122 同名同义）。 */
    public static java.util.Set<String> loadedModelIds() {
        return java.util.Collections.unmodifiableSet(MODELS.keySet());
    }

    /** 按模型 id 取纹理；default 走主面。 */
    public static OuterFileTexture textureOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return boundTexture;
        }
        return TEXTURES.get(modelId);
    }

    // ponytail: 变体表不落缓存，直接读 bundle textureMap（OrderedStringMap 保序，
    // 装载/热重载后自动跟随新 bundle；单模型变体量级个位数，逐帧读无压力）
    private static final Map<UUID, String> LAST_VARIANT = new ConcurrentHashMap<>();

    /**
     * 多纹理变体表（B2）：bundle textureMap 保序快照（name→texture）。
     * default 走主面 bundle；程序化回退路径（bundle=null）返回 null。
     */
    public static OrderedStringMap<String, OuterFileTexture> variantsOf(String modelId) {
        ClientModelInfo info = bundleOf(modelId);
        MainModelData data = info == null ? null : info.getMainModelData();
        return data == null ? null : data.getTextureMap();
    }

    /**
     * 按玩家稳定选变体（B2 多纹理面）。主线选择面=GUI 选名+网络同步 per-player
     * textureIndex（PlayerTextureScreen→C2SRequestSwitchModelPacket 服务端校验→
     * S2CSetModelAndTexturePacket→LivingAnimatable.updateCurrentTexture indexOf），
     * legacy 无该同步链——可达成口径=UUID hash 稳定散列（同玩家同模型恒同变体，
     * 玩家间差异化）；单纹理模型恒 0 零回归。变更换发一条绑定日志
     * （textureIndex/绑定名=验收采证面）。122 LegacyModelState 同名同义 twin。
     */
    public static OuterFileTexture variantOf(String modelId, UUID uuid) {
        OrderedStringMap<String, OuterFileTexture> map = variantsOf(modelId);
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (uuid == null || map.size() == 1) {
            return map.getValueAt(0);
        }
        long bits = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int idx = Math.floorMod((int) (bits ^ (bits >>> 32)) ^ modelId.hashCode(), map.size());
        OuterFileTexture tex = map.getValuesList().get(idx);
        // 防膨胀：>256 玩家全清，误清代价=重打一条日志
        String sig = modelId + "|" + idx;
        if (!sig.equals(LAST_VARIANT.get(uuid))) {
            if (LAST_VARIANT.size() > 256) {
                LAST_VARIANT.clear();
            }
            LAST_VARIANT.put(uuid, sig);
            Logger.getLogger("yes_steve_model").info(String.format(
                    "[ysm-legacy1710] texture variant: player=%s model=%s textureIndex=%d/%d name=%s",
                    uuid, modelId, idx, map.size(), map.getKeyAt(idx)));
        }
        return tex;
    }

    /** 按模型 id 取解析 bundle（ModelProperties+动画文件）；default 走主面。 */
    public static ClientModelInfo bundleOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return mainBundle;
        }
        return BUNDLES.get(modelId);
    }

    public static OuterFileTexture realTexture() {
        return boundTexture;
    }

    public static LegacyBakedModel mainModel() {
        return mainModel;
    }

    public static float[] boneParams(LegacyBakedModel model) {
        return model != null && model == mainModel ? boneParams : null;
    }

    public static ResourceLocation texture() {
        return texture;
    }

    public static int boneCount() {
        return mainModel == null ? 0 : mainModel.bones.size();
    }

    public static String boneName(int idx) {
        return mainModel == null || idx >= mainModel.bones.size() ? null : mainModel.bones.get(idx).name;
    }
}
