package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
// 1.21.5 GlStateManager 迁移 platform→opengl 包
//? if <21.5
import com.mojang.blaze3d.platform.GlStateManager;
// 1.21.5 GlStateManager 迁移 platform→opengl 包（vcs 直通铁律：非 1.20.1 分支源码态必须注释）
//? if >=21.5
/*import com.mojang.blaze3d.opengl.GlStateManager;*/
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class GpuMeshBuilder {
    public static GpuMesh build(GeoModel model) {
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return null;
        //? if >1.17 {
        //? if <1.18
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.18
        RenderSystem.assertOnRenderThread();
        //?}
        ByteBuffer modelBuf = serializeModel(model);
        int[] meta = new int[9];
        long handle = GeoModel.nBuildGpuMesh(modelBuf, meta);
        if (handle == 0) {
            return null;
        }
        int vertexCount = meta[0];
        int indexCount = meta[1];
        int boneCount = meta[2];

        ByteBuffer vbuf = GeoModel.nGetGpuMeshVertexBuffer(handle);
        ByteBuffer ibuf = GeoModel.nGetGpuMeshIndexBuffer(handle);
        if (vbuf == null || ibuf == null) {
            GeoModel.nFreeGpuMesh(handle);
            return null;
        }
        vbuf.order(ByteOrder.nativeOrder());
        ibuf.order(ByteOrder.nativeOrder());

        int vao = GL30.glGenVertexArrays();
        int vbo = GlStateManager._glGenBuffers();
        int ibo = GlStateManager._glGenBuffers();
        int ssbo = GlStateManager._glGenBuffers();

        GL30.glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vbuf, GL15.GL_STATIC_DRAW);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ibuf, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 32, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL15.GL_FLOAT, false, 32, 12L);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL33.GL_INT_2_10_10_10_REV, true, 32, 20L);
        GL20.glEnableVertexAttribArray(3);
        GL30.glVertexAttribIPointer(3, 1, GL15.GL_UNSIGNED_SHORT, 32, 24L);

        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 1, GL11.GL_UNSIGNED_BYTE, false, 32, 27L);

        GL30.glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL45.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) boneCount * 144, GL15.GL_DYNAMIC_DRAW);
        GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        GeoModel.nReleaseGpuMeshScratch(handle);

        GpuMesh mesh = new GpuMesh(handle, vao, vbo, ibo, ssbo, vertexCount, indexCount, boneCount, meta[3], meta[4], meta[5], meta[6], meta[7], meta[8]);
        mesh.attachIrisExtra(serializeIrisExtra(model));
        return mesh;
    }

    private static ByteBuffer serializeModel(GeoModel model) {
        int totalBones = model.bakedBones.size();
        int totalCubes = 0;
        int totalQuads = 0;
        for (GeoModel.BakedBone bone : model.bakedBones) {
            totalCubes += bone.cubes.size();
            for (GeoModel.BakedCube cube : bone.cubes) {
                totalQuads += cube.quads.size();
            }
        }
        int sz = 4 + (totalBones * 25) + (totalCubes * 5) + (totalQuads * 92);
        ByteBuffer buf = ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder());
        buf.putInt(totalBones);
        for (GeoModel.BakedBone bone : model.bakedBones) {
            buf.putInt(bone.parentIdx);
            buf.putInt(bone.partMask);
            buf.put((byte) (bone.glow ? 1 : 0));
            buf.putFloat(bone.pivotX);
            buf.putFloat(bone.pivotY);
            buf.putFloat(bone.pivotZ);
            buf.putInt(bone.cubes.size());
            for (GeoModel.BakedCube cube : bone.cubes) {
                buf.put((byte) (cube.cullable ? 1 : 0));
                buf.putInt(cube.quads.size());
                for (GeoModel.BakedQuad quad : cube.quads) {
                    for (float position : quad.positions) {
                        buf.putFloat(position);
                    }
                    for (float uv : quad.uvs) {
                        buf.putFloat(uv);
                    }
                    buf.putFloat(quad.normal[0]);
                    buf.putFloat(quad.normal[1]);
                    buf.putFloat(quad.normal[2]);
                }
            }
        }
        buf.position(0);
        return buf;
    }

    /**
     * Iris 扩展属性数据（per-vertex 20B：midU | midV | tangentPacked | entity u16x3=0 | pad）。
     * 填充惯例照抄 Iris 对 vanilla 顶点的做法（tmp/harvest/oculus-1201-src
     * MixinBufferBuilder.java:133-240 fillExtendedData）：
     *   midTexCoord = quad 四顶点 uv 均值（:189-198）；
     *   tangent = NormalHelper.computeTangent(面法线, quad 顶点0/1/2)（NormalHelper.java:362-427），
     *     NormI8.pack 量化（NormI8.java:44-46，w=±127 表 handedness）；
     *   iris_Entity = 0/0/0（非实体上下文，SPEC 约定，同 :150-152 的 int 0 语义）。
     * 遍历顺序与 serializeModel 严格一致（bone→cube→quad，每 quad 4 顶点连续 = vbo 顶点序）。
     */
    private static ByteBuffer serializeIrisExtra(GeoModel model) {
        int totalQuads = 0;
        for (GeoModel.BakedBone bone : model.bakedBones) {
            for (GeoModel.BakedCube cube : bone.cubes) {
                totalQuads += cube.quads.size();
            }
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(totalQuads * 4 * 20).order(ByteOrder.nativeOrder());
        for (GeoModel.BakedBone bone : model.bakedBones) {
            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    float[] p = quad.positions;
                    float[] uv = quad.uvs;
                    float midU = (uv[0] + uv[2] + uv[4] + uv[6]) * 0.25f;
                    float midV = (uv[1] + uv[3] + uv[5] + uv[7]) * 0.25f;
                    float[] n = quad.normal;
                    int packedTangent = computeTangent(n[0], n[1], n[2],
                            p[0], p[1], p[2], uv[0], uv[1],
                            p[3], p[4], p[5], uv[2], uv[3],
                            p[6], p[7], p[8], uv[4], uv[5]);
                    for (int v = 0; v < 4; v++) {
                        buf.putFloat(midU);
                        buf.putFloat(midV);
                        buf.putInt(packedTangent);
                        buf.putShort((short) 0);
                        buf.putShort((short) 0);
                        buf.putShort((short) 0);
                        buf.putShort((short) 0);
                    }
                }
            }
        }
        buf.position(0);
        return buf;
    }

    // 复刻 Iris NormalHelper.computeTangent（NormalHelper.java:362-427），含 rsqrt(0)=1 防护
    private static int computeTangent(float normalX, float normalY, float normalZ,
                                      float x0, float y0, float z0, float u0, float v0,
                                      float x1, float y1, float z1, float u1, float v1,
                                      float x2, float y2, float z2, float u2, float v2) {
        float edge1x = x1 - x0, edge1y = y1 - y0, edge1z = z1 - z0;
        float edge2x = x2 - x0, edge2y = y2 - y0, edge2z = z2 - z0;
        float deltaU1 = u1 - u0, deltaV1 = v1 - v0;
        float deltaU2 = u2 - u0, deltaV2 = v2 - v0;
        float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
        float f = (fdenom == 0.0f) ? 1.0f : 1.0f / fdenom;
        float tx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
        float ty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
        float tc = rsqrt(tx * tx + ty * ty + tz * tz);
        tx *= tc; ty *= tc; tz *= tc;
        float bx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float by = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
        float bc = rsqrt(bx * bx + by * by + bz * bz);
        bx *= bc; by *= bc; bz *= bc;
        float pbx = ty * normalZ - tz * normalY;
        float pby = tz * normalX - tx * normalZ;
        float pbz = tx * normalY - ty * normalX;
        float dot = bx * pbx + by * pby + bz * pbz;
        float w = (dot < 0) ? -1.0f : 1.0f;
        return NormI8.pack(tx, ty, tz, w);
    }

    private static float rsqrt(float value) {
        return (value == 0.0f) ? 1.0f : (float) (1.0 / Math.sqrt(value));
    }

    // 复刻 Iris NormI8.pack（NormI8.java:44-46）
    private static final class NormI8 {
        static int pack(float x, float y, float z, float w) {
            return ((int) (x * 127) & 0xFF)
                    | (((int) (y * 127) & 0xFF) << 8)
                    | (((int) (z * 127) & 0xFF) << 16)
                    | (((int) (w * 127) & 0xFF) << 24);
        }
    }
}
