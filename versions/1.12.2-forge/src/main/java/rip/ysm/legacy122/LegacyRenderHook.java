package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 渲染接缝核心（wave-d-b2：接缝从 RenderPlayerEvent.Pre 迁至
 * RenderLivingBase.renderModel HEAD mixin，RenderLivingBaseMixin 注入）。
 *
 * 迁移根因：Forge 14.23.x RenderPlayerEvent.Pre 在 RenderPlayer.doRender 顶触发
 * （forge 1.12.x patches RenderPlayer.java.patch 原文），取消吞掉 super.doRender
 * 全链——renderLivingAt 定位/applyRotations（RenderPlayer 覆写：睡床对齐+死亡倾倒+
 * 鞘翅姿态）/prepareScale/setBrightness/renderLayers（盔甲披风鞘翅手持层）/名牌
 * 全部缺失，模型悬空在眼高（harness/out/wd-b1 item5 截图实证：脚下 vanilla 影子
 * 才是玩家真实位置）。改缝后 vanilla 主链照跑仅模型绘制被取消（renderModel:136，
 * prepareScale:99 之后 renderLayers:143 之前）——定位/姿态/层/名牌全部继承，
 * 零自补代码；B1 名牌补渲（原 renderNameTag/canRenderName/bodyYaw）随之退役，
 * 名牌回归 vanilla super.doRender:157 passSpecialRender 面方向。
 *
 * 注入点 GL 后缀=prepareScale 的 scale(-1,-1,1)·preRenderCallback(0.9375)·
 * translate(0,-1.501,0)（vanilla-mc-1.12.2 RenderLivingBase.java:160-167 +
 * RenderPlayer.java:124-127），逆变换 translate(0,1.501,0)·scale(1/0.9375)·
 * scale(-1,-1,1) 还原实体空间——translator「脚在 y=0」契约不变。
 *
 * B1 七项在新接缝下的去留：item1 alpha/blend、item2 glow、item3 受击红、
 * item5 height/widthScale、item6 隐身三态、item7 partialTick 均在 translator/
 * hook 内自管，保留；item4 名牌删除（vanilla 面恢复）。item6 必须保留：vanilla
 * 隐身三态在 renderModel:184-201 内部，被本接管 cancel 绕过。
 */
@SideOnly(Side.CLIENT)
public final class LegacyRenderHook {

    private LegacyRenderHook() {
    }

