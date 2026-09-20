package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.util.ResourceLocation;

import java.util.logging.Logger;

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
        mainModel = model;
        boneParams = mainModel == null || mainModel.bakedBones == null
                ? null : new float[mainModel.bakedBones.size() * 12];
        currentBoneParams = boneParams;
        // L2 纹理面：真实贴图优先（OuterFileTexture 直 bind），null 回退占位皮肤
        boundTexture = realTexture instanceof com.elfmcys.yesstevemodel.client.texture.OuterFileTexture
                ? (com.elfmcys.yesstevemodel.client.texture.OuterFileTexture) realTexture : null;
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

    public static com.elfmcys.yesstevemodel.client.texture.OuterFileTexture realTexture() {
        return boundTexture;
    }

    private static com.elfmcys.yesstevemodel.client.texture.OuterFileTexture boundTexture;

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
