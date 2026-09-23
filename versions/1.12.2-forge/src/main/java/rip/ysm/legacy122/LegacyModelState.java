package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 1.12.2 线渲染状态（legacy-1222-l1-render；L3-1 双实体异模型扩展）。
 *
 * 持默认模型的 GeoModel + 每骨动画参数面（12 float/骨契约，
 * NativeModelRenderer.calculateBoneMatrix:328 消费侧同款布局）。
 * L3-1：按模型 id 的装载缓存（MODELS/TEXTURES/PARAMS）——渲染读侧按 UUID 查
 * LegacyModelRegistry 得模型 id，再从本表取模型；未装载的 id 由
 * LegacyModelLoader.loadModel 惰性装载后入表。default 仍走 mainModel 主面。
 */
public final class LegacyModelState {

    private static ClientModelInfo bundle;
    private static ClientModelInfo mainBundle;
    private static GeoModel mainModel;
    private static float[] boneParams;
    private static float[] currentBoneParams;
    private static long animTick;
    private static ResourceLocation texture;
    private static OuterFileTexture boundTexture;

    // L3-1 按模型 id 缓存（default 之外的模型装载后入表，params 每模型一份）
    private static final Map<String, GeoModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, OuterFileTexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, float[]> PARAMS = new ConcurrentHashMap<>();
    // M-U2 r3 语义A：保留解析 bundle——preview_animation/disable_preview_rotation/
    // height_scale/动画文件全在里面（主线 ModelAssembly 同构），预览播放器按 id 取用
    private static final Map<String, ClientModelInfo> BUNDLES = new ConcurrentHashMap<>();

    private LegacyModelState() {
    }

    public static void setBundle(ClientModelInfo info) {
        MainModelData data = info == null ? null : info.getMainModelData();
        setBundle(info,
                data == null || data.getModels().isEmpty() ? null : data.getModels().get(0));
    }

    // L1 程序化模型入口（OpenYSMStub：装载链 L2 接入前直喂 GeoModel）
    public static void setBundle(ClientModelInfo info, GeoModel model) {
        setBundle(info, model, null);
    }

    // L2 真实装载入口（LegacyModelLoader）：真实贴图直持 OuterFileTexture
    //（AbstractTexture 子类，RenderManager.renderEngine.bindTexture 直通）
    public static void setBundle(ClientModelInfo info, GeoModel model, Object realTexture) {
        bundle = info;
        mainBundle = info;
        mainModel = model;
        boneParams = mainModel == null || mainModel.bakedBones == null
                ? null : new float[mainModel.bakedBones.size() * 12];
        currentBoneParams = boneParams;
        // L2 纹理面：真实贴图优先（OuterFileTexture 直 bind），null 回退占位皮肤
        boundTexture = realTexture instanceof OuterFileTexture
                ? (OuterFileTexture) realTexture : null;
        texture = boundTexture != null
                ? null : new ResourceLocation("yes_steve_model", "textures/entity/default.png");
        // twin YesSteveModel 的 main-compileJava classpath 解析在本环布局下不稳定
        //（pass81 实证"package YesSteveModel does not exist"）——ponytail: 直接 JUL，
        // 不为一条日志维护 twin 编译序
        Logger.getLogger("yes_steve_model").info(String.format(
                "[ysm-legacy122] state set: model=%b bones=%d realTex=%b",
                mainModel != null, mainModel == null || mainModel.bakedBones == null
                        ? -1 : mainModel.bakedBones.size(),
                realTexture != null));
    }

    /** L3-1：装载结果入表（LegacyModelLoader.loadModel 成功后调）。 */
    public static void registerModel(String modelId, ClientModelInfo info,
                                     GeoModel model, OuterFileTexture tex) {
        MODELS.put(modelId, model);
        TEXTURES.put(modelId, tex);
        PARAMS.put(modelId, model == null || model.bakedBones == null
                ? null : new float[model.bakedBones.size() * 12]);
        BUNDLES.put(modelId, info);
        Logger.getLogger("yes_steve_model").info(String.format(
                "[ysm-legacy122] model registered: id=%s bones=%d realTex=%b",
                modelId,
                model == null || model.bakedBones == null ? -1 : model.bakedBones.size(),
                tex != null));
    }

    /** L3-1：按模型 id 取已装载模型；default 走 mainModel 主面。 */
    public static GeoModel modelOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return mainModel;
        }
        return MODELS.get(modelId);
    }

    /** L3-1：按模型取骨参数面（非本表模型返回 null）。 */
    public static float[] paramsOf(String modelId, GeoModel model) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return model == mainModel ? boneParams : null;
        }
        float[] params = PARAMS.get(modelId);
        return model != null && params != null && params.length == model.bakedBones.size() * 12
                ? params : null;
    }

    /** L3-1：按模型 id 取纹理；default 走主面。 */
    public static OuterFileTexture textureOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return boundTexture;
        }
        return TEXTURES.get(modelId);
    }

    /** M-U2 r3：按模型 id 取解析 bundle（ModelProperties+动画文件）；default 走主面。 */
    public static ClientModelInfo bundleOf(String modelId) {
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId) || modelId == null) {
            return mainBundle;
        }
        return BUNDLES.get(modelId);
    }

    /** L3-3：已装载非 default 模型 id 集（重载枚举面；MODELS 活键集视图）。 */
    public static java.util.Set<String> loadedModelIds() {
        return java.util.Collections.unmodifiableSet(MODELS.keySet());
    }

    public static OuterFileTexture realTexture() {
        return boundTexture;
    }

    public static GeoModel mainModel() {
        return mainModel;
    }

    public static float[] boneParams(GeoModel model) {
        return model == mainModel ? boneParams : null;
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

    public static long animTick() {
        return animTick;
    }
}
