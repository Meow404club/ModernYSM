package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

/**
 * GeoModel→LegacyBakedModel 机械映射（legacy1710-l2a-model-load）。
 *
 * 共享 GeoModel.BakedBone/BakedQuad（GeoModel.java:111-133）与 LegacyBakedModel
 * 逐字段同构（positions12/uv8/normal3 float 契约），BakedQuad 多一个 isTranslucent
 *（固定管线翻译层不消费，丢弃）。parentIdx 是 bakedBones 列表下标——转换保序，
 * 下标语义不变。glow/partMask/rotX-Y-Z 等翻译层不消费的字段一并丢弃。
 */
public final class LegacyGeoModelConverter {

    private LegacyGeoModelConverter() {
    }

    public static LegacyBakedModel convert(GeoModel geo) {
        LegacyBakedModel out = new LegacyBakedModel();
        if (geo == null || geo.bakedBones == null) {
            return out;
        }
        for (GeoModel.BakedBone src : geo.bakedBones) {
            LegacyBakedModel.BakedBone bone = new LegacyBakedModel.BakedBone();
            bone.name = src.name;
            bone.parentIdx = src.parentIdx;
            bone.pivotX = src.pivotX;
            bone.pivotY = src.pivotY;
            bone.pivotZ = src.pivotZ;
            for (GeoModel.BakedCube srcCube : src.cubes) {
                LegacyBakedModel.BakedCube cube = new LegacyBakedModel.BakedCube();
                for (GeoModel.BakedQuad srcQuad : srcCube.quads) {
                    LegacyBakedModel.BakedQuad quad = new LegacyBakedModel.BakedQuad();
                    System.arraycopy(srcQuad.positions, 0, quad.positions, 0, 12);
                    System.arraycopy(srcQuad.uvs, 0, quad.uvs, 0, 8);
                    System.arraycopy(srcQuad.normal, 0, quad.normal, 0, 3);
                    cube.quads.add(quad);
                }
                bone.cubes.add(cube);
            }
            out.bones.add(bone);
        }
        return out;
    }
}
