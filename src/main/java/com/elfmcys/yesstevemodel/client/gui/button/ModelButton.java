package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.client.ClientOnlySelection;
import com.elfmcys.yesstevemodel.client.animation.AnimationTracker;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.model.PlayerModelBundle;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.upload.IResourceLocatable;
import com.elfmcys.yesstevemodel.client.upload.UploadManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket;
import com.elfmcys.yesstevemodel.resource.models.Metadata;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmButton;
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import rip.ysm.gpu.Pie;

import java.util.List;
import java.util.Objects;

public class ModelButton extends YsmButton {

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(YesSteveModel.MOD_ID, "texture/icon.png");

    public final boolean isStarred;

    private final int backgroundColor;

    public final ModelAssembly renderContext;

    public final PlayerPreviewEntity modelIdHolder;

    private final boolean disablePreviewRotation;

    private final String targetModelId;

    @Nullable
    private IResourceLocatable backgroundTexture;

    @Nullable
    private IResourceLocatable foregroundTexture;

    @Nullable
    private String cachedLanguage;

    @Nullable
    private List<Component> tooltipLines;

    @Nullable
    private List<Component> detailedTooltipLines;

    private long lastHoverTime;

    // 以下五项在构造期从动画包缓存：目标模型彼时若仍在增量加载（isModelPending=true），
    // 动画包为 null → 动画名退化为 "empty"/0、显示名退化为 "default"（holder 未 init）；
    // 模型加载完成后由 refreshPendingCaches() 补齐，故不能 final
    // （增量刷新不重建卡片，见 PlayerModelScreen.refreshLoadedModelSlots）
    private String modelId;

    private String modelName;

    private String authorName;

    private double animationDuration;

    private Component displayName;

    public ModelButton(int x, int y, boolean isAuthLocked, PlayerPreviewEntity playerPreviewEntity, ModelAssembly textureRegistry) {
        this(x, y, isAuthLocked, playerPreviewEntity, textureRegistry, playerPreviewEntity.getModelId());
    }

    public ModelButton(int x, int y, boolean isAuthLocked, PlayerPreviewEntity playerPreviewEntity, ModelAssembly textureRegistry, String targetModelId) {
        super(x, y, 52, 90, createDisplayName(playerPreviewEntity, textureRegistry), button -> {
        });
        this.targetModelId = targetModelId;
        this.backgroundTexture = null;
        this.foregroundTexture = null;
        this.tooltipLines = null;
        this.detailedTooltipLines = null;
        this.lastHoverTime = -1L;
        this.isStarred = isAuthLocked;
        this.backgroundColor = isAuthLocked ? 2130706432 : -12369342;
        this.renderContext = textureRegistry;
        this.modelIdHolder = playerPreviewEntity;
        this.disablePreviewRotation = textureRegistry.getModelData().getModelProperties().isDisablePreviewRotation();
        this.displayName = YsmText.literal(FileTypeUtil.getNameWithoutArchiveExtension(playerPreviewEntity.getModelId()));
        refreshPendingCaches();
    }

    /**
     * 模型从增量加载态转为已加载后，补齐构造期缺失的缓存（幂等：已加载时重算结果不变）：
     * <ul>
     *   <li>hover/hover_fadeout/focus 动画名与 fadeout 时长——pending 期动画包为 null，曾退化为
     *       "empty"/0（本类构造器旧行为）</li>
     *   <li>gui 前/后景贴图——pending 期 LazyModelAssembly 委托空 displayAssets，get 为 null</li>
     *   <li>SHOW_MODEL_ID_FIRST 模式显示名——pending 期 holder 尚未 initModelWithTexture，
     *       modelId 为 "default"，旧代码在整页重建时借机修正，增量刷新路径在此补齐</li>
     * </ul>
     * 只被增量加载路径调用（结构性刷新走 init() 重建整卡，不走这里）。
     */
    public void refreshPendingCaches() {
        PlayerModelBundle animationBundle = ClientModelManager.isModelPending(this.targetModelId) ? null : this.renderContext.getAnimationBundle();
        if (animationBundle == null) {
            return;
        }
        Object2ReferenceMap<String, Animation> bundles = animationBundle.getMainAnimations();
        if (bundles.containsKey("hover")) {
            this.modelId = "hover";
        } else {
            this.modelId = "empty";
        }
        if (bundles.containsKey("hover_fadeout")) {
            this.modelName = "hover_fadeout";
            this.animationDuration = bundles.get("hover_fadeout").animationLength * 50.0f;
        } else {
            this.modelName = "empty";
            this.animationDuration = 0.0d;
        }
        if (bundles.containsKey("focus")) {
            this.authorName = "focus";
        } else {
            this.authorName = "empty";
        }
        if (this.backgroundTexture == null && this.renderContext.getTextureRegistry().getGuiBackground() != null) {
            this.backgroundTexture = UploadManager.getOrCreateLocatableWithSize(this.renderContext.getTextureRegistry().getGuiBackground(), true, 200);
        }
        if (this.foregroundTexture == null && this.renderContext.getTextureRegistry().getGuiForeground() != null) {
            this.foregroundTexture = UploadManager.getOrCreateLocatableWithSize(this.renderContext.getTextureRegistry().getGuiForeground(), true, 200);
        }
        // 构造期若卡片尚在加载，holder 未 initModelWithTexture、modelId 为 "default"，
        // 显示名曾据此算成 "default"；此处 holder 已就绪，按真实 id 重算
        this.displayName = YsmText.literal(FileTypeUtil.getNameWithoutArchiveExtension(this.modelIdHolder.getModelId()));
    }

