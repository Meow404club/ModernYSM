package rip.ysm.legacy1710;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7.10 L1 烘焙几何契约（legacy-1710-l1-render）。
 *
 * 与共享 GeoModel.BakedBone/BakedQuad（geckolib3 geo/render/built，12 float
 * positions/8 uv/3 normal + pivot 1/16 块 + 12 float/骨动画面）逐字段同构——
 * 但共享 GeoModel 绑定 modern BufferBuilder/ASM/YesSteveModel 面（GeoModel.java
 * :11-25 import 实证），1.7.10 不挂共享源白名单（1.12.2 全量分代 7409 错先例），
 * 故 twin 最小自有结构。.ysm 文件解析链 1.12.2 也未通（NativeLibLoader/
 * GeckoLibCache 闭包锁死），L1 走程序化人形——文件装载 = L2 接入时再对齐契约。
 */
public final class LegacyBakedModel {

    public final List<BakedBone> bones = new ArrayList<>();

    // 主贴图（textureMap 首个，恒绑 index 0）半透明扫描结果——GeoModel.isTranslucentTexture(0)
    // 同源（TranslucencyScanner），LegacyGeoModelConverter 装载
    public boolean translucent;

    public static class BakedBone {
        public String name;
        // ysmGlow 前缀骨（GeoBone GLOWING_PREFIX 同语义，主线 NativeModelRenderer:291 全亮）
        public boolean glow;
        public int parentIdx = -1;
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public final List<BakedCube> cubes = new ArrayList<>();
    }

    public static class BakedCube {
        public final List<BakedQuad> quads = new ArrayList<>();
    }

    public static class BakedQuad {
        public final float[] positions = new float[12];
        public final float[] uvs = new float[8];
        public final float[] normal = new float[3];
    }
}
