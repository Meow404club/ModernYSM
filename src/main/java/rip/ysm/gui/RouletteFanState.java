package rip.ysm.gui;

//? if >=21.6 {
/*import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;

// 1.21.6+ GUI 扇形绘制元素。drawSpecial 于 1.21.6 删除后的正道：
// GuiElementRenderState + GuiGraphics.submitGuiElementRenderState（1.21.8 GuiGraphics.java:1282），
// GuiRenderer 按 pipeline() 的顶点格式通用渲染（GuiRenderer.java:665 getBufferBuilder），
// 任意 RenderPipeline 无需注册。收集 POSITION_COLOR 顶点，屏幕坐标直接提交。
// 铁律：存储态块内（含注释行）禁出现块注释关闭符字面序列与 javadoc（stitcher 会误判/放弃解析，21.8 生成树实证）。
public final class RouletteFanState implements GuiElementRenderState {
    private final List<float[]> vertices = new ArrayList<>();
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;

    public VertexConsumer sink() {
        return new Sink();
    }

    // 1.21.10 buildVertices(VertexConsumer,float z) → buildVertices(VertexConsumer)（z 由
    // GuiRenderer 控制面承接，neoforge-21.10.64 GuiElementRenderState.java:16 实证）
    // 1.21.10 buildVertices(VertexConsumer,float z) → buildVertices(VertexConsumer)（z 由
    // GuiRenderer 控制面承接，neoforge-21.10.64 GuiElementRenderState.java:16 实证）。
    // 本类整体为 >=21.6 存储态（铁律：存储态块内禁字面块注释符）→ 内层分代一律用嵌套块+活性文本
    //? if >=21.9 {
    @Override
    public void buildVertices(VertexConsumer vc) {
        this.buildVerticesImpl(vc, 0.0f);
    }
    //?}
    //? if <21.9 {
    @Override
    public void buildVertices(VertexConsumer vc, float z) {
        this.buildVerticesImpl(vc, z);
    }
    //?}
    //? if >=21.9 {
    private void buildVerticesImpl(VertexConsumer vc, float z) {
        for (float[] v : this.vertices) {
            vc.addVertex(v[0], v[1], z).setColor(v[2], v[3], v[4], v[5]);
        }
    }
    //?}
    //? if <21.9 {
    private void buildVerticesImpl(VertexConsumer vc, float z) {
        for (float[] v : this.vertices) {
            vc.addVertex(v[0], v[1], z).setColor(v[2], v[3], v[4], v[5]);
        }
    }
    //?}

    @Override
    public ScreenRectangle bounds() {
        if (this.vertices.isEmpty()) {
            return null;
        }
        int x0i = (int) Math.floor(this.minX);
        int y0i = (int) Math.floor(this.minY);
        return new ScreenRectangle(x0i, y0i,
            (int) Math.ceil(this.maxX) - x0i, (int) Math.ceil(this.maxY) - y0i);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return null;
    }

    private final class Sink implements VertexConsumer {
        // 1.21.11 VertexConsumer 新增抽象 setLineWidth(float)（2111 VertexConsumer 实证）
        //? if >=21.11 {
        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }
        //?}
        private float[] cur;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.cur = new float[6];
            this.cur[0] = x;
            this.cur[1] = y;
            RouletteFanState.this.vertices.add(this.cur);
            RouletteFanState.this.minX = Math.min(RouletteFanState.this.minX, x);
            RouletteFanState.this.minY = Math.min(RouletteFanState.this.minY, y);
            RouletteFanState.this.maxX = Math.max(RouletteFanState.this.maxX, x);
            RouletteFanState.this.maxY = Math.max(RouletteFanState.this.maxY, y);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.cur[2] = r / 255.0f;
            this.cur[3] = g / 255.0f;
            this.cur[4] = b / 255.0f;
            this.cur[5] = a / 255.0f;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}*/
//?}
