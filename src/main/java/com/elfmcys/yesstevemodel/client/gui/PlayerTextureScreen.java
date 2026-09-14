package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.button.IconButton;
import com.elfmcys.yesstevemodel.client.gui.button.TextureButton;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
//? if >=21.9 {
/*import net.minecraft.client.input.MouseButtonEvent;
 *///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
//? if >=1.21 {
/*import com.elfmcys.yesstevemodel.util.YsmFrame;*/
//?}

public class PlayerTextureScreen extends Screen {

    private static final String HIDDEN_PREFIX = "——";

    private static final float MAX_ZOOM = 360.0f;

    private static final float MIN_ZOOM = 18.0f;

    private static final float MAX_PITCH = 90.0f;

    private static final float MIN_PITCH = -90.0f;

    private static final PlayerPreviewEntity[] texturePreviewHolders = new PlayerPreviewEntity[4];

    private static final int LEFT_MOUSE_BUTTON = 0;

    private static final int RIGHT_MOUSE_BUTTON = 1;

    public final PlayerPreviewEntity modelHolder;

    public final ModelAssembly renderContext;

    private final PlayerModelScreen parentScreen;

    private final String modelId;

    private final OrderedStringMap<String, ? extends AbstractTexture> textureMap;

    private final List<String> animationKeys;

    private String currentAnimation;

    private int textureMaxPage;

    private int textureCurrentPage;

    private int animationMaxPage;

    private int animationCurrentPage;

    public int guiLeft;

    public int guiTop;

    public float offsetX;

    public float offsetY;

    public float zoom;

    public float yaw;

    public float pitch;

    public boolean showGround;

    static {
        for (int i = 0; i < texturePreviewHolders.length; i++) {
            texturePreviewHolders[i] = new PlayerPreviewEntity();
        }
    }

    public PlayerTextureScreen(PlayerModelScreen modelScreen, String str, ModelAssembly modelAssembly) {
        super(YsmText.literal("Player Texture GUI"));
        this.currentAnimation = StringPool.EMPTY;
        this.offsetX = 0.0f;
        this.offsetY = -60.0f;
        this.zoom = 80.0f;
        this.yaw = 165.0f;
        this.pitch = -5.0f;
        this.showGround = true;
        this.modelHolder = new PlayerPreviewEntity();
        for (PlayerPreviewEntity c0685xf513e8bf : texturePreviewHolders) {
            c0685xf513e8bf.resetModel();
            c0685xf513e8bf.getAnimationStateMachine().setCurrentAnimation("idle");
        }
        this.parentScreen = modelScreen;
        this.modelId = str;
        this.renderContext = modelAssembly;
        this.textureMap = modelAssembly.getAnimationBundle().getTextures();
        this.animationKeys = new ArrayList(modelAssembly.getAnimationBundle().getMainAnimations().keySet());
        this.animationKeys.removeIf(str2 -> {
            return str2.startsWith(HIDDEN_PREFIX);
        });
        this.animationKeys.sort((v0, v1) -> {
            return v0.compareTo(v1);
        });
    }

    public TextureButton createTextureButton(int x, int y, PlayerPreviewEntity previewEntity, int textureIndex) {
        return new TextureButton(x, y, previewEntity, this.renderContext);
    }

