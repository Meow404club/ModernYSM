package rip.ysm.legacy1710;

import java.util.List;

/**
 * L1 程序化盒状人形（legacy-1710-l1-render）。
 *
 * 1.12.2 LegacyTestModel 的 1.7.10 twin：.ysm 文件装载链（YSMFolderDeserializer
 * 传递闭包）1.12.2 亦未通，本卡不接——程序化构建 Bedrock 人形骨架
 * （head/body/arms/legs）的烘焙几何，契约与共享 GeoModel.BakedBone 同构，
 * 走同一翻译层/骨矩阵/动画驱动链——渲染+动画链全真，文件解析=L2。
 *
 * 骨架对齐 vanilla ModelBiped 命名（LegacyAnimationDriver 名匹配面），
 * pivot 单位 1/16 块（0.0625 消费同款）。
 */
public final class LegacyTestModel {

    private LegacyTestModel() {
    }

    public static LegacyBakedModel build() {
        LegacyBakedModel model = new LegacyBakedModel();
        List<LegacyBakedModel.BakedBone> bones = model.bones;

        // 树序：body(0) → head(1), leftArm(2), rightArm(3), leftLeg(4), rightLeg(5)
        bones.add(bone("body", -1, 0, 12, 0));          // 躯干 pivot 腰部
        bones.add(bone("head", 0, 0, 24, 0));           // 头 pivot 颈
        bones.add(bone("leftArm", 0, 5, 22, 0));        // 左臂 pivot 肩
        bones.add(bone("rightArm", 0, -5, 22, 0));      // 右臂 pivot 肩
        bones.add(bone("leftLeg", 0, 2, 12, 0));        // 左腿 pivot 髋
        bones.add(bone("rightLeg", 0, -2, 12, 0));      // 右腿 pivot 髋

        // 盒子：body 8x12x4 / head 8x8x8 / arm 4x12x4 / leg 4x12x4（vanilla 人形比例）
        addBox(bones.get(0), -4, 0, -2, 8, 12, 4);
        addBox(bones.get(1), -4, 0, -4, 8, 8, 8);
        addBox(bones.get(2), 0, -12, -2, 4, 12, 4);
        addBox(bones.get(3), -4, -12, -2, 4, 12, 4);
        addBox(bones.get(4), -2, -12, -2, 4, 12, 4);
        addBox(bones.get(5), -2, -12, -2, 4, 12, 4);

        return model;
    }

    private static LegacyBakedModel.BakedBone bone(String name, int parentIdx, float pivotX, float pivotY, float pivotZ) {
        LegacyBakedModel.BakedBone b = new LegacyBakedModel.BakedBone();
        b.name = name;
        b.parentIdx = parentIdx;
        b.pivotX = pivotX;
        b.pivotY = pivotY;
        b.pivotZ = pivotZ;
        return b;
    }

    // 轴对齐盒 6 面 24 顶点（契约：positions 12/uv 8/normal 3）
    private static void addBox(LegacyBakedModel.BakedBone bone, float x, float y, float z, float w, float h, float d) {
        LegacyBakedModel.BakedCube cube = new LegacyBakedModel.BakedCube();
        float x1 = x, y1 = y, z1 = z, x2 = x + w, y2 = y + h, z2 = z + d;

        // -Z 北 / +Z 南 / -X 西 / +X 东 / -Y 下 / +Y 上
        quad(cube, z1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, 0, 0, -1);
        quad(cube, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 0, 0, 1);
        quad(cube, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, -1, 0, 0);
        quad(cube, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, 1, 0, 0);
        quad(cube, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, -1, 0);
        quad(cube, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, 1, 0);

        bone.cubes.add(cube);
    }

    private static void quad(LegacyBakedModel.BakedCube cube,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        LegacyBakedModel.BakedQuad q = new LegacyBakedModel.BakedQuad();
        q.positions[0] = ax; q.positions[1] = ay; q.positions[2] = az;
        q.positions[3] = bx; q.positions[4] = by; q.positions[5] = bz;
        q.positions[6] = cx; q.positions[7] = cy; q.positions[8] = cz;
        q.positions[9] = dx; q.positions[10] = dy; q.positions[11] = dz;
        for (int v = 0; v < 4; v++) {
            q.uvs[v * 2] = v == 0 || v == 3 ? 0.0f : 1.0f;
            q.uvs[v * 2 + 1] = v < 2 ? 0.0f : 1.0f;
        }
        q.normal[0] = nx; q.normal[1] = ny; q.normal[2] = nz;
        cube.quads.add(q);
    }
}
