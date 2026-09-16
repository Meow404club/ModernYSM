package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
// 1.21.5 GlStateManager 迁移 platform→opengl 包
//? if <21.5
import com.mojang.blaze3d.platform.GlStateManager;
// 1.21.5 GlStateManager 迁移 platform→opengl 包（vcs 直通铁律：非 1.20.1 分支源码态必须注释）
//? if >=21.5
/*import com.mojang.blaze3d.opengl.GlStateManager;*/
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class GpuMesh {
    public final long pointer;
    public final int vao;
    public final int vbo;
    public final int ibo;
    public final int boneSsbo;
    public final int vertexCount;
    public final int indexCount;
    public final int boneCount;
    public final int partMask1Start, partMask1Count;
    public final int partMask2Start, partMask2Count;
    public final int partMask3Start, partMask3Count;
    public final ByteBuffer perFrameBoneBuffer;

    private int xformVbo = 0;
    private int xformVao = 0;
    // Iris/Oculus 扩展顶点数据（构建期一次性填好，首次 ensureIrisBuffers 上传后释放）
    private ByteBuffer irisExtraData = null;
    private int irisVbo = 0;
    private int irisVao = 0;
    private boolean disposed = false;

    GpuMesh(long pointer, int vao, int vbo, int ibo, int boneSsbo, int vertexCount, int indexCount, int boneCount, int pm1s, int pm1c, int pm2s, int pm2c, int pm3s, int pm3c) {
        this.pointer = pointer;
        this.vao = vao;
        this.vbo = vbo;
        this.ibo = ibo;
        this.boneSsbo = boneSsbo;
        this.vertexCount = vertexCount;
        this.indexCount = indexCount;
        this.boneCount = boneCount;
        this.partMask1Start = pm1s;
        this.partMask1Count = pm1c;
        this.partMask2Start = pm2s;
        this.partMask2Count = pm2c;
        this.partMask3Start = pm3s;
        this.partMask3Count = pm3c;
        this.perFrameBoneBuffer = MemoryUtil.memAlloc(boneCount * 144);
    }

    public int indexOffsetBytes(int renderPartMask) {
        if (renderPartMask == 0 || renderPartMask == 3) return 0;
        if (renderPartMask == 1) return partMask1Start * Integer.BYTES;
        if (renderPartMask == 2) return partMask2Start * Integer.BYTES;
        return 0;
    }

    public int indexDrawCount(int renderPartMask) {
        if (renderPartMask == 0) return indexCount;
        if (renderPartMask == 3) return indexCount;
        int self = (renderPartMask == 1) ? partMask1Count : (renderPartMask == 2) ? partMask2Count : 0;
        return self + partMask3Count;
    }

    public int xformVbo() {
        return xformVbo;
    }

    public int xformVao() {
        return xformVao;
    }

    public int irisVao() {
        return irisVao;
    }

    /**
     * 构建期由 GpuMeshBuilder 注入的 per-vertex 扩展数据（20B/顶点：
     * midU f32 | midV f32 | tangent packed i32 | entity u16x3 全 0 | pad u16）。
     * 顶点序与 vbo 一一对应（native nBuildGpuMesh 只重排 index 不重排顶点，每 quad 4 顶点连续）。
     */
    void attachIrisExtra(ByteBuffer data) {
        this.irisExtraData = data;
    }

    /**
     * Iris 兼容扩展 VAO：属性 0-5 复用 compute 输出 xformVbo（36B，vanilla NEW_ENTITY 语义），
     * 属性 7/8/9 从 irisExtraData 读——对齐 IrisVertexFormats.ENTITY 的属性布局
     * （tmp/harvest/oculus-1201-src IrisVertexFormats.java:47-57）：
     *   7 = iris_Entity  (ENTITY_ID_ELEMENT: USHORT x3, Usage.UV -> IPointer，EntityPatcher.java:157 `in ivec3`)
     *   8 = mc_midTexCoord (MID_TEXTURE_ELEMENT: FLOAT x2, Usage.GENERIC -> Pointer normalized=false)
     *   9 = at_tangent   (TANGENT_ELEMENT: BYTE x4, Usage.GENERIC -> Pointer normalized=false，
     *                     同 vanilla 1.20.1 VertexFormatElement.Usage.GENERIC :147-150)
     * location 序号 = VertexFormat 元素序号（vanilla 1.20.1 ShaderInstance.java:171-175），
     * 6/10 = PADDING usage 不启用（vanilla VertexFormatElement.java:144-146 空 setup）。
     * 无包路径（GpuRenderPath）继续用 xformVao，不受影响。
     */
    public void ensureIrisBuffers() {
        if (irisVao != 0 || irisExtraData == null) return;
        irisVbo = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, irisVbo);
        GL45.glBufferData(GL15.GL_ARRAY_BUFFER, irisExtraData, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(irisExtraData);
        irisExtraData = null;
        irisVao = GL45.glGenVertexArrays();
        GL45.glBindVertexArray(irisVao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, xformVbo);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 36, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, 36, 12L);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL15.GL_FLOAT, false, 36, 16L);
        GL20.glEnableVertexAttribArray(3);
        GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, 36, 24L);
        GL20.glEnableVertexAttribArray(4);
        GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, 36, 28L);
        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, 36, 32L);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, irisVbo);
        GL20.glEnableVertexAttribArray(8);
        GL20.glVertexAttribPointer(8, 2, GL15.GL_FLOAT, false, 20, 0L);
        GL20.glEnableVertexAttribArray(9);
        GL20.glVertexAttribPointer(9, 4, GL11.GL_BYTE, false, 20, 8L);
        GL20.glEnableVertexAttribArray(7);
        GL30.glVertexAttribIPointer(7, 3, GL11.GL_UNSIGNED_SHORT, 20, 12L);
        GL45.glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void ensureXformBuffers() {
        if (xformVao != 0) return;
        xformVbo = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, xformVbo);
        GL45.glBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexCount * 36, GL15.GL_DYNAMIC_DRAW);
        xformVao = GL45.glGenVertexArrays();
        GL45.glBindVertexArray(xformVao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, xformVbo);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 36, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, 36, 12L);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL15.GL_FLOAT, false, 36, 16L);
        GL20.glEnableVertexAttribArray(3);
        GL30.glVertexAttribIPointer(3, 2, GL11.GL_SHORT, 36, 24L);
        GL20.glEnableVertexAttribArray(4);
        GL30.glVertexAttribIPointer(4, 2, GL11.GL_SHORT, 36, 28L);
        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 3, GL11.GL_BYTE, true, 36, 32L);
        GL45.glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        GlStateManager._glDeleteBuffers(vbo);
        GlStateManager._glDeleteBuffers(ibo);
        GlStateManager._glDeleteBuffers(boneSsbo);
        GL45.glDeleteVertexArrays(vao);
        if (xformVbo != 0) GlStateManager._glDeleteBuffers(xformVbo);
        if (xformVao != 0) GL45.glDeleteVertexArrays(xformVao);
        if (irisVbo != 0) GlStateManager._glDeleteBuffers(irisVbo);
        if (irisVao != 0) GL45.glDeleteVertexArrays(irisVao);
        if (irisExtraData != null) MemoryUtil.memFree(irisExtraData);
        if (pointer != 0) {
            GeoModel.nFreeGpuMesh(pointer);
        }
        MemoryUtil.memFree(perFrameBoneBuffer);
    }
}
