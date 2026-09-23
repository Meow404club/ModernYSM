package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
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
    private static float[] currentBoneParams;
    private static long animTick;
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
        currentBoneParams = boneParams;
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

    public static float[] currentBoneParams() {
        return currentBoneParams;
    }

    public static ResourceLocation texture() {
        return texture;
    }

    public static long nextAnimTick() {
        return ++animTick;
    }

    public static int boneCount() {
        return mainModel == null ? 0 : mainModel.bones.size();
    }

    public static String boneName(int idx) {
        return mainModel == null || idx >= mainModel.bones.size() ? null : mainModel.bones.get(idx).name;
    }
}
