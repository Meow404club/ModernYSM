package rip.ysm.legacy1710.yui1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjglx.opengl.GL11;

import rip.ysm.legacy1710.LegacyAnimationSampler;
import rip.ysm.legacy1710.LegacyBakedModel;
import rip.ysm.legacy1710.LegacyModelRegistry;
import rip.ysm.legacy1710.LegacyModelState;
import rip.ysm.legacy1710.LegacyModelTranslator;
import rip.ysm.yui.YuiPreview;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI 内真实模型预览驱动（legacy1710-l2a-model-load）。
 *
 * 1.12.2 LegacyCardPreview（rip.ysm.legacy122.yui）卡内预览路径的 1.7.10 最小面：
 * 按 bundle properties.preview_animation 播放（主线 setCurrentAnimation 同语义），
 * {@link LegacyAnimationSampler} 逐骨采样写 boneParams；空串/查不到动画=绑定位
 * 静像（主线 STOP 同语义，无 idle 回退）。时间轴按 动画名 跨帧持久（刷新/重布局
 * 不重置——主线 m2.6.1 教训）。hover/focus 三态与 height_scale 之外的姿态细节随
 * L3a 收编。GL 序列=vanilla-mc-1710 GuiInventory.drawEntityOnScreen:64-97 直调
 * 形态（1710 无 GlStateManager，lwjglx 面），RenderManager→翻译层直调。
 */
public final class LegacyPreview1710 implements YuiPreview {

    /** 卡内预览定尺（主线 ModelButton.java:287 第三参 30）。 */
    private static final float CARD_SCALE = 30.0F;
    /** height_scale 解析缺省（RawYsmModel.RawProperties 默认 0.7f）。 */
    private static final float DEFAULT_HEIGHT_SCALE = 0.7F;

    /** 各动画名独立时间轴（键=modelId|anim；同名动画跨刷新持续，不重置时间轴）。 */
    private static final Map<String, Long> STARTS = new ConcurrentHashMap<String, Long>();

    // 采证打点开关（translator 同款 -Dysm.legacy1710.debug）
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy1710.debug");

    private static long debugFrame;

    private final String modelId;

    private LegacyPreview1710(String modelId) {
        this.modelId = modelId;
    }

    /** 指定模型 id 的卡内预览（null=default 主面）。 */
    public static LegacyPreview1710 of(String modelId) {
        return new LegacyPreview1710(modelId);
    }

    @Override
    public void render(int x1, int y1, int x2, int y2, float mouseX, float mouseY, float partialTick) {
        String id = this.modelId == null ? LegacyModelRegistry.DEFAULT_MODEL_ID : this.modelId;
        LegacyBakedModel model = LegacyModelState.modelOf(id);
        float[] params = LegacyModelState.paramsOf(id, model);
        if (model == null || params == null || model.bones.isEmpty()) {
            return;
        }
        ClientModelInfo bundle = LegacyModelState.bundleOf(id);
        ModelProperties props = bundle == null ? null : bundle.getInfo().getModelProperties();
        float heightScale = props == null ? DEFAULT_HEIGHT_SCALE : props.getHeightScale();
        boolean disableRot = props != null && props.isDisablePreviewRotation();
        String baseAnim = props == null ? "" : props.getPreviewAnimation();

        // --- 姿态/缩放/锚点（主线 ModelButton:287 + ModelPreviewRenderer:773/846 语义）---
        float scale = CARD_SCALE * heightScale;
        // disable_preview_rotation=true（builtin 全系）→ previewYaw=180/pitch 0/下移 5.5px，
        // 否则 200/-10/0（122 LegacyCardPreview 同式换算）
        float yawRot = disableRot ? -180.0F : -200.0F;
        float pitchRot = disableRot ? 0.0F : -10.0F;
        float anchorX = x1 + (x2 - x1) / 2.0F;
        float anchorY = y1 + (y2 - y1) - 5.0F;
        if (disableRot) {
            anchorY += 5.5F;
        }

        // --- 预览动画（语义A）：空=绑定位静像；时间轴跨帧持久 ---
        String timelineKey = id + "|" + baseAnim;
        Long start = STARTS.get(timelineKey);
        if (start == null) {
            start = Long.valueOf(System.currentTimeMillis());
            STARTS.put(timelineKey, start);
        }
        float tick = (System.currentTimeMillis() - start.longValue()) / 50.0F;
        boolean animated = LegacyAnimationSampler.sample(model, params, bundle, baseAnim, tick);
        if (DEBUG && (debugFrame++ % 40 == 0)) {
            System.out.printf(
                    "[ysm-legacy1710] preview frame=%d id=%s anim=%s playing=%b tick=%.1f bones=%d%n",
                    debugFrame, id, baseAnim, animated, tick, model.bones.size());
        }

        // --- 纹理双路（LegacyRenderHook 同款：OuterFileTexture 直绑 / RL 回退）---
        OuterFileTexture tex = LegacyModelState.textureOf(id);
        if (tex != null) {
            tex.ensureUploaded();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlTextureId());
        } else {
            ResourceLocation rl = LegacyModelState.texture();
            if (rl == null) {
                return;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        }

        // === drawEntityOnScreen 可照抄段（vanilla-mc-1710 GuiInventory.java:64-97，
        //     常量走字面量=vanilla 同形；lwjglx 面无 GL_RESCALE_NORMAL 常量）===
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);              // :64 (2903)
        GL11.glPushMatrix();                                // :65
        GL11.glTranslatef(anchorX, anchorY, 50.0F);         // :66（GUI ortho 基面 +50）
        GL11.glScalef(-scale, scale, scale);                // :67（GUI y 向下而模型 Y-up）
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);           // :69
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);           // :75
        RenderHelper.enableStandardItemLighting();          // :76
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);          // :77
        GL11.glRotatef(pitchRot, 1.0F, 0.0F, 0.0F);         // 俯仰
        GL11.glRotatef(yawRot, 0.0F, 1.0F, 0.0F);           // yBodyRot=previewYaw 等效

        // RenderManager 不可用（L2a 无实体渲染接管）——翻译层直调（动画已采样进 params）
        LegacyModelTranslator.render(model, params, 1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();                                 // :95
        RenderHelper.disableStandardItemLighting();         // :96
        GL11.glDisable(32826);                              // :97 GL_RESCALE_NORMAL（防御性配平）
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit); // :98-100 lightmap 尾三连
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
