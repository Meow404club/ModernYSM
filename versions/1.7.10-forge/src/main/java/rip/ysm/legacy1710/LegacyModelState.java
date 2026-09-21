package rip.ysm.legacy1710;

import net.minecraft.util.ResourceLocation;

/**
 * 1.7.10 线渲染状态（legacy-1710-l1-render）。
 *
 * 1.12.2 LegacyModelState 的 1.7.10 twin：持默认模型烘焙几何 + 每骨动画参数面
 * （12 float/骨契约）。L1 单默认模型全玩家同模（网络同步本卡不做）。
 */
public final class LegacyModelState {

    private static LegacyBakedModel mainModel;
    private static float[] boneParams;
    private static float[] currentBoneParams;
    private static long animTick;
    private static ResourceLocation texture;

    private LegacyModelState() {
    }

    public static void setModel(LegacyBakedModel model, ResourceLocation tex) {
        mainModel = model;
        texture = tex;
        if (model == null) {
            boneParams = null;
            currentBoneParams = null;
            return;
        }
        boneParams = new float[model.bones.size() * 12];
        currentBoneParams = new float[model.bones.size() * 12];
    }

    public static LegacyBakedModel mainModel() {
        return mainModel;
    }

    public static int boneCount() {
        return mainModel == null ? 0 : mainModel.bones.size();
    }

    public static String boneName(int idx) {
        return mainModel == null || idx >= mainModel.bones.size() ? null : mainModel.bones.get(idx).name;
    }

    public static float[] boneParams(LegacyBakedModel model) {
        return model != null && model == mainModel ? boneParams : null;
    }

    public static float[] currentBoneParams() {
        return currentBoneParams;
    }

    public static long nextAnimTick() {
        return ++animTick;
    }

    public static ResourceLocation texture() {
        return texture;
    }
}
