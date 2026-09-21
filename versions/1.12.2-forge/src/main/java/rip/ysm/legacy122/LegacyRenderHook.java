package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 渲染接缝（legacy-1222-l1-render commit 4；L3-1 按 UUID 查表取模型）。
 *
 * RenderPlayerEvent.Pre（forge 14.23.x，tmp/refs/forge-api/forge-1.12.x RenderPlayerEvent.java:54
 * @Cancelable 实证）拦玩家渲染→翻译层接管；L3-1 读侧：按被渲染玩家
 * EntityPlayer.getUniqueID 查 LegacyModelRegistry.modelIdOf（S2C 同步落表，缺省
 * 回退 default），再从 LegacyModelState 按 id 取模型+骨参数+纹理；查不到的
 * 非 default id 由 LegacyModelLoader.loadModel 惰性装载（装载失败回退 default）。
 */
@SideOnly(Side.CLIENT)
public final class LegacyRenderHook {

    private LegacyRenderHook() {
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        String modelId = LegacyModelRegistry.modelIdOf(event.getEntityPlayer().getUniqueID());
        if (!LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)
                && LegacyModelState.modelOf(modelId) == null) {
            // 惰性装载（同步包先于渲染到达的正常路径不会走到这里；兜底）
            if (!LegacyModelLoader.loadModel(modelId)) {
                System.out.println("[ysm-legacy122] model not available, fallback default: id=" + modelId);
                modelId = LegacyModelRegistry.DEFAULT_MODEL_ID;
            }
        }
        GeoModel model = LegacyModelState.modelOf(modelId);
        if (model == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return;
        }
        float[] boneParams = LegacyModelState.paramsOf(modelId, model);
        if (boneParams == null) {
            return;
        }
        // 1.12.2 无 PoseStack：RenderManager.renderEntityWithYawPitch 已在 GL 建好
        // 实体定位状态（vanilla-mc-1.12.2 RenderManager 实证 doRender 链），翻译层
        // 从模型原点起绘，vanilla RenderPlayer 取消即无原版模型重叠
        event.setCanceled(true);
        net.minecraft.client.renderer.entity.RenderManager rm = net.minecraft.client.Minecraft
                .getMinecraft().getRenderManager();
        // L2 纹理面：真实 OuterFileTexture 优先，null 回退 L1 占位皮肤
        //（1.12.2 RenderManager 纹理入口是 public 字段 renderEngine，RenderManager.java:123）
        com.elfmcys.yesstevemodel.client.texture.OuterFileTexture tex =
                LegacyModelState.textureOf(modelId);
        if (tex != null) {
            // 解析态纹理（byte[] 构造）不走 TextureManager 装载面——首次绑定前
            // 就地解码+上传（OuterFileTexture.ensureUploaded，DynamicTexture 同款 GL 路径）
            tex.ensureUploaded();
            // 1.12.2 TextureManager 只有 bindTexture(ResourceLocation)（TextureManager.java:32
            // MCP 实证）——AbstractTexture 直挂 glTextureId 走 bindTexture(int) 同款 GL 绑定
            net.minecraft.client.renderer.GlStateManager.bindTexture(tex.getGlTextureId());
        } else {
            rm.renderEngine.bindTexture(LegacyModelState.texture());
        }
        LegacyAnimationDriver.tick(event.getEntityPlayer(), event.getPartialRenderTick(),
                model, boneParams);
        LegacyModelTranslator.render(model, boneParams,
                1.0f, 1.0f, 1.0f, 1.0f);
    }
}
