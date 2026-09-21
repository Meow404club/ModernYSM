package rip.ysm.legacy1710;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 1.7.10 渲染接缝核心（legacy-1710-l1-render）。
 *
 * 1.12.2 LegacyRenderHook 的 1.7.10 twin：1.7.10 无 RenderPlayerEvent（forge 1.7.10
 * 事件面实证缺位）——1.12.2 的 Pre cancel 接缝不可用，改由 Mixin 直接 @Inject
 * RendererLivingEntity.renderModel（调用点 doRender:111）head 切：模型就绪时
 * cancel 掉 vanilla mainModel.render 并画 YSM 模型（同一 GL 状态：rotateCorpse/
 * scale(-1,-1,1)/translate(-24*0.0625) 已就位，vanilla-mc-1.7.10
 * RendererLivingEntity.java:93-111 实证）。
 *
 * Y 轴约定（1.12.2 修复教训 613fa38，LegacyModelTranslator 头注详述）：烘焙数据
 * Y-up，注入点在 vanilla scale(-1,-1,1) 之后——翻译层内只补 X 镜像。
 *
 * 纹理：1.7.10 TextureManager 只有 bindTexture(ResourceLocation)
 *（TextureManager.java:30 实证），走内置占位皮肤 ResourceLocation。
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
        LegacyBakedModel model = LegacyModelState.mainModel();
        if (model == null || !(entity instanceof AbstractClientPlayer)) {
            return false;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        float[] boneParams = LegacyModelState.boneParams(model);
        if (boneParams == null) {
            return false;
        }

        // 纹理绑定：占位皮肤（1.7.10 TextureManager.java:30 bindTexture(RL) 唯一公开面）
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        ResourceLocation tex = LegacyModelState.texture();
        if (tex != null) {
            tm.bindTexture(tex);
        } else {
            tm.bindTexture(player.getLocationSkin());
        }

        long tick = LegacyModelState.nextAnimTick();
        LegacyAnimationDriver.tick(player, 0.5f, tick);
        LegacyModelTranslator.render(model, LegacyModelState.currentBoneParams(),
                1.0f, 1.0f, 1.0f, 1.0f);
        return true;
    }
}