    private static MutableComponent createDisplayName(PlayerPreviewEntity previewEntity, ModelAssembly modelAssembly) {
        Metadata metadata2 = modelAssembly.getModelData().getExtraInfo();
        if (metadata2 == null || StringUtils.isBlank(metadata2.getName())) {
            return YsmText.literal(FileTypeUtil.getNameWithoutArchiveExtension(previewEntity.getModelId()));
        }
        return YsmText.literal(ModelMetadataPresenter.getLocalizedModelString(modelAssembly, "metadata.name", metadata2.getName()));
    }

    public Component getMessage() {
        if (GeneralConfig.SHOW_MODEL_ID_FIRST.get().booleanValue()) {
            return this.displayName;
        }
        return super.getMessage();
    }

    public void onPress() {
        LocalPlayer localPlayer;
        if (ClientModelManager.isModelPending(this.targetModelId)) {
            return;
        }
        if (!this.isStarred && (localPlayer = Minecraft.getInstance().player) != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                if (NetworkHandler.isClientConnected() && !ClientOnlyMode.isForced()) {
                    if (cap.hasMolangVars(this.modelIdHolder.getModelAssembly().getModelData().getHashId())) {
                        cap.initModelWithTexture(this.modelIdHolder.getModelId(), this.modelIdHolder.getCurrentTextureName());
                        NetworkHandler.sendToServer(new C2SRequestSwitchModelPacket(cap.getModelId(), cap.getCurrentTextureName()));
                        return;
                    } else {
                        NetworkHandler.sendToServer(new C2SRequestSwitchModelPacket(this.modelIdHolder.getModelId(), this.modelIdHolder.getCurrentTextureName()));
                        return;
                    }
                }
                cap.initModelWithTexture(this.modelIdHolder.getModelId(), this.modelIdHolder.getCurrentTextureName());
                ClientOnlySelection.save(this.modelIdHolder.getModelId(), this.modelIdHolder.getCurrentTextureName());
            });
        }
    }

    //? if >=1.20 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    @Override
    public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        AnimationTracker c0117x8455a741Mo1262xaffeef43 = this.modelIdHolder.getAnimationStateMachine();
        if (/*? if <1.17 {*/ /*isHovered()*//*?} else {*/ isHoveredOrFocused() /*?}*/) {
            this.lastHoverTime = Util.getMillis();
            c0117x8455a741Mo1262xaffeef43.setPreviousAnimation(this.modelId);
        } else if (Util.getMillis() - this.lastHoverTime < this.animationDuration) {
            c0117x8455a741Mo1262xaffeef43.setPreviousAnimation(this.modelName);
        } else {
            c0117x8455a741Mo1262xaffeef43.setPreviousAnimation("empty");
        }
        if (isFocused()) {
            c0117x8455a741Mo1262xaffeef43.setQueuedAnimation(this.authorName);
        } else {
            c0117x8455a741Mo1262xaffeef43.setQueuedAnimation("empty");
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int x = getX();
        int y = getY();
        guiGraphics.fillGradient(x, y, x + this.width, y + this.height, this.backgroundColor, this.backgroundColor);
        if (this.backgroundTexture != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(this.backgroundTexture.getResourceLocation().get(), x, y, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
            RenderSystem.disableBlend();
        }
        if (ClientModelManager.isModelPending(this.targetModelId)) {
            drawLoading(guiGraphics, x + (this.width / 2.0f), y + ((this.height - 20) / 2.0f), 8.0f);
        } else {
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            RenderSystem.enableScissor((int) (x * guiScale), (int) (Minecraft.getInstance().getWindow().getHeight() - (((y + this.height) - 20) * guiScale)), (int) (this.width * guiScale), (int) ((this.height - 20) * guiScale));
            ModelPreviewRenderer.renderLivingEntityPreview(x + (this.width / 2.0f), y + (this.height / 2.0f) + 20.0f, 30.0f, minecraft.getFrameTime(), this.modelIdHolder, RendererManager.getPlayerRenderer(), this.disablePreviewRotation, true);
            RenderSystem.disableScissor();
        }
        int starZ = 3500;
        if (this.foregroundTexture != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(this.foregroundTexture.getResourceLocation().get(), x, y, 3500, 0, 0, this.width, this.height, this.width, this.height);
            RenderSystem.disableBlend();
        }
        List listSplit = font.split(getMessage(), 45);
        if (listSplit.size() > 1) {
            guiGraphics.drawCenteredString(font, (FormattedCharSequence) listSplit.get(0), x + (this.width / 2), (y + this.height) - 19, 15986656);
            guiGraphics.drawCenteredString(font, (FormattedCharSequence) listSplit.get(1), x + (this.width / 2), (y + this.height) - 10, 15986656);
        } else {
            guiGraphics.drawCenteredString(font, getMessage(), x + (this.width / 2), (y + this.height) - 15, 15986656);
        }
        if (!this.isStarred && hoveredOrFocused()) {
            guiGraphics.fillGradient(x, y + 1, x + 1, (y + this.height) - 1, 3500, -790560, -790560);
            guiGraphics.fillGradient(x, y, x + this.width, y + 1, 3500, -790560, -790560);
            guiGraphics.fillGradient((x + this.width) - 1, y + 1, x + this.width, (y + this.height) - 1, 3500, -790560, -790560);
            guiGraphics.fillGradient(x, (y + this.height) - 1, x + this.width, y + this.height, 3500, -790560, -790560);
        }
        if (this.isStarred) {
            guiGraphics.fillGradient(x, y, x + this.width, y + this.height, 3500, -1625152990, -1625152990);
        }
        if (minecraft.player != null) {
            StarModelsCapability.get(minecraft.player).ifPresent(cap -> {
                if (cap.containsModel(this.modelIdHolder.getModelId())) {
                    guiGraphics.blit(ICON_TEXTURE, (x + this.width) - 14, y, starZ, 0, 0, 16, 16, 256, 256);
                }
            });
        }
    }

    public static void drawLoading(YsmGui guiGraphics, float centerX, float centerY, float radius) {
        float thickness = Math.max(1.5f, radius * 0.28f);
        float inner = radius - thickness;
        float time = (System.nanoTime() % 10_000_000_000L) / 1.0E9f;

        Pie.draw(guiGraphics.pose(), centerX, centerY, inner, radius, 0.0f, Pie.tau, 0x33FFFFFF);

        float sweepPhase = (time % 2.0f) / 2.0f;
        float eased = 0.5f - 0.5f * Mth.cos(sweepPhase * Pie.tau);
        float sweep = Mth.lerp(eased, 0.12f, 0.78f) * Pie.tau;
        float start = ((time % 1.4f) / 1.4f) * Pie.tau + sweepPhase * Pie.tau;

        Pie.draw(guiGraphics.pose(), centerX, centerY, inner, radius, start, start + sweep, 0xFFF3D08A);
    }

    public void renderTooltip(YsmGui guiGraphics, Screen screen, int mouseX, int mouseY) {
        if (/*? if <1.17 {*/ /*isHovered()*//*?} else {*/ isHoveredOrFocused() /*?}*/) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, 0.0f, 4000.0f);
            //? if <1.17
        /*String selected = Minecraft.getInstance().getLanguageManager().getSelected().getCode();*/
        //? if >=1.17
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
            if (!Objects.equals(this.cachedLanguage, selected)) {
                this.cachedLanguage = selected;
                this.detailedTooltipLines = null;
                this.tooltipLines = null;
            }
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344)) {
                if (this.detailedTooltipLines == null) {
                    this.detailedTooltipLines = ModelMetadataPresenter.buildModelTooltip(this.renderContext, selected, this.modelIdHolder.getModelId(), true);
                }
                guiGraphics.renderScreenComponentTooltip(screen, Minecraft.getInstance().font, this.detailedTooltipLines, mouseX, mouseY);
            } else {
                if (this.tooltipLines == null) {
                    this.tooltipLines = ModelMetadataPresenter.buildModelTooltip(this.renderContext, selected, this.modelIdHolder.getModelId(), false);
                }
                guiGraphics.renderScreenComponentTooltip(screen, Minecraft.getInstance().font, this.tooltipLines, mouseX, mouseY);
            }
            guiGraphics.pose().popPose();
        }
    }

    public boolean clicked(double mouseX, double mouseY) {
        return !this.isStarred && super.clicked(mouseX, mouseY);
    }
}