package rip.ysm.yui;

/**
 * 实体预览回调槽（模型预览挂点）。M-U1 仅定签名：实现方=版本树消费侧
 * （yui 树不 import 模型系统，编译环归零），真预览驱动等 M-U1R 研究卡
 * （1.12.2 固定管线 POC：GuiInventory.drawEntityOnScreen 等价性+GL 状态恢复清单）
 * 定稿后落各版本树。
 */
public interface YuiPreview {

    /**
     * 在给定矩形（GUI 逻辑坐标）内渲染实体预览。实现自行负责深度/光照等
     * GL 状态的保存恢复；矩形裁剪语义由 YuiBackend.preview 实现方保证。
     */
    void render(int x1, int y1, int x2, int y2, float mouseX, float mouseY, float partialTick);
}