    /**
     * renderModel 注入回调（RenderLivingBaseMixin）。返回 true=已接管（vanilla
     * mainModel.render 被取消）。ageInTicks=handleRotationFloat（doRender:97，
     * ticksExisted+partialTick）——partialTick 反演同 1710 item7（renderModel
     * 形参无 partialTick）。
     *
     * 读侧：按被渲染玩家 getUniqueID 查 LegacyModelRegistry.modelIdOf（S2C 同步
     * 落表，缺省回退 default），再从 LegacyModelState 按 id 取模型+骨参数+变体
     * 纹理；查不到的非 default id 由 LegacyModelLoader.loadModel 惰性装载（失败
     * 负缓存后回退 default，返回 false 走 vanilla 绘制）。
     */
    public static boolean takeover(net.minecraft.entity.EntityLivingBase base, float ageInTicks) {
        if (!(base instanceof net.minecraft.client.entity.AbstractClientPlayer)) {
            return false;
        }
        net.minecraft.entity.player.EntityPlayer player =
                (net.minecraft.entity.player.EntityPlayer) base;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        // DisableSelfModel/DisableOtherModel（wave-d-b4）：主线 ReplacePlayerRenderEvent
        // :36/:39 同语义——本端开 self 关=自己走 vanilla 绘制，other 关=他人走 vanilla。
        // vanilla 面恢复即名牌/盔甲层等同步回 vanilla（接管取消=零绘制）
        boolean self = mc.player == player;
        if (self && rip.ysm.LegacyConfig.disableSelfModel()) {
            return false;
        }
        if (!self && rip.ysm.LegacyConfig.disableOtherModel()) {
            return false;
        }
        float partialTick = ageInTicks - (float) player.ticksExisted;
        String modelId = LegacyModelRegistry.modelIdOf(player.getUniqueID());
        if (!LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)
                && LegacyModelState.modelOf(modelId) == null) {
            // 惰性装载（同步包先于渲染到达的正常路径不会走到这里；兜底）。
            // 失败已负缓存于 LegacyModelLoader，此处不再每帧打日志
            if (!LegacyModelLoader.loadModel(modelId)) {
                modelId = LegacyModelRegistry.DEFAULT_MODEL_ID;
            }
        }
        GeoModel model = LegacyModelState.modelOf(modelId);
        if (model == null || model.bakedBones == null || model.bakedBones.isEmpty()) {
            return false;
        }
        float[] boneParams = LegacyModelState.paramsOf(modelId, model);
        if (boneParams == null) {
            return false;
        }
        // 纹理绑定：按玩家稳定变体优先（B2 多纹理面），null 回退 L1 占位皮肤
        //（1.12.2 RenderManager 纹理入口是 public 字段 renderEngine，RenderManager.java:123）
        com.elfmcys.yesstevemodel.client.texture.OuterFileTexture tex =
                LegacyModelState.variantOf(modelId, player.getUniqueID());
        net.minecraft.client.renderer.entity.RenderManager rm = net.minecraft.client.Minecraft
                .getMinecraft().getRenderManager();
        if (tex != null) {
            // 解析态纹理（byte[] 构造）不走 TextureManager 装载面——首次绑定前
            // 就地解码+上传（OuterFileTexture.ensureUploaded，DynamicTexture 同款 GL 路径）
            tex.ensureUploaded();
            // 1.12.2 TextureManager 只有 bindTexture(ResourceLocation)（TextureManager.java:32
            // MCP 实证）——AbstractTexture 直挂 glTextureId 走 bindTexture(int) 同款 GL 绑定
            GlStateManager.bindTexture(tex.getGlTextureId());
        } else {
            rm.renderEngine.bindTexture(LegacyModelState.texture());
        }
        LegacyAnimationDriver.tick(player, partialTick,
                model, boneParams, LegacyModelState.bundleOf(modelId));
        // item2：实体 lightmap 坐标传 translator（ysmGlow 发光骨 240 全亮覆盖+恢复用；
        // isBurning 置 15728880 与 vanilla RenderManager.renderEntityStatic:322-328 同款）。
        int lightmap = player.isBurning() ? 15728880 : player.getBrightnessForRender();
        // item3：受击红闪（hurtTime/deathTime 是 EntityLivingBase 公有字段；红强度=
        // getBrightness()，vanilla 1710 doRender:177 glColor4f(var29,0,0,0.4) 同源）。
        // 注：接缝迁移后 vanilla setDoRenderBrightness（doRender:135）的红色 COMBINE
        // texenv 也在本绘制窗内生效（与 vanilla 玩家同面），叠加本二次覆盖红≈稍强。
        float hurtRed = player.hurtTime > 0 || player.deathTime > 0 ? player.getBrightness() : 0.0F;
        // item5：模型 properties height_scale/width_scale。语义对位主线
        // IGeoRenderer.renderEarly:89-93：scale(heightScale, widthScale, heightScale)；
        // 缩放锚=模型原点（脚 y=0）。
        ClientModelInfo bundle = LegacyModelState.bundleOf(modelId);
        ModelProperties props = bundle == null ? null : bundle.getInfo().getModelProperties();
        float heightScale = props == null ? 1.0F : props.getHeightScale();
        float widthScale = props == null ? 1.0F : props.getWidthScale();
        // item6：隐身检查（vanilla 三态在 renderModel:184-201 内部，被接管绕过）。
        // 可见→不透明画；隐身但观察者可见（isInvisibleToPlayer=false，队伍
        // seeFriendlyInvisibles 等）→ ghost 半透明 0.15（Profile.c 同值）；隐身且
        // 观察者不可见→跳过绘制（动画 tick 照跑）。
        boolean visibleBody = !player.isInvisible();
        boolean ghost = !visibleBody && !player.isInvisibleToPlayer(mc.player);
        if (visibleBody || ghost) {
            if (ghost) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.depthMask(false);
            }
            GlStateManager.pushMatrix();
            // prepareScale 后缀逆变换（S(-1,-1,1)·U(0.9375)·T(0,-1.501,0) 的
            // T⁻¹·U⁻¹·S⁻¹，还原实体空间脚原点；1.501/0.9375 同 vanilla 常量）
            GlStateManager.translate(0.0F, 1.501F, 0.0F);
            float inv = 1.0F / 0.9375F;
            GlStateManager.scale(inv, inv, inv);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.scale(heightScale, widthScale, heightScale);
            LegacyModelTranslator.render(model, boneParams,
                    1.0f, 1.0f, 1.0f, ghost ? 0.15F : 1.0f, lightmap, hurtRed);
            GlStateManager.popMatrix();
            if (ghost) {
                // Profile.c clean 同款（GlStateManager.java:1048-1052）
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, 0.1F);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        return true;
    }
}
