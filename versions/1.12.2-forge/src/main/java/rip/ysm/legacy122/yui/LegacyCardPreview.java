package rip.ysm.legacy122.yui;

import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import rip.ysm.legacy122.LegacyAnimationDriver;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyModelTranslator;
import rip.ysm.yui.YuiPreview;

/**
 * 卡内实时预览驱动（M-U2 收编实验分支 be0a794 的 POC 配方，GO 结论照录）。
 *
 * <p>矩阵/状态序列=GuiInventory.drawEntityOnScreen 等效（vanilla-mc-1.12.2
 * GuiInventory.java:93-106 可照抄段）。两处必改（POC 实证）：
 * ①:107-111 实体字段链 → 直接 rotate（RenderManager applyRotations
 * 180-renderYawOffset 等效，renderYawOffset=atan(yawSrc/40)*20；POC 常量 180
 * 即 yawSrc=0 特例；头随鼠标语义对齐主线 ModelPreviewRenderer 的 yBodyRot=yHeadRot
 * 刚体转头消费方式）；②:113-116 RenderManager.renderEntity → 翻译层直调
 * （GUI 无实体渲染管线前提，legacy 渲染走 RenderPlayerEvent 接管）。
 *
 * <p>恢复清单六项（POC 结论 b）：popMatrix / disableStandardItemLighting（含收口
 * colorMaterial，RenderHelper.java:16）/ disableRescaleNormal（冗余防御）/
 * lightmap 尾三连（:126-128 可省保留）/ scissor pop 与深度关由后端 preview 契约收口
 * （YuiBackendGL1122）。深度叠序（POC 结论 c）：模型 z≈-1950 写深度挡后续 2D，
 * preview 返回后深度已关（GuiContainer:73 先例），槽外零泄漏由 scissor 掩深度写入。
 */
@SideOnly(Side.CLIENT)
public final class LegacyCardPreview implements YuiPreview {

    /** 模型缩放 30：主线 ModelButton 预览同值（ModelButton.java:287）；52x70 槽内 2 格模型高 60px。 */
    private static final float MODEL_SCALE = 30.0F;

    private final String modelId;

    public LegacyCardPreview(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public void render(int x1, int y1, int x2, int y2, float mouseX, float mouseY, float partialTick) {
        GeoModel model = LegacyModelState.modelOf(this.modelId);
        float[] params = LegacyModelState.paramsOf(this.modelId, model);
        if (model == null || params == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return; // 未装载（屏内逐 tick 惰性装载中）：空槽
        }
        // 纹理双路（LegacyRenderHook.onRenderPlayerPre 同款：OuterFileTexture 直绑 / RL 回退）
        OuterFileTexture tex = LegacyModelState.textureOf(this.modelId);
        if (tex != null) {
            tex.ensureUploaded();
            GlStateManager.bindTexture(tex.getGlTextureId());
        } else {
            ResourceLocation rl = LegacyModelState.texture();
            if (rl == null) {
                return; // 无任何贴图面（default 主面未走 OuterFileTexture 分支的异常态）
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        }

        float centerX = (x1 + x2) / 2.0F;
        // 模型空间 y=0=脚（Y-up 烘焙，translator 头注）；scale 30 高 60px，槽 70px 上下各留 5
        float feetY = y2 - 5.0F;
        // 头随鼠标源：vanilla GuiInventory:89 传参同形（renderX-mouseX / renderY-50-mouseY），
        // 俯仰锚点取槽中上（POC 实证 :106 var4 同形）
        float yawSrc = centerX - mouseX;
        float pitchSrc = (y1 + y2) / 2.0F - 25.0F - mouseY;

        // === drawEntityOnScreen 可照抄段（GuiInventory.java:93-106，POC 逐行实证）===
        GlStateManager.enableColorMaterial();            // :93
        GlStateManager.pushMatrix();                     // :94
        GlStateManager.translate(centerX, feetY, 50.0F); // :95（GUI ortho z=-2000 基面 +50 → z=-1950 可视带）
        GlStateManager.scale(-MODEL_SCALE, MODEL_SCALE, MODEL_SCALE); // :96
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F); // :97（:96+:97 合并=Y 翻转必须：GUI y 向下而模型 Y-up）
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F); // :103
        RenderHelper.enableStandardItemLighting();       // :104（glLight 位置按当前矩阵捕获=光源方位）
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F); // :105
        GlStateManager.rotate(-((float) Math.atan(pitchSrc / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F); // :106 俯仰跟随

        // 必改①：:107-111 实体字段 → applyRotations(180-renderYawOffset) 矩阵等效（头随鼠标偏航）
        GlStateManager.rotate(180.0F - (float) Math.atan(yawSrc / 40.0F) * 20.0F, 0.0F, 1.0F, 0.0F);

        // idle 动画：时间相位直驱（GuiScreen 无 limbSwing 源，相位重载自 be0a794 收编）
        LegacyAnimationDriver.tick((System.currentTimeMillis() % 1000000L) * 0.001F, 0.45F, model, params);

        // 必改②：:113-116 RenderManager.renderEntity → 翻译层直调
        LegacyModelTranslator.render(model, params, 1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();                      // :123
        RenderHelper.disableStandardItemLighting();      // :124（含 disableColorMaterial，RenderHelper.java:16）
        GlStateManager.disableRescaleNormal();           // :125（translator 内部已配对，照抄=冗余防御）
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit); // :126-128 lightmap 尾三连
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
