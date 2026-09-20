package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 线渲染状态（legacy-1222-l1-render）。
 *
 * 持默认模型的 GeoModel + 每骨动画参数面（12 float/骨契约，
 * NativeModelRenderer.calculateBoneMatrix:328 消费侧同款布局）。
 * L1 单默认模型全玩家同模（PlayerCapability/网络同步=L2）。
 */
public final class LegacyModelState {

    private static ClientModelInfo bundle;
    private static GeoModel mainModel;
    private static float[] boneParams;
    private static float[] currentBoneParams;
    private static long animTick;
    private static ResourceLocation texture;

    private LegacyModelState() {
    }

    public static void setBundle(ClientModelInfo info) {
        bundle = info;
        MainModelData data = info == null ? null : info.getMainModelData();
        mainModel = data == null || data.getModels().isEmpty() ? null : data.getModels().get(0);
        boneParams = mainModel == null || mainModel.bakedBones == null
                ? null : new float[mainModel.bakedBones.size() * 12];
        currentBoneParams = boneParams;
        // L1 纹理面：默认贴图（MissingTexture 兜底由 GL 无绑定时的白面承接）
        texture = new ResourceLocation("yes_steve_model", "textures/entity/default.png");
        YesSteveModel.LOGGER.info("[ysm-legacy122] state set: model={} bones={}",
                mainModel != null, mainModel == null || mainModel.bakedBones == null
                        ? -1 : mainModel.bakedBones.size());
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
