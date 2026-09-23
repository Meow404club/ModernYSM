package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
// 绑定/绘制侧 GL 一律 org.lwjglx（lwjgl3ify 运行面直译面；L1 translator 先例同款）
import org.lwjglx.opengl.GL11;

/**
 * 1.7.10 渲染接缝核心（legacy-1710-l1-render；L2a 切真实装载模型）。
 *
 * 1.7.10 无 RenderPlayerEvent——Mixin 直接 @Inject RendererLivingEntity.renderModel
 * head 切（调用点 doRender:111）。L2a 读侧对齐 1.12.2 LegacyRenderHook：按被渲染
 * 玩家 getUniqueID（vanilla-mc-1710 Entity.java:1644）查 LegacyModelRegistry
 * → 按模型 id 从 LegacyModelState 取模型+骨参数+纹理；查不到的非 default id 由
 * LegacyModelLoader.loadModel 惰性装载（失败负缓存后回退 default）。
 *
 * 纹理：1.7.10 无 GlStateManager——解析态 OuterFileTexture.ensureUploaded 后走
 * org.lwjglx GL11.glBindTexture 直绑（研究 block1 twin_diff 裁定面）；占位皮肤
 * 回退走 TextureManager.bindTexture(RL)（TextureManager.java:30 唯一公开面）。
 *
 * Y 轴约定（1.12.2 修复教训 613fa38）：注入点在 vanilla scale(-1,-1,1) 之后——
 * 翻译层内只补 X 镜像。
 */
public final class LegacyRenderHook {

    private LegacyRenderHook() {
    }

    public static boolean isModelActive() {
        return LegacyModelState.mainModel() != null;
    }

    /**
     * renderModel 注入回调。返回 true = 已接管（vanilla render 被取消）。
     * player 为被渲染实体（1.7.10 renderModel 参数是 EntityLivingBase，RenderPlayer
     * 链实际传入 AbstractClientPlayer）。
     */
    public static boolean takeover(Object entity) {
        if (!(entity instanceof AbstractClientPlayer)) {
            return false;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        // L2a 读侧：UUID→模型 id；未装载的非 default id 惰性装载（失败负缓存，
        // 不打每帧日志），装载失败回退 default
        String modelId = LegacyModelRegistry.modelIdOf(player.getUniqueID());
        if (!LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)
                && LegacyModelState.modelOf(modelId) == null
                && !LegacyModelLoader.loadModel(modelId)) {
            modelId = LegacyModelRegistry.DEFAULT_MODEL_ID;
        }
        LegacyBakedModel model = LegacyModelState.modelOf(modelId);
        if (model == null || model.bones.isEmpty()) {
            return false;
        }
        float[] boneParams = LegacyModelState.paramsOf(modelId, model);
        if (boneParams == null) {
            return false;
        }

        // 纹理绑定：解析态 OuterFileTexture 直绑（首次绑定前就地解码+上传），
        // null 回退占位皮肤/玩家皮肤 RL
        com.elfmcys.yesstevemodel.client.texture.OuterFileTexture tex =
                LegacyModelState.textureOf(modelId);
        if (tex != null) {
            tex.ensureUploaded();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlTextureId());
        } else {
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            ResourceLocation texRl = LegacyModelState.texture();
            tm.bindTexture(texRl != null ? texRl : player.getLocationSkin());
        }

        LegacyAnimationDriver.tick(player, 0.5f, model, boneParams);
        // item2：实体 lightmap 坐标传 translator（ysmGlow 发光骨 240 全亮覆盖+恢复用；
        // isBurning 置 15728880 与 vanilla RenderManager.func_147939_a:232-240 同款；
        // getBrightnessForRender 形参不参与计算，partialTick 值无影响）
        int lightmap = player.isBurning() ? 15728880 : player.getBrightnessForRender(0.5f);
        // item3：受击红闪（vanilla 1710 doRender:176 hurtTime>0||deathTime>0 面，
        // 红强度=getBrightness(partialTick) 同源 :177 var29）
        float hurtRed = player.hurtTime > 0 || player.deathTime > 0 ? player.getBrightness(0.5f) : 0.0F;
        // item5：模型 properties height_scale/width_scale 世界路径（此前恒 1:1）。
        // 语义对位主线 IGeoRenderer.renderEarly:89-93：scale(heightScale, widthScale,
        // heightScale)；缩放锚=模型原点（脚 y=0）。push/pop 限定模型绘制内——不泄入
        // vanilla renderModel 后续 shouldRenderPass 盔甲层（原版 biped 定位，保持原尺寸）。
        ClientModelInfo bundle = LegacyModelState.bundleOf(modelId);
        ModelProperties props = bundle == null ? null : bundle.getInfo().getModelProperties();
        float heightScale = props == null ? 1.0F : props.getHeightScale();
        float widthScale = props == null ? 1.0F : props.getWidthScale();
        GL11.glPushMatrix();
        GL11.glScalef(heightScale, widthScale, heightScale);
        LegacyModelTranslator.render(model, boneParams, 1.0f, 1.0f, 1.0f, 1.0f, lightmap, hurtRed);
        GL11.glPopMatrix();
        return true;
    }
}