    public void init() {
        int texIndex;
        int animIndex;
        MutableComponent mutableComponentLiteral;
// 1.16.5 无 Screen.clearWidgets（死注释形态的 init(mc,w,h)+return 从未生效，1.16.5 字段
// 由 setScreen→init(Minecraft,w,h) 先行赋值，无参 init() 直接可用，m2.6 实测语义）
//? if >=1.17 {
        clearWidgets();
//?}
        this.guiLeft = (this.width - 420) / 2;
        this.guiTop = (this.height - 235) / 2;
        this.textureMaxPage = (this.textureMap.size() - 1) / 4;
        this.animationMaxPage = (this.animationKeys.size() - 1) / 11;
        if (this.textureCurrentPage > this.textureMaxPage) {
            this.textureCurrentPage = 0;
        }
        if (this.animationCurrentPage > this.animationMaxPage) {
            this.animationCurrentPage = 0;
        }
        ysmAddWidget(new FlatColorButton(this.guiLeft + 5, this.guiTop, 80, 18, YsmText.translatable("gui.yes_steve_model.model.return"), button -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
        }));
        ysmAddWidget(new IconButton(this.guiLeft + 281, this.guiTop + 2, 16, 16, 64, 16, button2 -> {
            this.currentAnimation = "idle";
        }).setTooltipText("gui.yes_steve_model.model.stop"));
        ysmAddWidget(new IconButton(this.guiLeft + 263, this.guiTop + 2, 16, 16, 48, 16, button3 -> {
            this.offsetX = 0.0f;
            this.offsetY = -60.0f;
            this.zoom = 80.0f;
            this.yaw = 165.0f;
            this.pitch = -5.0f;
        }).setTooltipText("gui.yes_steve_model.model.reset"));
        ysmAddWidget(new IconButton(this.guiLeft + 245, this.guiTop + 2, 16, 16, 64, 0, button4 -> {
            this.showGround = !this.showGround;
        }).setTooltipText("gui.yes_steve_model.model.ground"));
        ysmAddWidget(new FlatColorButton(this.guiLeft + 321, this.guiTop + 213, 18, 18, YsmText.literal("<"), button5 -> {
            if (this.textureCurrentPage > 0) {
                this.textureCurrentPage--;
                init();
            }
        }));
        ysmAddWidget(new FlatColorButton(this.guiLeft + 383, this.guiTop + 213, 18, 18, YsmText.literal(">"), button6 -> {
            if (this.textureCurrentPage < this.textureMaxPage) {
                this.textureCurrentPage++;
                init();
            }
        }));
        ysmAddWidget(new FlatColorButton(this.guiLeft + 11, this.guiTop + 214, 16, 16, YsmText.literal("<"), button7 -> {
            if (this.animationCurrentPage > 0) {
                this.animationCurrentPage--;
                init();
            }
        }));
        ysmAddWidget(new FlatColorButton(this.guiLeft + 63, this.guiTop + 214, 16, 16, YsmText.literal(">"), button8 -> {
            if (this.animationCurrentPage < this.animationMaxPage) {
                this.animationCurrentPage++;
                init();
            }
        }));
        for (int animSlot = 0; animSlot < 11 && (animIndex = animSlot + (this.animationCurrentPage * 11)) < this.animationKeys.size(); animSlot++) {
            String str = this.animationKeys.get(animIndex);
            int animButtonY = this.guiTop + 27 + (17 * animSlot);
            String str2 = String.format("gui.yes_steve_model.texture.button.%s", str.replaceAll("\\:", "."));
            String str3 = String.format("gui.yes_steve_model.texture.button.%s.desc", str.replaceAll("\\:", "."));
            if (I18n.exists(str2)) {
                mutableComponentLiteral = YsmText.translatable(str2);
            } else {
                mutableComponentLiteral = YsmText.literal(str);
            }
            FlatColorButton colorButton = new FlatColorButton(this.guiLeft + 5, animButtonY, 80, 16, mutableComponentLiteral, button9 -> {
                this.currentAnimation = str;
            });
            if (I18n.exists(str3)) {
                colorButton.setTooltipLines(Lists.newArrayList(new Component[]{YsmText.translatable(str3).withStyle(ChatFormatting.GOLD), YsmText.translatable("gui.yes_steve_model.texture.button.animation_name", str).withStyle(ChatFormatting.GRAY)}));
            }
            ysmAddWidget(colorButton);
        }
        for (int texSlot = 0; texSlot < 4 && (texIndex = texSlot + (this.textureCurrentPage * 4)) < this.textureMap.size(); texSlot++) {
            int texButtonX = this.guiLeft + 306 + (56 * (texSlot % 2));
            int texButtonY = this.guiTop + 5 + (104 * (texSlot / 2));
            PlayerPreviewEntity previewEntity = texturePreviewHolders[texSlot];
            previewEntity.initModelWithTexture(this.modelId, this.textureMap.getKeyAt(texIndex));
            ysmAddWidget(createTextureButton(texButtonX, texButtonY, previewEntity, texIndex));
        }
    }

    //? if >=1.20 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(graphics), mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.render(new YsmGui(poseStack), mouseX, mouseY, partialTick);
    }
     *///?}

    public void render(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        guiGraphics.renderScreenBackground(this);
        guiGraphics.fillGradient(this.guiLeft, this.guiTop + 22, this.guiLeft + 90, this.guiTop + 235, -14540254, -14540254);
        guiGraphics.fillGradient(this.guiLeft + 93, this.guiTop, this.guiLeft + 299, this.guiTop + 235, -14540254, -14540254);
        guiGraphics.fillGradient(this.guiLeft + 302, this.guiTop, this.guiLeft + 420, this.guiTop + 235, -14540254, -14540254);
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int scissorX = (int) ((this.guiLeft + 93) * guiScale);
        int height = (int) (Minecraft.getInstance().getWindow().getHeight() - ((this.guiTop + 235) * guiScale));
        int scissorWidth = (int) (206.0d * guiScale);
        int scissorHeight = (int) (235.0d * guiScale);
        if (!this.modelHolder.getAnimationStateMachine().isCurrentAnimation(this.currentAnimation)) {
            this.modelHolder.getAnimationStateMachine().setCurrentAnimation(this.currentAnimation);
        }
        //? if >=1.21
        /*renderTexturePreview(guiGraphics, scissorX, height, scissorWidth, scissorHeight, YsmFrame.partialTick(this.minecraft));*/
        //? if <1.21
        renderTexturePreview(guiGraphics, scissorX, height, scissorWidth, scissorHeight, this.minecraft.getFrameTime());
        String str = String.format("%d/%d", this.textureCurrentPage + 1, this.textureMaxPage + 1);
        Font font = this.font;
        int iWidth = this.guiLeft + 302 + ((118 - this.font.width(str)) / 2);
        int pageY = this.guiTop + 223;
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(font, str, iWidth, pageY - (9 / 2), 15986656);
        String str2 = String.format("%d/%d", this.animationCurrentPage + 1, this.animationMaxPage + 1);
        guiGraphics.drawString(this.font, str2, this.guiLeft + 5 + ((80 - this.font.width(str2)) / 2), this.guiTop + 218, 15986656);
        //? if <1.17
        /*super.render(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.17 && <1.20
        /*super.render(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
        //? if >=1.20
        super.render(guiGraphics.graphics(), mouseX, mouseY, partialTick);
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable -> {
            return renderable instanceof FlatColorButton;
        }).forEach(renderable2 -> {
            ((FlatColorButton) renderable2).renderTooltip(guiGraphics, this, mouseX, mouseY);
        });
    }

    public void renderTexturePreview(YsmGui guiGraphics, int scissorX, int scissorY, int scissorWidth, int scissorHeight, float partialTick) {
        // 1.21.6+ RenderSystem.enableScissor 删 → GuiGraphics 剪裁（GUI 坐标=窗口坐标/guiScale 翻转 Y）
        // scissor 系 API vanilla 1.16.4 才有（javap 实证）→ <1.16.4 走 GL11 直调
        //? if <1.16.4 {
        /*org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);*/
        //?}
        //? if >=1.16.4 && <21.6
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        //? if >=21.6 {
        /*double ysmGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int ysmWinH = Minecraft.getInstance().getWindow().getHeight();
        guiGraphics.graphics().enableScissor(
            (int) (scissorX / ysmGuiScale),
            (int) ((ysmWinH - (scissorY + scissorHeight)) / ysmGuiScale),
            (int) ((scissorX + scissorWidth) / ysmGuiScale),
            (int) ((ysmWinH - scissorY) / ysmGuiScale));*/
        //?}
        PlayerCapability.get(this.minecraft.player).ifPresent(cap -> {
            this.modelHolder.initModelWithTexture(this.modelId, cap.getCurrentTextureName());
            ModelPreviewRenderer.renderEntityPreview(this.guiLeft + 149.5f + 40.0f + this.offsetX, this.guiTop + 117.5f + 80.0f + this.offsetY, this.zoom, this.pitch, this.yaw, partialTick, this.modelHolder, RendererManager.getPlayerRenderer(), this.showGround);
        });
        //? if <1.16.4
        /*org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);*/
        //? if >=1.16.4 && <21.6
        RenderSystem.disableScissor();
        //? if >=21.6
        /*guiGraphics.graphics().disableScissor();*/
    }

    // 1.21.9+ 输入事件对象化（mouseDragged(MouseButtonEvent,double,double)）
    //? if >=21.9 {
    /*public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.minecraft == null || !isInPreviewArea(mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            this.yaw = (float) (this.yaw + (1.5d * dragX));
            adjustPitch((float) dragY);
        }
        if (button == 1) {
            this.offsetX = (float) (this.offsetX + dragX);
            this.offsetY = (float) (this.offsetY + dragY);
            return true;
        }
        return true;
    }
    *///?}
    //? if <21.9 {
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.minecraft == null || !isInPreviewArea(mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            this.yaw = (float) (this.yaw + (1.5d * dragX));
            adjustPitch((float) dragY);
        }
        if (button == 1) {
            this.offsetX = (float) (this.offsetX + dragX);
            this.offsetY = (float) (this.offsetY + dragY);
            return true;
        }
        return true;
    }
    //?}

    //? if neoforge
