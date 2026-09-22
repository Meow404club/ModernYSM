package rip.ysm.legacy122.yui;

import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import rip.ysm.legacy122.LegacyAnimationDriver;
import rip.ysm.legacy122.LegacyModelRegistry;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyModelTranslator;
import rip.ysm.yui.YuiPreview;

/**
 * GUI 内实时模型预览驱动（M-U2 r2 重构：对齐主线 ModelPreviewRenderer 观感）。
 *
 * <p>两种消费形态：
 * <ul>
 * <li>{@link #card(String)}——卡片内固定姿态（yaw 200°/pitch -10°，刚体头，
 *     无鼠标跟随）：主线 ModelButton 卡内预览同语义（ModelButton.java:287
 *     renderLivingEntityPreview + disablePreviewRotation=false 的 previewYaw=200
 *     与 rotationX=-10，ModelPreviewRenderer.java:846-868）。</li>
 * <li>{@link #pane()}——左栏大预览，头/身随鼠标（vanilla GuiInventory:106-111
 *     atan(x/40)*20 公式，1.12.2 原生背包纸娃娃手感；主线 1.20.1 左栏
 *     renderEntityInInventoryFollowsMouse 同为 atan/40 族）。</li>
 * </ul>
 *
 * <p>r2 修：缩放改按模型 AABB 自适应（{@link LegacyModelTranslator#computeBounds}），
 * 脚底锚 bbox 下缘——不同模型烘焙尺寸/原点差异大，r1 固定 scale 30 使大型模型
 * 巨大化裁切、原点偏移模型沉底（854x480 采证 old-1222-scale2-gui.png 实证）。
 * 自适应后 2 格人形自然落在主线同值域（≈28-30），异形模型恰好收在槽内=主线观感。
 *
 * <p>矩阵/状态序列仍=GuiInventory.drawEntityOnScreen 可照抄段（:93-106）+两处必改
 * （POC be0a794 结论）：实体字段链→直接 rotate；RenderManager→翻译层直调。
 * 光照 135° 进出对与 lightmap 尾三连照抄不动；深度/scissor 契约在
 * {@link YuiBackendGL1122#preview} 收口。
 */
@SideOnly(Side.CLIENT)
public final class LegacyCardPreview implements YuiPreview {

    /** 槽内填充率：AABB 自适应目标的留边比（主线卡内 2 格模型≈槽高 86%）。 */
    private static final float FILL = 0.80F;

    private static final Map<String, float[]> BOUNDS = new ConcurrentHashMap<>();

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

    @Override
    public void render(int x1, int y1, int x2, int y2, float mouseX, float mouseY, float partialTick) {
        String id = this.followMouse ? currentModelId() : this.modelId;
        GeoModel model = LegacyModelState.modelOf(id);
        float[] params = LegacyModelState.paramsOf(id, model);
        if (model == null || params == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return; // 未装载（屏内逐 tick 惌性装载中）：空槽
        }
        // 动画先于 bounds：首帧 params 为全零初始态（scale 旗标=0 → 全骨不可见），
        // tick 置中性位后 AABB 才有效（16:33 采证 bounds=±3.4e38 空盒根因）
        LegacyAnimationDriver.tick((System.currentTimeMillis() % 1000000L) * 0.001F, 0.45F, model, params);
        float[] b = boundsOf(id, model, params);
        if (b == null) {
            return;
        }
        // 纹理双路（LegacyRenderHook.onRenderPlayerPre 同款：OuterFileTexture 直绑 / RL 回退）
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

        // AABB 自适应缩放+锚点：x=bbox 中心对槽中心，y=bbox 下缘对槽底留 pad
        float slotW = x2 - x1;
        float slotH = y2 - y1;
        float bw = Math.max(b[3] - b[0], 1.0E-4F);
        float bh = Math.max(b[4] - b[1], 1.0E-4F);
        float s = Math.min(FILL * slotW / bw, FILL * slotH / bh);
        float cx = (x1 + x2) / 2.0F - (b[0] + b[3]) / 2.0F * s;
        float feetY = y2 - (this.followMouse ? 10.0F : 4.0F) + b[1] * s;

        // 姿态角：pane=鼠标跟随（GuiInventory:89 传参同形 atan(src/40)*20）；
        // card=主线固定 yaw200/pitch-10（GL 等效 180-200=-20 / -10，刚体不歪头）
        float yawDeg;
        float pitchDeg;
        if (this.followMouse) {
            yawDeg = (float) Math.atan((cx - mouseX) / 40.0F) * 20.0F;
            pitchDeg = (float) Math.atan(((y1 + y2) / 2.0F - 25.0F - mouseY) / 40.0F) * 20.0F;
        } else {
            yawDeg = -20.0F;
            pitchDeg = -10.0F;
        }

        // === drawEntityOnScreen 可照抄段（GuiInventory.java:93-106）===
        GlStateManager.enableColorMaterial();            // :93
        GlStateManager.pushMatrix();                     // :94
        GlStateManager.translate(cx, feetY, 50.0F);      // :95（GUI ortho z=-2000 基面 +50 → z=-1950 可视带）
        GlStateManager.scale(-s, s, s);                  // :96
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F); // :97（:96+:97 合并=Y 翻转必须：GUI y 向下而模型 Y-up）
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F); // :103
        RenderHelper.enableStandardItemLighting();       // :104（glLight 位置按当前矩阵捕获=光源方位）
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F); // :105
        GlStateManager.rotate(-pitchDeg, 1.0F, 0.0F, 0.0F); // :106 俯仰（实体 rotationPitch 等效值）
        GlStateManager.rotate(180.0F - yawDeg, 0.0F, 1.0F, 0.0F); // :107-111 applyRotations 等效（renderYawOffset=yawDeg）

        // 必改②：:113-116 RenderManager.renderEntity → 翻译层直调（动画已在 bounds 前驱动）
        LegacyModelTranslator.render(model, params, 1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();                      // :123
        RenderHelper.disableStandardItemLighting();      // :124（含 disableColorMaterial，RenderHelper.java:16）
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

    /** AABB 一次计算按模型缓存（模型进程内不重载；L3-3 热重载落地时随缓存失效）。 */
    private static float[] boundsOf(String id, GeoModel model, float[] params) {
        float[] b = BOUNDS.get(id);
        if (b == null) {
            b = LegacyModelTranslator.computeBounds(model, params);
            if (b != null) {
                BOUNDS.put(id, b);
                System.out.printf("[ysm-legacy122] preview bounds: id=%s x[%.2f..%.2f] y[%.2f..%.2f] z[%.2f..%.2f]%n",
                        id, b[0], b[3], b[1], b[4], b[2], b[5]);
            }
        }
        return b;
    }
}
