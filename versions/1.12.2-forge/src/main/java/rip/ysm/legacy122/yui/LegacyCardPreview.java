package rip.ysm.legacy122.yui;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import rip.ysm.legacy122.LegacyAnimationSampler;
import rip.ysm.legacy122.LegacyModelRegistry;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyModelTranslator;
import rip.ysm.yui.YuiPreview;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI 内实时模型预览驱动（M-U2 r3：主线预览行为语义移植）。
 *
 * <p>两种消费形态：
 * <ul>
 * <li>{@link #card(String)}——卡内预览。姿态/缩放/锚点按主线
 *     ModelPreviewRenderer.renderLivingEntityPreview 逐项：
 *     固定 scale 30×height_scale（ModelButton.java:287 定值 30 ×
 *     IGeoRenderer.renderEarly:89-93 heightScale 链，properties 默认 0.7）+
 *     模型原点锚（锚=卡 x+26,y+65=ModelButton :287 x+w/2,y+h/2+20）；
 *     disable_preview_rotation=true（builtin 全系）→ previewYaw=180/pitch 0/
 *     下移 5.5px（ModelPreviewRenderer.java:773/785/846-868），否则 200/-10/0。
 *     超槽模型靠 scissor 裁切=主线同款观感（无 AABB 自适应，r2 发明已弃）。</li>
 * <li>{@link #pane()}——左栏大预览：固定 70×height_scale（主线
 *     PlayerModelScreen.java:836 size=70）+原点锚（guiLeft+67, guiTop+190，
 *     即槽 x1+62/y1+161）；头/身随鼠标（GuiInventory:92-106 atan/40 公式）。
 *     主线画 LocalPlayer 实体（装备/手持随行）——1.12.2 RenderManager 不可用
 *     只能画选中模型=不可消除项（研究 port_1122.risk 结论）。</li>
 * </ul>
 *
 * <p>预览动画（语义A）：按模型 properties.preview_animation 播放
 * （主线 PlayerModelScreen.java:531 setCurrentAnimation 同语义），
 * {@link LegacyAnimationSampler} 逐骨采样写 boneParams；空串/查不到动画=
 * 绑定位静像（主线 PlayerBaseAnimationPredicate.java:16-19 STOP 同语义，
 * 无 idle 回退）。时间轴跨帧持久：增量刷新/翻页/重布局绝不重置
 * （主线 refreshLoadedModelSlots javadoc :1240-1253 m2.6.1 同教训）。
 * hover 三态（主线 ModelButton.java:244-257）：悬停播 "hover"、离槽
 * animationLength*50ms 内播 "hover_fadeout"、焦点光标播 "focus"
 * （{@link #setFocused}），动画不存在则该态跳过=主线 containsKey 同语义。
 *
 * <p>矩阵/状态序列=GuiInventory.drawEntityOnScreen 可照抄段（1.12.2 vanilla
 * GuiInventory.java:92-129）+POC be0a794 两处必改（实体字段链→直接 rotate；
 * RenderManager→翻译层直调）；yBodyRot 等效映射 rotate(180-previewYaw, Y)。
 */
@SideOnly(Side.CLIENT)
public final class LegacyCardPreview implements YuiPreview {

    /** 卡内预览定尺（ModelButton.java:287 第三参 30）。 */
    private static final float CARD_SCALE = 30.0F;
    /** 左栏大预览定尺（PlayerModelScreen.java:836 size=70）。 */
    private static final float PANE_SCALE = 70.0F;
    /** height_scale 解析缺省（RawYsmModel.RawProperties:224 默认 0.7f）。 */
    private static final float DEFAULT_HEIGHT_SCALE = 0.7F;

    /** 每模型播放态（键=模型 id）：hover 边沿/各动画时间轴起点。 */
    private static final Map<String, Playback> PLAYBACKS = new ConcurrentHashMap<String, Playback>();

    /** 焦点光标模型 id（屏侧键盘导航写入；null=无）。 */
    private static volatile String focusedId;

    // 采证打点开关（LegacyModelTranslator 同款）
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy122.debug");

    private final String modelId;
    private final boolean followMouse;

    private LegacyCardPreview(String modelId, boolean followMouse) {
        this.modelId = modelId;
        this.followMouse = followMouse;
    }

    public static LegacyCardPreview card(String modelId) {
        return new LegacyCardPreview(modelId, false);
    }

    public static LegacyCardPreview pane() {
        return new LegacyCardPreview(null, true);
    }

    /** 焦点光标（focus 动画触发面；传 null 清除）。 */
    public static void setFocused(String modelId) {
        focusedId = modelId;
    }

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
        GeoModel model = LegacyModelState.modelOf(id);
        float[] params = LegacyModelState.paramsOf(id, model);
        if (model == null || params == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return; // 未装载（屏内逐 tick 惰性装载中）：空槽
        }
        ClientModelInfo bundle = LegacyModelState.bundleOf(id);
        ModelProperties props = bundle == null ? null : bundle.getInfo().getModelProperties();
        float heightScale = props == null ? DEFAULT_HEIGHT_SCALE : props.getHeightScale();
        boolean disableRot = props != null && props.isDisablePreviewRotation();
        String baseAnim = props == null ? "" : props.getPreviewAnimation();
        long now = System.currentTimeMillis();

        // --- 姿态/缩放/锚点（主线 ModelPreviewRenderer:773/785/846 + ModelButton:287）---
        float scale = (this.followMouse ? PANE_SCALE : CARD_SCALE) * heightScale;
        // 终值 rotate 角：yawRot 绕 Y，pitchRot 绕 X（正=俯视倾）
        float yawRot;
        float pitchRot;
        float anchorX;
        float anchorY;
        if (this.followMouse) {
            // GuiInventory:92-106 配方（r2 采证头随鼠标方向实证）：yBodyRot 等效
            // atan(dx/40)*20 → applyRotations rotate(180-yaw)
            yawRot = 180.0F - (float) Math.atan((x1 + 62.0F - mouseX) / 40.0F) * 20.0F;
            pitchRot = -(float) Math.atan((y1 + 56.0F - mouseY) / 40.0F) * 20.0F;
            anchorX = x1 + 62.0F;  // guiLeft+67（槽 x1=guiLeft+5，:836）
            anchorY = y1 + 161.0F; // guiTop+190
        } else {
            float previewYaw = disableRot ? 180.0F : 200.0F; // :846
            // 基向换算：本配方正面=rotate(180,Y)（vanilla 纸娃娃/r2 pane 实证），
            // 主线正面=setupRotations Y(180-bodyYaw) 的 bodyYaw=180 ⇒ 总角
            // θ = 180 + (180-previewYaw) ≡ -previewYaw；previewYaw=200 ⇒ 正面偏 20°
            yawRot = -previewYaw;
            pitchRot = disableRot ? 0.0F : -10.0F; // :785 rotationX(0 / -10)
            anchorX = x1 + (x2 - x1) / 2.0F;       // x+26（:287 x+w/2）
            anchorY = y1 + 65.0F;                  // y+65（:287 y+h/2+20）
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
                // 三态边沿打点（-Dysm.legacy122.debug 开；builtin 包无 hover/fadeout/
                // focus 动画名→containsKey 语义下该态跳过回 base=主线同行为）
                if (DEBUG) {
                    System.out.printf("[ysm-legacy122] card hover edge: id=%s hovered=%b hoverAnim=%b fadeoutMs=%.0f focusAnim=%b%n",
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
        LegacyAnimationSampler.sample(model, params, bundle, effective, (now - start.longValue()) / 50.0F);

        // --- 纹理双路（LegacyRenderHook.onRenderPlayerPre 同款：OuterFileTexture 直绑 / RL 回退）---
        OuterFileTexture tex = LegacyModelState.textureOf(id);
        if (tex != null) {
            tex.ensureUploaded();
            GlStateManager.bindTexture(tex.getGlTextureId());
        } else {
            ResourceLocation rl = LegacyModelState.texture();
            if (rl == null) {
                return;
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        }

        // === drawEntityOnScreen 可照抄段（GuiInventory.java:92-106）===
        GlStateManager.enableColorMaterial();            // :93
        GlStateManager.pushMatrix();                     // :94
        GlStateManager.translate(anchorX, anchorY, 50.0F); // :95（原点锚：GUI ortho z=-2000 基面 +50）
        GlStateManager.scale(-scale, scale, scale);      // :96（固定尺×heightScale，无 AABB）
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F); // :97（:96+:97 合并=Y 翻转必须：GUI y 向下而模型 Y-up）
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F); // :100
        RenderHelper.enableStandardItemLighting();       // :101（glLight 位置按当前矩阵捕获=光源方位）
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F); // :102
        GlStateManager.rotate(pitchRot, 1.0F, 0.0F, 0.0F); // 俯仰（rotationX(tilt) 等效）
        GlStateManager.rotate(yawRot, 0.0F, 1.0F, 0.0F); // yBodyRot=previewYaw 的 applyRotations 等效

        // 必改②：RenderManager.renderEntity → 翻译层直调（动画已采样进 params）
        LegacyModelTranslator.render(model, params, 1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();                      // :123
        RenderHelper.disableStandardItemLighting();      // :124（含 disableColorMaterial）
        GlStateManager.disableRescaleNormal();           // :125（translator 内部已配对，照抄=冗余防御）
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit); // :126-128 lightmap 尾三连
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private static String currentModelId() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.player != null
                ? LegacyModelRegistry.modelIdOf(mc.player.getUniqueID())
                : LegacyModelRegistry.DEFAULT_MODEL_ID;
    }
}