/*public boolean mouseScrolled(double mouseX, double mouseY, double delta, double scrollY) {*/
//? if forge
public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.minecraft == null) {
            return false;
        }
        if (delta != 0.0d) {
            if (isInPreviewArea(mouseX, mouseY)) {
                adjustZoom(((float) delta) * 0.07f);
                return true;
            }
            if (isInAnimationArea(mouseX, mouseY)) {
                return scrollAnimationPage(delta);
            }
            if (isInTextureArea(mouseX, mouseY)) {
                return scrollTexturePage(delta);
            }
        }
        //? if neoforge
/*return super.mouseScrolled(mouseX, mouseY, delta, delta);*/
//? if forge
return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean scrollTexturePage(double delta) {
        if (delta > 0.0d && this.textureCurrentPage > 0) {
            this.textureCurrentPage--;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
        }
        if (delta < 0.0d && this.textureCurrentPage < this.textureMaxPage) {
            this.textureCurrentPage++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
            return true;
        }
        return true;
    }

    private boolean scrollAnimationPage(double delta) {
        if (delta > 0.0d && this.animationCurrentPage > 0) {
            this.animationCurrentPage--;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
        }
        if (delta < 0.0d && this.animationCurrentPage < this.animationMaxPage) {
            this.animationCurrentPage++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
            return true;
        }
        return true;
    }

    private boolean isInPreviewArea(double mouseX, double mouseY) {
        return ((((double) (this.guiLeft + 93)) > mouseX ? 1 : (((double) (this.guiLeft + 93)) == mouseX ? 0 : -1)) < 0 && (mouseX > ((double) (this.guiLeft + 299)) ? 1 : (mouseX == ((double) (this.guiLeft + 299)) ? 0 : -1)) < 0) && ((((double) this.guiTop) > mouseY ? 1 : (((double) this.guiTop) == mouseY ? 0 : -1)) < 0 && (mouseY > ((double) (this.guiTop + 235)) ? 1 : (mouseY == ((double) (this.guiTop + 235)) ? 0 : -1)) < 0);
    }

    private boolean isInAnimationArea(double mouseX, double mouseY) {
        return ((((double) this.guiLeft) > mouseX ? 1 : (((double) this.guiLeft) == mouseX ? 0 : -1)) < 0 && (mouseX > ((double) (this.guiLeft + 90)) ? 1 : (mouseX == ((double) (this.guiLeft + 90)) ? 0 : -1)) < 0) && ((((double) (this.guiTop + 22)) > mouseY ? 1 : (((double) (this.guiTop + 22)) == mouseY ? 0 : -1)) < 0 && (mouseY > ((double) (this.guiTop + 235)) ? 1 : (mouseY == ((double) (this.guiTop + 235)) ? 0 : -1)) < 0);
    }

    private boolean isInTextureArea(double mouseX, double mouseY) {
        return ((((double) (this.guiLeft + 302)) > mouseX ? 1 : (((double) (this.guiLeft + 302)) == mouseX ? 0 : -1)) < 0 && (mouseX > ((double) (this.guiLeft + 420)) ? 1 : (mouseX == ((double) (this.guiLeft + 420)) ? 0 : -1)) < 0) && ((((double) this.guiTop) > mouseY ? 1 : (((double) this.guiTop) == mouseY ? 0 : -1)) < 0 && (mouseY > ((double) (this.guiTop + 235)) ? 1 : (mouseY == ((double) (this.guiTop + 235)) ? 0 : -1)) < 0);
    }

    private void adjustPitch(float deltaY) {
        if (this.pitch - deltaY > MAX_PITCH) {
            this.pitch = MAX_PITCH;
        } else if (this.pitch - deltaY < MIN_PITCH) {
            this.pitch = MIN_PITCH;
        } else {
            this.pitch -= deltaY;
        }
    }

    private void adjustZoom(float zoomDelta) {
        this.zoom = Mth.clamp(this.zoom + (zoomDelta * this.zoom), MIN_ZOOM, MAX_ZOOM);
    }

    public boolean isPauseScreen() {
        return false;
    }
    // addRenderableWidget/addWidget 均为 protected 实例方法（JLS 6.6.2 子类内才可调）→ 桥方法；
    // 泛型返回保持原 addRenderableWidget 的链式取回语义（如 .setTooltipText 续链）
    //? if <1.17 {
    /*private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractButton) {
            this.addButton((net.minecraft.client.gui.components.AbstractButton) widget);
        } else {
            this.addWidget(widget);
            ((com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor) this).ysm$getRenderables().add(widget);
        }
        return widget;
    }
     *///?} else {
    private <T extends net.minecraft.client.gui.components.AbstractWidget> T ysmAddWidget(T widget) {
        this.addRenderableWidget(widget);
        return widget;
    }
    //?}

}