package rip.ysm.legacy1710.yui1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
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
 * GUI 内真实模型预览驱动（legacy1710-l3b-preview：122 LegacyCardPreview 语义完整适形）。
 *
 * <p>两种消费形态（rip.ysm.legacy122.yui.LegacyCardPreview 逐项对齐）：
 * <ul>
 * <li>{@link #card(String)}——卡内预览。固定 scale 30×height_scale（ModelButton.java:287
 *     定值 30 × properties heightScale 链，缺省 0.7）+ 模型原点锚（锚=卡 x+w/2,y+65=
 *     ModelButton :287 x+w/2,y+h/2+20）；disable_preview_rotation=true（builtin
 *     default/wine_fox 01 系）→ previewYaw=180/pitch 0/下移 5.5px
 *     （ModelPreviewRenderer.java:773/785/846-868），false（wine_fox/12_little）→
 *     200/-10/0。超槽模型靠 scissor 裁切=主线同款观感（无 AABB 自适应）。</li>
 * <li>{@link #pane()}——左栏大预览（消费者=L3a 模型选择屏）：固定 70×height_scale
 *     （主线 PlayerModelScreen.java:836 size=70）+原点锚（guiLeft+67, guiTop+190=
 *     槽 x1+62/y1+161）+ 头/身随鼠标（vanilla-mc-1710 GuiInventory.java:60/77-80
 *     atan/40 公式，122 面同款单通道）。画当前选中模型（registry UUID 读侧）——
 *     RenderManager 不可用，装备/手持不随行=两线共同不可消除项。</li>
 * </ul>
 *
 * <p>预览动画（语义A）：按模型 properties.preview_animation 播放（主线
 * PlayerModelScreen.java:531 setCurrentAnimation 同语义），{@link LegacyAnimationSampler}
 * 逐骨采样写 boneParams；空串/查不到动画=绑定位静像（主线 STOP 同语义，无 idle
 * 回退）。时间轴按 动画名 跨帧持久：增量刷新/翻页/滚动/重布局绝不重置（主线
 * refreshLoadedModelSlots javadoc :1240-1253 m2.6.1 同教训）。hover 三态（主线
 * ModelButton.java:244-257）：悬停播 "hover"、离槽 animationLength*50ms 内播
 * "hover_fadeout"、焦点光标播 "focus"（{@link #setFocused}），动画不存在则该态
 * 跳过=主线 containsKey 同语义（builtin default/01 系无此三名→静走 base；
 * 12_little 全有→三态真播）。
 *
 * <p>矩阵/状态序列=vanilla-mc-1710 GuiInventory.drawEntityOnScreen 可照抄段
 * （GuiInventory.java:63-97：GL11 固定管线+RenderHelper 标准光照+无 stencil+
 * 尾部 lightmap 恢复 :94-96）+两处必改（122 POC be0a794 先例同款）：实体字段链→
 * 直接 rotate（yBodyRot 等效映射 rotate(180-previewYaw, Y)）；RenderManager→
 * {@link LegacyModelTranslator} 直调。槽外零泄漏=YuiBackendGL1710.preview 配方
 * （深度关+scissor，122 YuiBackendGL1122.preview 同契约）。
 */
public final class LegacyPreview1710 implements YuiPreview {

    /** 卡内预览定尺（主线 ModelButton.java:287 第三参 30）。 */
    private static final float CARD_SCALE = 30.0F;
    /** 左栏大预览定尺（主线 PlayerModelScreen.java:836 size=70）。 */
    private static final float PANE_SCALE = 70.0F;
    /** height_scale 解析缺省（RawYsmModel.RawProperties 默认 0.7f）。 */
    private static final float DEFAULT_HEIGHT_SCALE = 0.7F;

    /** 每模型播放态（键=模型 id）：hover 边沿/各动画时间轴起点（122 Playback 同构）。 */
    private static final Map<String, Playback> PLAYBACKS = new ConcurrentHashMap<String, Playback>();

    /** 焦点光标模型 id（L3a 屏侧键盘导航写入；null=无）。 */
    private static volatile String focusedId;

    // 采证打点开关（translator 同款 -Dysm.legacy1710.debug）
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy1710.debug");

    private static long debugFrame;

    private final String modelId;
    private final boolean followMouse;

    private LegacyPreview1710(String modelId, boolean followMouse) {
        this.modelId = modelId;
        this.followMouse = followMouse;
    }

    /** 指定模型 id 的卡内预览（null/default=主面）。 */
    public static LegacyPreview1710 card(String modelId) {
        return new LegacyPreview1710(modelId, false);
    }

    /** 左栏大预览（当前玩家 registry 模型；头/身随鼠标）。 */
    public static LegacyPreview1710 pane() {
        return new LegacyPreview1710(null, true);
    }

    /** 焦点光标（focus 动画触发面；传 null 清除；L3a 屏侧消费）。 */
    public static void setFocused(String modelId) {
        focusedId = modelId;
    }

    /** 每模型 hover/时间轴态（122 LegacyCardPreview.Playback 同构）。 */
    private static final class Playback {
        boolean hovered;
        long unhoverMs;
        boolean resolved;
        boolean hoverAnim;
        boolean fadeoutAnim;
        float fadeoutMs;
        boolean focusAnim;
        // 各动画名独立时间轴（hover 不打断 gui 主时间轴=主线分层同语义）
        final Map<String, Long> starts = new ConcurrentHashMap<String, Long>();
    }

    private static Playback playback(String id) {
        Playback pb = PLAYBACKS.get(id);
        if (pb == null) {
            pb = new Playback();
            PLAYBACKS.put(id, pb);
        }
        return pb;
    }

    @Override
    public void render(int x1, int y1, int x2, int y2, float mouseX, float mouseY, float partialTick) {
        String id = this.followMouse ? currentModelId() : this.modelId;
        if (id == null) {
            id = LegacyModelRegistry.DEFAULT_MODEL_ID;
        }
        LegacyBakedModel model = LegacyModelState.modelOf(id);
        float[] params = LegacyModelState.paramsOf(id, model);
        if (model == null || params == null || model.bones.isEmpty()) {
            return; // 未装载（屏内逐 tick 惰性装载中）：空槽
        }
        ClientModelInfo bundle = LegacyModelState.bundleOf(id);
        ModelProperties props = bundle == null ? null : bundle.getInfo().getModelProperties();
        float heightScale = props == null ? DEFAULT_HEIGHT_SCALE : props.getHeightScale();
        boolean disableRot = props != null && props.isDisablePreviewRotation();
        String baseAnim = props == null ? "" : props.getPreviewAnimation();
        long now = System.currentTimeMillis();

        // --- 姿态/缩放/锚点（主线 ModelButton:287 + ModelPreviewRenderer:773/785/846）---
        float scale = (this.followMouse ? PANE_SCALE : CARD_SCALE) * heightScale;
        // 终值 rotate 角：yawRot 绕 Y，pitchRot 绕 X（正=俯视倾）
        float yawRot;
        float pitchRot;
        float anchorX;
        float anchorY;
        if (this.followMouse) {
            // GuiInventory:60/77-80 配方（122 面采证头随鼠标方向实证）：
            // yBodyRot 等效 atan(dx/40)*20 → rotate(180-yaw)；var4=锚y-mouseY→pitch
            yawRot = 180.0F - (float) Math.atan((x1 + 62.0F - mouseX) / 40.0F) * 20.0F;
            pitchRot = -(float) Math.atan((y1 + 161.0F - mouseY) / 40.0F) * 20.0F;
            anchorX = x1 + 62.0F;   // guiLeft+67（槽 x1=guiLeft+5，主线 :836）
            anchorY = y1 + 161.0F;  // guiTop+190
        } else {
            float previewYaw = disableRot ? 180.0F : 200.0F; // :846
            // 基向换算（122 同式）：本配方正面=rotate(180,Y)，主线正面总角
            // θ = 180 + (180-previewYaw) ≡ -previewYaw；previewYaw=200 ⇒ 正面偏 20°
            yawRot = -previewYaw;
            pitchRot = disableRot ? 0.0F : -10.0F; // :785 rotationX(0 / -10)
            anchorX = x1 + (x2 - x1) / 2.0F;       // x+w/2（:287）
            anchorY = y1 + 65.0F;                  // y+65（:287 y+h/2+20，原点锚定值）
            if (disableRot) {
                anchorY += 5.5F;                   // :773 translate(0, 5.5, 1000)
            }
        }

        // --- 预览动画（语义A）：hover 三态叠加后采样；空=绑定位静像 ---
        Playback pb = playback(id);
        String effective = baseAnim;
        if (!this.followMouse) {
            if (!pb.resolved) {
                pb.hoverAnim = LegacyAnimationSampler.findAnimation(bundle, "hover") != null;
                Animation fadeout = LegacyAnimationSampler.findAnimation(bundle, "hover_fadeout");
                pb.fadeoutAnim = fadeout != null;
                pb.fadeoutMs = fadeout == null ? 0.0F : fadeout.animationLength * 50.0F; // ModelButton:156
                pb.focusAnim = LegacyAnimationSampler.findAnimation(bundle, "focus") != null;
                pb.resolved = true;
            }
            boolean hovered = mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
            if (hovered != pb.hovered) {
                pb.hovered = hovered;
                pb.starts.put(hovered ? "hover" : "hover_fadeout", Long.valueOf(now));
                if (!hovered) {
                    pb.unhoverMs = now;
                }
                if (DEBUG) {
                    System.out.printf("[ysm-legacy1710] card hover edge: id=%s hovered=%b hoverAnim=%b fadeoutMs=%.0f focusAnim=%b%n",
                            id, hovered, pb.hoverAnim, pb.fadeoutMs, pb.focusAnim);
                }
            }
            if (hovered && pb.hoverAnim) {
                effective = "hover";
            } else if (!hovered && pb.fadeoutAnim && pb.fadeoutMs > 0.0F
                    && now - pb.unhoverMs < (long) pb.fadeoutMs) {
                effective = "hover_fadeout"; // :248 离槽 fadeout 窗口
            }
            String focus = focusedId;
            if (focus != null && focus.equals(id) && pb.focusAnim) {
                effective = "focus";
            }
        }
        Long start = pb.starts.get(effective);
        if (start == null) {
            start = Long.valueOf(now); // 首播注册；同名动画跨刷新持续（不重置时间轴）
            pb.starts.put(effective, start);
        }
        float tick = (now - start.longValue()) / 50.0F;
        boolean animated = LegacyAnimationSampler.sample(model, params, bundle, effective, tick);
        if (DEBUG && (debugFrame++ % 40 == 0)) {
            System.out.printf(
                    "[ysm-legacy1710] preview frame=%d id=%s anim=%s playing=%b tick=%.1f bones=%d%n",
                    debugFrame, id, effective, animated, tick, model.bones.size());
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

        // === drawEntityOnScreen 可照抄段（vanilla-mc-1710 GuiInventory.java:63-97，
        //     常量走字面量=vanilla 同形；lwjglx 面无 GL_RESCALE_NORMAL 常量）===
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);              // :64 (2903)
        GL11.glPushMatrix();                                // :65
        GL11.glTranslatef(anchorX, anchorY, 50.0F);         // :66（GUI ortho 基面 +50）
        GL11.glScalef(-scale, scale, scale);                // :67（GUI y 向下而模型 Y-up）
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);           // :68
        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);           // :74
        RenderHelper.enableStandardItemLighting();          // :75（光源方位按当前矩阵捕获）
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);          // :76
        GL11.glRotatef(pitchRot, 1.0F, 0.0F, 0.0F);         // :77（rotationX(tilt) 等效）
        GL11.glRotatef(yawRot, 0.0F, 1.0F, 0.0F);           // :78-82 yBodyRot 等效

        // 必改②：RenderManager.renderEntityWithPosYaw（:84-85）→ 翻译层直调（动画已采样进 params）
        LegacyModelTranslator.render(model, params, 1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();                                 // :91
        RenderHelper.disableStandardItemLighting();         // :92（含 disableColorMaterial）
        GL11.glDisable(32826);                              // :93 GL_RESCALE_NORMAL（防御性配平）
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit); // :94-96 lightmap 尾三连
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /** 左栏消费面：当前玩家 registry 模型 id（122 currentModelId 同语义）。 */
    private static String currentModelId() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null
                ? LegacyModelRegistry.modelIdOf(mc.thePlayer.getUniqueID())
                : LegacyModelRegistry.DEFAULT_MODEL_ID;
    }
}
