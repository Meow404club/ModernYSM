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
            // 惰性装载（同步包先于渲染到达的正常路径不会走到这里；兜底）。
            // 修①：失败已负缓存于 LegacyModelLoader，此处不再每帧打日志
            if (!LegacyModelLoader.loadModel(modelId)) {
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
                model, boneParams, LegacyModelState.bundleOf(modelId));
        // item2：实体 lightmap 坐标传 translator（ysmGlow 发光骨 240 全亮覆盖+恢复用；
        // isBurning 置 15728880 与 vanilla RenderManager.renderEntityStatic:324-328 同款）。
        // rebase 冲突解决（review-merge）：与 wave-d-anim1 状态机卡正交——驱动侧保
        // bundle 实参（状态机），lightmap/hurtRed 计算保留（B1 item2/3），translator
        // 尾调共用。声明取 item3 强类型 EntityPlayer（deathTime 访问面）。
        net.minecraft.entity.player.EntityPlayer player = event.getEntityPlayer();
        int lightmap = player.isBurning() ? 15728880 : player.getBrightnessForRender();
        // item3：受击红闪（hurtTime/deathTime 是 EntityLivingBase 公有字段；红强度=
        // getBrightness()，vanilla 1710 doRender:177 glColor4f(var29,0,0,0.4) 同源）
        float hurtRed = player.hurtTime > 0 || player.deathTime > 0 ? player.getBrightness() : 0.0F;
        LegacyModelTranslator.render(model, boneParams,
                1.0f, 1.0f, 1.0f, 1.0f, lightmap, hurtRed);
        // item4：名牌恢复（Pre 取消吞 vanilla doRender 主链连坐名牌渲染面）
        renderNameTag(player, event.getPartialRenderTick());
    }

    /**
     * 名牌补渲（item4）。语义逐面复制 vanilla-mc-1.12.2：
     * canRenderName（RenderLivingBase.java:387-411，含隐身显隐+计分板
     * EnumVisible 四档规则+isGuiEnabled/renderViewEntity/isBeingRidden 门）→
     * renderName 距离门（RenderLivingBase renderName：潜行 32 格否则 64 格）→
     * renderLivingLabel（Render.java:293-304：名牌高度=height+0.5-潜行 0.25、
     * 潜行走 seeThrough 面交 EntityRenderer.drawNameplate:1778 公有静态）。
     *
     * 1.12.2 renderName 在 doRender popMatrix 后的相机空间画（名牌不受实体
     * 旋转影响）；本 hook 的 GL 栈已含 applyRotations 的 rotate(180- yawBody)
     * （mu2-r3 采证：定位+朝向已就位），先 rotate(yawBody-180) 退掉实体朝向
     * 再画。死亡倒地/Dinnerbone 翻转面未退（edge case，倒地帧名牌随之倾斜）。
     */
    private static void renderNameTag(net.minecraft.entity.player.EntityPlayer player, float partialTick) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.renderer.entity.RenderManager rm = mc.getRenderManager();
        if (!canRenderName(player, rm)) {
            return;
        }
        double dist = player.getDistanceSq(rm.renderViewEntity);
        float maxDist = player.isSneaking() ? 32.0F : 64.0F;
        if (dist >= (double) (maxDist * maxDist)) {
            return;
        }
        String str = player.getDisplayName().getFormattedText();
        boolean sneaking = player.isSneaking();
        float labelY = player.height + 0.5F - (sneaking ? 0.25F : 0.0F);
        int verticalShift = "deadmau5".equals(str) ? -10 : 0;
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.rotate(bodyYaw(player, partialTick) - 180.0F, 0.0F, 1.0F, 0.0F);
        // vanilla renderName 显式 alphaFunc(516,0.1)（其前置 enableAlpha 来自
        // doRender:114，本 hook 被取消连坐）——本面自管 enable/restore 配对
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.alphaFunc(org.lwjgl.opengl.GL11.GL_GREATER, 0.1F);
        net.minecraft.client.renderer.EntityRenderer.drawNameplate(rm.getFontRenderer(), str,
                0.0F, labelY, 0.0F, verticalShift,
                rm.playerViewY, rm.playerViewX, rm.options.thirdPersonView == 2, sneaking);
        net.minecraft.client.renderer.GlStateManager.disableAlpha();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    /** vanilla-mc-1.12.2 RenderLivingBase.canRenderName:387-411 同款语义。 */
    private static boolean canRenderName(net.minecraft.entity.player.EntityPlayer player,
                                         net.minecraft.client.renderer.entity.RenderManager rm) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.entity.EntityPlayerSP viewer = mc.player;
        boolean visible = !player.isInvisibleToPlayer(viewer);
        if (player != viewer) {
            net.minecraft.scoreboard.Team entityTeam = player.getTeam();
            if (entityTeam != null) {
                net.minecraft.scoreboard.Team viewerTeam = viewer.getTeam();
                switch (entityTeam.getNameTagVisibility()) {
                    case ALWAYS:
                        return visible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return viewerTeam == null ? visible
                                : entityTeam.isSameTeam(viewerTeam)
                                && (entityTeam.getSeeFriendlyInvisiblesEnabled() || visible);
                    case HIDE_FOR_OWN_TEAM:
                        return viewerTeam == null ? visible
                                : !entityTeam.isSameTeam(viewerTeam) && visible;
                    default:
                        return true;
                }
            }
        }
        return net.minecraft.client.Minecraft.isGuiEnabled()
                && player != rm.renderViewEntity && visible && !player.isBeingRidden();
    }

    /**
     * 实体朝向 yaw（含骑乘钳制），vanilla-mc-1.12.2 RenderLivingBase.doRender:55-93
     * 的 applyRotations 入参同款计算（1.7.10 RendererLivingEntity.doRender:70-88
     * 逐行同构）；interpolateRotation 为 protected 本处内联（wrapDegrees 差值插值）。
     */
    private static float bodyYaw(net.minecraft.entity.player.EntityPlayer player, float partialTick) {
        float yaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTick);
        if (player.isRiding() && player.getRidingEntity() instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase mount =
                    (net.minecraft.entity.EntityLivingBase) player.getRidingEntity();
            yaw = interpolateRotation(mount.prevRenderYawOffset, mount.renderYawOffset, partialTick);
            float headYaw = interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, partialTick);
            float clamped = net.minecraft.util.math.MathHelper.wrapDegrees(headYaw - yaw);
            if (clamped < -85.0F) {
                clamped = -85.0F;
            }
            if (clamped >= 85.0F) {
                clamped = 85.0F;
            }
            yaw = headYaw - clamped;
            if (clamped * clamped > 2500.0F) {
                yaw += clamped * 0.2F;
            }
        }
        return yaw;
    }

    private static float interpolateRotation(float prev, float current, float partialTick) {
        float delta = current - prev;
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        while (delta >= 180.0F) {
            delta -= 360.0F;
        }
        return prev + delta * partialTick;
    }
}
