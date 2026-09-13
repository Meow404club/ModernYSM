package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.event.AnimationLockEvent;
import com.elfmcys.yesstevemodel.client.gui.button.AnimationSlider;
import com.elfmcys.yesstevemodel.client.gui.button.ConfigCheckBox;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.button.FlatIconButton;
import com.elfmcys.yesstevemodel.client.gui.custom.AbstractConfig;
import com.elfmcys.yesstevemodel.client.gui.custom.ExtraAnimationButtons;
import com.elfmcys.yesstevemodel.client.gui.custom.configs.CheckboxConfig;
import com.elfmcys.yesstevemodel.client.gui.custom.configs.RadioConfig;
import com.elfmcys.yesstevemodel.client.gui.custom.configs.RangeConfig;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SPlayAnimationPacket;
import com.elfmcys.yesstevemodel.network.message.C2SRequestExecuteMolangPacket;
import com.elfmcys.yesstevemodel.resource.models.ModelProperties;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import rip.ysm.gui.YsmGui;
//? if >=1.19.4 {
import net.minecraft.client.gui.components.Renderable;
//?} else {
/*import net.minecraft.client.gui.components.Widget;
 *///?}
//? if >=1.19.4 {
import net.minecraft.client.gui.components.Tooltip;
//?}
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if <1.19.4 {
/*import com.mojang.math.Matrix4f;
 *///?}
//? if >=1.19.4 {
import org.joml.Matrix4f;
//?}
import rip.ysm.api.client.KeyMappingFactory;

import java.util.*;
import java.util.function.Consumer;

public class AnimationRouletteScreen extends Screen {

    private static final String SUBMENU_PREFIX = "#";

    private static final String RETURN_KEY = "#return";

    private static final String CONFIG_TITLE_FORMAT = "properties.extra_animation_buttons.%s.config_forms.%d.title";

    private static final String CONFIG_DESC_FORMAT = "properties.extra_animation_buttons.%s.config_forms.%d.description";

    private static final String CONFIG_LABEL_FORMAT = "properties.extra_animation_buttons.%s.config_forms.%d.labels.%d";

    private static final LinkedList<Pair<String, Integer>> navigationStack = Lists.newLinkedList();

    private static String lastModelId = StringPool.EMPTY;

    private int centerX;

    private int centerY;

    private int hoveredIndex;

    private int hoveredConfigIndex;

    private ExtraAnimationButtons currentConfigGroup;

    private Pair<String, Integer> currentNavEntry;

    private int configScrollOffset;

    private int maxConfigScroll;

    @Nullable
    private FlatColorButton scrollUpButton;

    @Nullable
    private FlatColorButton scrollDownButton;

    private final OrderedStringMap<String, String> currentProperties;

    private final Map<String, ExtraAnimationButtons> renderGroups;

    private final Map<String, OrderedStringMap<String, String>> textProperties;

    private final ModelProperties timingConfig;

    private final AnimatableEntity<?> animatableModel;

    private final ModelAssembly renderContext;

    public AnimationRouletteScreen(Map<String, ExtraAnimationButtons> map, Map<String, OrderedStringMap<String, String>> map2, ModelAssembly modelAssembly, AnimatableEntity<?> animatableEntity) {
        super(YsmText.literal("Animation Roulette GUI"));
        this.hoveredIndex = -1;
        this.hoveredConfigIndex = -1;
        this.currentConfigGroup = null;
        this.configScrollOffset = 0;
        this.maxConfigScroll = 0;
        this.renderContext = modelAssembly;
        this.timingConfig = modelAssembly.getModelData().getModelProperties();
        this.animatableModel = animatableEntity;
        this.textProperties = map2;
        this.renderGroups = map;
        this.currentNavEntry = navigationStack.peekLast();
        if (this.currentNavEntry != null && this.textProperties.containsKey(this.currentNavEntry.getLeft())) {
            this.currentProperties = this.textProperties.get(this.currentNavEntry.getLeft());
            return;
        }
        this.currentProperties = this.timingConfig.getExtraAnimation();
        navigationStack.clear();
        navigationStack.add(MutablePair.of(StringPool.EMPTY, Integer.valueOf(this.currentNavEntry == null ? 0 : this.currentNavEntry.getRight().intValue())));
        this.currentNavEntry = navigationStack.peekLast();
    }

    public AnimationRouletteScreen(String str, ModelAssembly modelAssembly, AnimatableEntity<?> animatableEntity) {
        super(YsmText.literal("Animation Roulette GUI"));
        this.hoveredIndex = -1;
        this.hoveredConfigIndex = -1;
        this.currentConfigGroup = null;
        this.configScrollOffset = 0;
        this.maxConfigScroll = 0;
        this.renderContext = modelAssembly;
        this.timingConfig = modelAssembly.getModelData().getModelProperties();
        this.animatableModel = animatableEntity;
        this.textProperties = this.timingConfig.getExtraAnimationClassify();
        this.renderGroups = this.timingConfig.getExtraAnimationButtons();
        if (!lastModelId.equals(str)) {
            navigationStack.clear();
            lastModelId = str;
        }
        if (navigationStack.isEmpty()) {
            navigationStack.add(MutablePair.of(StringPool.EMPTY, 0));
        }
        this.currentNavEntry = navigationStack.peekLast();
        if (this.textProperties.containsKey(this.currentNavEntry.getLeft())) {
            this.currentProperties = this.textProperties.get(this.currentNavEntry.getLeft());
            return;
        }
        this.currentProperties = this.timingConfig.getExtraAnimation();
        navigationStack.clear();
        navigationStack.add(MutablePair.of(StringPool.EMPTY, this.currentNavEntry.getRight()));
        this.currentNavEntry = navigationStack.peekLast();
    }

    public void init() {
        // 修复 s2（经典轮盘 1.16.5 界面堆积）：本屏 init() 会被原地重入——单选/复选回调 init()、
        // showConfigGroup 调 init()——vanilla 只在 init(mc,w,h) 清场，1.16.5 无 clearWidgets()
        // → FlatColorButton/ConfigCheckBox/AnimationSlider 每次重入整层累积（现代版无 init 纯逐帧
        // 绘制故正常，用户实报「现代版正常、经典版不正常」即此根因）。
        // 对齐 vanilla init(mc,w,h) 清场三连（vanilla-mc-1165 Screen.java:302-305）。
        // 必须块形式条件+/* */包裹（活跃分支被解包为真代码，PlayerModelScreen.render 同款实证）；
        // 不可用「行条件+this.init(mc,w,h);return;」写法：m2-compile-green 实测展开后 return 悬中段不可达。
        //? if <1.17 {
        /*this.buttons.clear();
        this.children.clear();
        this.setFocused(null);
         *///?} else {
        clearWidgets();
        //?}
        this.centerX = (this.width / 2) - 70;
        this.centerY = (this.height / 2) - 8;
        if (this.currentProperties.size() < (this.currentNavEntry.getRight().intValue() * 8) + 1) {
            this.currentNavEntry.setValue(0);
        }
        if (this.currentProperties.size() <= this.hoveredIndex) {
            this.hoveredIndex = 0;
        }
        if (this.animatableModel.getEntity() instanceof Player) {
            ysmAddWidget(new FlatColorButton(this.centerX - 20, this.centerY - 10, 40, 20, YsmText.literal(""), button -> {
                AnimationLockEvent.toggleLock();
            }) {
                @NotNull
                public Component getMessage() {
                    if (AnimationLockEvent.isLocked()) {
                        return YsmText.translatable("gui.yes_steve_model.roulette.lock_on");
                    }
                    return YsmText.translatable("gui.yes_steve_model.roulette.lock_off");
                }
            });
        } else {
            ysmAddWidget(new FlatColorButton(this.centerX - 20, this.centerY - 10, 40, 20, YsmText.translatable("gui.yes_steve_model.roulette.stop"), button2 -> {
                NetworkHandler.sendToServer(C2SPlayAnimationPacket.createWithIndex(this.animatableModel.getEntity().getId()));
                onClose();
            }));
        }
        ysmAddWidget(new FlatColorButton(this.centerX + 125, this.centerY - 102, 30, 30, YsmText.literal("<"), button3 -> {
            previousPage();
        }));
        ysmAddWidget(new FlatColorButton(this.centerX + 240, this.centerY - 102, 30, 30, YsmText.literal(">"), button4 -> {
            nextPage();
        }));
        ysmAddWidget(new FlatColorButton(this.centerX + 125, this.centerY - 70, 145, 22, YsmText.translatable("gui.yes_steve_model.model.return"), button5 -> {
            navigateBack();
        }));
        if (this.currentConfigGroup != null) {
            this.scrollUpButton = new FlatColorButton(this.centerX + 242, this.centerY - 46, 28, 60, YsmText.literal("↑"), button6 -> {
                scrollConfigUp(50);
                if (this.configScrollOffset == 0 && this.scrollUpButton != null) {
                    this.scrollUpButton.active = false;
                }
                if (this.scrollDownButton != null) {
                    this.scrollDownButton.active = true;
                }
            });
            this.scrollDownButton = new FlatColorButton(this.centerX + 242, this.centerY + 50, 28, 60, YsmText.literal("↓"), button7 -> {
                scrollConfigDown(50);
                if (this.configScrollOffset == this.maxConfigScroll && this.scrollDownButton != null) {
                    this.scrollDownButton.active = false;
                }
                if (this.scrollUpButton != null) {
                    this.scrollUpButton.active = true;
                }
            });
            ysmAddWidget(this.scrollUpButton);
            ysmAddWidget(this.scrollDownButton);
            int[] iArr = {-46};
            int[] iArr2 = {0};
            for (AbstractConfig config : this.currentConfigGroup.getConfigForms()) {
                renderConfigFormItem(config, iArr, iArr2);
            }
        }
    }

    private void renderConfigFormItem(AbstractConfig abstractConfig, int[] iArr, int[] iArr2) {
        if (abstractConfig instanceof CheckboxConfig) {
            CheckboxConfig config = (CheckboxConfig) abstractConfig;
            executeExpression(abstractConfig.getValue(), str -> {
                this.minecraft.execute(() -> {
                    ysmAddWidget(createCheckbox(config, str, iArr, iArr2));
                    iArr[0] = iArr[0] + 14;
                    iArr2[0] = iArr2[0] + 1;
                    this.maxConfigScroll = Math.max(0, iArr[0] - 110);
                });
            });
        }
        if (abstractConfig instanceof RangeConfig) {
            RangeConfig config = (RangeConfig) abstractConfig;
            executeExpression(abstractConfig.getValue(), str2 -> {
                this.minecraft.execute(() -> {
                    ysmAddWidget(createSlider(config, str2, iArr, iArr2));
                    iArr[0] = iArr[0] + 17;
                    iArr2[0] = iArr2[0] + 1;
                    this.maxConfigScroll = Math.max(0, iArr[0] - 110);
                });
            });
        }
        if (abstractConfig instanceof RadioConfig) {
            RadioConfig config = (RadioConfig) abstractConfig;
            executeExpression(abstractConfig.getValue(), str3 -> {
                this.minecraft.execute(() -> {
                    renderRadioGroup(config, str3, iArr, iArr2);
                });
            });
        }
    }

    private void renderRadioGroup(RadioConfig radioConfig, String str, int[] iArr, int[] iArr2) {
        int iRound = Math.round(parseFloatValue(str));
        OrderedStringMap<String, String> orderedStringMap = radioConfig.getLabels();
        if (iRound < 0 || orderedStringMap.size() < iRound) {
            iRound = 0;
        }
        int iMax = 0;
        int i = 0;
        Iterator<String> it = orderedStringMap.getKeys().iterator();
        while (it.hasNext()) {
            iMax = Math.max(iMax, this.font.width(ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_LABEL_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0]), Integer.valueOf(i)), it.next())) + 16);
            i++;
        }
        if (iMax == 0) {
            iMax = 115;
        }
        int iMax2 = Math.max(1, 115 / iMax);
        String str2 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_TITLE_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), radioConfig.getTitle());
        String str3 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_DESC_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), radioConfig.getDescription());
        MutableComponent mutableComponentLiteral = YsmText.literal(str2);
        // Tooltip（1.17+）：<1.17 悬浮说明降级不展示（配置项 hover 描述，功能差记回报）
        //? if >=1.19.4 {
        Tooltip tooltipCreate = Tooltip.create(YsmText.literal(str3));
        //?}
        int size = ((((orderedStringMap.size() - 1) / iMax2) + 1) * 14) + 14;
        FlatIconButton iconButton = new FlatIconButton(this.centerX + 125, this.centerY + iArr[0], size, mutableComponentLiteral);
        //? if >=1.19.4 {
        iconButton.setTooltip(tooltipCreate);
        addRenderableOnly(iconButton);
        //?} else {
        /*ysmAddWidget(iconButton);
         *///?}
        int rowY = iArr[0] + 14;
        int idx = 0;
        while (idx < orderedStringMap.size()) {
            MutableComponent mutableComponentLiteral2 = YsmText.literal(ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_LABEL_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0]), Integer.valueOf(idx)), orderedStringMap.getKeyAt(idx)));
            String str4 = orderedStringMap.getValueAt(idx);
            boolean isSelected = iRound == idx;
            int iRound2 = Math.round(110.0f / iMax2);
            ConfigCheckBox configCheckBox = new ConfigCheckBox(this.centerX + 127 + (iRound2 * (idx % iMax2)), this.centerY + rowY, iRound2, mutableComponentLiteral2, bool -> {
                executeExpression(str4, null);
                if (!GeckoLibCache.isRoamingVariableAssignment(str4) && NetworkHandler.isClientConnected() && !ServerConfig.LOW_BANDWIDTH_USAGE.get().booleanValue()) {
                    NetworkHandler.sendToServer(new C2SRequestExecuteMolangPacket(str4, this.animatableModel.getEntity().getId()));
                }
                init();
            });
            configCheckBox.setStateTriggered(isSelected);
            ysmAddWidget(configCheckBox);
            if (idx % iMax2 == iMax2 - 1) {
                rowY += 14;
            }
            idx++;
        }
        iArr[0] = iArr[0] + size + 3;
        iArr2[0] = iArr2[0] + 1;
        this.maxConfigScroll = Math.max(0, iArr[0] - 110);
    }

    @NotNull
    private AnimationSlider createSlider(RangeConfig rangeConfig, String str, int[] iArr, int[] iArr2) {
        String str2 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_TITLE_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), rangeConfig.getTitle());
        String str3 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_DESC_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), rangeConfig.getDescription());
        MutableComponent mutableComponentLiteral = YsmText.literal(str2);
        //? if >=1.19.4 {
        Tooltip tooltipCreate = Tooltip.create(YsmText.literal(str3));
        //?}
        AnimationSlider animationSlider = new AnimationSlider(this.centerX + 125, this.centerY + iArr[0], mutableComponentLiteral, parseFloatValue(str), this.animatableModel, rangeConfig.getValue(), rangeConfig.getStep(), rangeConfig.getMin(), rangeConfig.getMax());
//? if >=1.19.4 {
        animationSlider.setTooltip(tooltipCreate);
        //?}
        return animationSlider;
    }

    @NotNull
    private ConfigCheckBox createCheckbox(CheckboxConfig checkboxConfig, String str, int[] iArr, int[] iArr2) throws NumberFormatException {
        String str3 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_TITLE_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), checkboxConfig.getTitle());
        String str4 = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format(CONFIG_DESC_FORMAT, this.currentConfigGroup.getId(), Integer.valueOf(iArr2[0])), checkboxConfig.getDescription());
        MutableComponent mutableComponentLiteral = YsmText.literal(str3);
//? if >=1.19.4 {
        Tooltip tooltipCreate = Tooltip.create(YsmText.literal(str4));
        //?}
        float parsedValue = parseFloatValue(str);
        ConfigCheckBox configCheckBox = new ConfigCheckBox(this.centerX + 125, this.centerY + iArr[0], mutableComponentLiteral, bool -> {
            String str2 = checkboxConfig.getValue() + "=" + (bool.booleanValue() ? "1" : "0");
            executeExpression(str2, null);
            if (!GeckoLibCache.isRoamingVariableAssignment(str2) && NetworkHandler.isClientConnected() && !ServerConfig.LOW_BANDWIDTH_USAGE.get().booleanValue()) {
                NetworkHandler.sendToServer(new C2SRequestExecuteMolangPacket(str2, this.animatableModel.getEntity().getId()));
            }
        }) {
            //? if >=1.20 {
            @Override
            public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                this.renderWidget(new YsmGui(graphics), mouseX, mouseY, partialTick);
            }
            //?}
            //? if >=1.19.4 && <1.20 {
            /*
            @Override
            public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
            }
             *///?}
            //? if <1.19.4 {
            /*
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                this.renderWidget(new YsmGui(poseStack), mouseX, mouseY, partialTick);
            }
             *///?}

            @Override
            public void renderWidget(YsmGui guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), -280804798);
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        };
        configCheckBox.setStateTriggered(parsedValue > 0.0f);
//? if >=1.19.4 {
        configCheckBox.setTooltip(tooltipCreate);
        //?}
        return configCheckBox;
    }

    private float parseFloatValue(String str) throws NumberFormatException {
        float value;
        if ("null".equals(str)) {
            value = 0.0f;
        } else if (NumberUtils.isParsable(str)) {
            value = Float.parseFloat(str);
        } else if (BooleanUtils.toBooleanObject(str) != null) {
            value = BooleanUtils.toBoolean(str) ? 1.0f : 0.0f;
        } else {
            value = 0.0f;
        }
        return value;
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
        int scrolledMouseY;
        guiGraphics.drawCenteredString(this.font, YsmText.translatable("gui.yes_steve_model.roulette.path", StringUtils.joinWith(" > ", navigationStack.stream().map((v0) -> {
            return v0.getLeft();
        }).toArray())), this.centerX + 195, this.centerY - 100, 16777215);
        renderRadialBackground(guiGraphics.pose(), mouseX, mouseY);
        renderRadialButtons(guiGraphics);
        renderPageInfo(guiGraphics);
        for (/*? if <1.19.4 {*/ /*Widget *//*?} else {*/ Renderable /*?}*/ renderable : ((ScreenAccessor) this).ysm$getRenderables()) {
            if (!(renderable instanceof ISpecialWidget)) {
                //? if <1.20
                /*renderable.render(guiGraphics.pose(), mouseX, mouseY, partialTick);*/
                //? if >=1.20
                renderable.render(guiGraphics.graphics(), mouseX, mouseY, partialTick);
            }
        }
        guiGraphics.enableScissor(0, this.centerY - 46, this.width, this.centerY + 110);
        if (mouseY < this.centerY - 46 || this.centerY + 110 < mouseY) {
            scrolledMouseY = -1000;
        } else {
            scrolledMouseY = mouseY + this.configScrollOffset;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, -this.configScrollOffset, 0.0f);
        for (/*? if <1.19.4 {*/ /*Widget *//*?} else {*/ Renderable /*?}*/ renderable2 : ((ScreenAccessor) this).ysm$getRenderables()) {
            if (renderable2 instanceof ISpecialWidget) {
                //? if <1.20
                /*renderable2.render(guiGraphics.pose(), mouseX, scrolledMouseY, partialTick);*/
                //? if >=1.20
                renderable2.render(guiGraphics.graphics(), mouseX, scrolledMouseY, partialTick);
            }
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        renderHoverTooltip(guiGraphics, mouseX, scrolledMouseY);
    }

    private void renderHoverTooltip(YsmGui guiGraphics, int mouseX, int mouseY) {
        if (-1 < this.hoveredIndex && this.hoveredIndex < this.currentProperties.size()) {
            String str = ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format("properties.extra_animation.%s.desc", this.currentProperties.getKeyAt(this.hoveredIndex)), StringPool.EMPTY);
            if (StringUtils.isNotBlank(str)) {
                guiGraphics.renderTooltip(this.font, this.font.split(YsmText.literal(str), 240), mouseX, mouseY);
            }
        }
    }

    private void executeExpression(String str, @Nullable Consumer<String> consumer) {
        try {
            this.animatableModel.executeExpression(GeckoLibCache.parseSimpleExpression(str), true, false, consumer);
        } catch (ParseException e) {
            YesSteveModel.LOGGER.error(e);
        }
    }

    private void renderPageInfo(YsmGui guiGraphics) {
        guiGraphics.fill(this.centerX + 157, this.centerY - 87, this.centerX + 238, this.centerY - 72, 0, -822083584);
        guiGraphics.drawCenteredString(this.font, String.format("%d/%d", Integer.valueOf(this.currentNavEntry.getRight().intValue() + 1), Integer.valueOf(((this.currentProperties.size() - 1) / 8) + 1)), this.centerX + 197, this.centerY - 83, ChatFormatting.AQUA.getColor().intValue());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta < 0.0d) {
            if (mouseX < this.centerX + 110) {
                nextPage();
                return true;
            }
            scrollConfigDown(20);
            return true;
        }
        if (delta <= 0.0d) {
            return false;
        }
        if (mouseX < this.centerX + 110) {
            previousPage();
            return true;
        }
        scrollConfigUp(20);
        return true;
    }

    private void previousPage() {
        this.currentNavEntry.setValue(Integer.valueOf(Math.max(0, this.currentNavEntry.getRight().intValue() - 1)));
    }

    private void nextPage() {
        if (this.currentProperties.size() > (this.currentNavEntry.getRight().intValue() + 1) * 8) {
            this.currentNavEntry.setValue(Integer.valueOf(this.currentNavEntry.getRight().intValue() + 1));
        }
    }

    private void scrollConfigUp(int i) {
        this.configScrollOffset = Math.max(0, this.configScrollOffset - i);
    }

    private void scrollConfigDown(int i) {
        this.configScrollOffset = Math.min(this.maxConfigScroll, this.configScrollOffset + i);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (-1 < this.hoveredIndex && this.hoveredIndex < this.currentProperties.size()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            String str = this.currentProperties.getKeyAt(this.hoveredIndex);
            if (RETURN_KEY.equals(str)) {
                navigateBack();
            } else if (str.startsWith(SUBMENU_PREFIX)) {
                navigateToSubmenu(str);
            } else {
                playAnimation(str);
            }
        } else if (-1 < this.hoveredConfigIndex && this.hoveredConfigIndex < this.currentProperties.size()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            String str2 = this.currentProperties.getValueAt(this.hoveredConfigIndex);
            if (str2.startsWith(SUBMENU_PREFIX)) {
                String strSubstring = str2.substring(SUBMENU_PREFIX.length());
                if (this.renderGroups.containsKey(strSubstring)) {
                    if (GeneralConfig.ROULETTE_SETTINGS_MODE.get() == GeneralConfig.RouletteSettingsMode.CLASSIC) {
                        showConfigGroup(strSubstring);
                    } else {
                        Minecraft.getInstance().setScreen(new rip.ysm.gui.ModelSettingsScreen(this.renderContext, this.animatableModel, this, strSubstring));
                    }
                }
            }
        }
        for (GuiEventListener guiEventListener : children()) {
            double scrolledMouseY = mouseY;
            if (guiEventListener instanceof ISpecialWidget) {
                scrolledMouseY = mouseY + this.configScrollOffset;
            }
            if (guiEventListener.mouseClicked(mouseX, scrolledMouseY, button)) {
                setFocused(guiEventListener);
                if (button == 0) {
                    setDragging(true);
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyMappingFactory.isActiveAndMatches(AnimationRouletteKey.KEY_ROULETTE, keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void showConfigGroup(String str) {
        this.currentConfigGroup = this.renderGroups.get(str);
        this.configScrollOffset = 0;
        this.maxConfigScroll = 0;
        init();
    }

    private void playAnimation(String str) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (NetworkHandler.isClientConnected()) {
            Pair<String, Integer> pairPeekLast = navigationStack.peekLast();
            String str2 = StringPool.EMPTY;
            if (pairPeekLast != null && StringUtils.isNotBlank(pairPeekLast.getLeft())) {
                str2 = pairPeekLast.getLeft();
            }
            Entity entity = this.animatableModel.getEntity();
            if (entity instanceof Player) {
                NetworkHandler.sendToServer(new C2SPlayAnimationPacket(this.hoveredIndex, str2));
            } else {
                NetworkHandler.sendToServer(new C2SPlayAnimationPacket(this.hoveredIndex, str2, entity.getId()));
            }
        } else if (localPlayer != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                cap.requestModelSwitch(str);
            });
        }
        if (localPlayer != null && GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG.get().booleanValue()) {
            YsmText.sendSystemMessage(localPlayer, YsmText.translatable("message.yes_steve_model.model.animation_roulette.play", str));
        }
        Minecraft.getInstance().setScreen(null);
    }

    private void navigateToSubmenu(String str) {
        if (navigationStack.size() > 5) {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null) {
                YsmText.sendSystemMessage(localPlayer, YsmText.translatable("gui.yes_steve_model.roulette.too_long"));
                return;
            }
            return;
        }
        String strSubstring = str.substring(SUBMENU_PREFIX.length());
        if (this.textProperties.get(strSubstring) != null) {
            navigationStack.addLast(MutablePair.of(strSubstring, 0));
            Minecraft.getInstance().setScreen(new AnimationRouletteScreen(this.renderGroups, this.textProperties, this.renderContext, this.animatableModel));
        }
    }

    private void navigateBack() {
        if (navigationStack.size() > 1) {
            navigationStack.removeLast();
            Minecraft.getInstance().setScreen(new AnimationRouletteScreen(this.renderGroups, this.textProperties, this.renderContext, this.animatableModel));
            return;
        }
        Minecraft.getInstance().setScreen(null);
    }

    public static void setInitialSubmenu(String str) {
        navigationStack.clear();
        navigationStack.addLast(MutablePair.of(StringPool.EMPTY, 0));
        navigationStack.addLast(MutablePair.of(str, 0));
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void renderRadialButtons(YsmGui guiGraphics) {
        float angle = 0.3926991f;
        int size = this.currentProperties.size() - (this.currentNavEntry.getRight().intValue() * 8);
        for (int i = 0; i < Math.min(8, size); i++) {
            int iIntValue = i + (this.currentNavEntry.getRight().intValue() * 8);
            int iCos = (int) (this.centerX + (65 * Mth.cos(angle)));
            float fSin = this.centerY + (65 * Mth.sin(angle));
            Objects.requireNonNull(this.font);
            int labelY = (int) (fSin - (9.0f / 2.0f));
            String str = this.currentProperties.getValueAt(iIntValue);
            boolean zStartsWith = this.currentProperties.getKeyAt(iIntValue).startsWith(SUBMENU_PREFIX);
            if (str.startsWith(SUBMENU_PREFIX)) {
                String strSubstring = str.substring(SUBMENU_PREFIX.length());
                if (this.renderGroups.containsKey(strSubstring)) {
                    str = this.renderGroups.get(strSubstring).getName();
                    int iCos2 = (int) (this.centerX + (35 * Mth.cos(angle)));
                    float fSin2 = this.centerY + (35 * Mth.sin(angle));
                    Objects.requireNonNull(this.font);
                    guiGraphics.drawCenteredString(this.font, YsmText.literal("⚙").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), iCos2, (int) (fSin2 - (9.0f / 2.0f)), 16777215);
                }
            }
            if (StringUtils.isNoneBlank(str)) {
                renderWrappedLabel(guiGraphics, YsmText.literal(ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format("properties.extra_animation.%s", this.currentProperties.getKeyAt(iIntValue)), str)), iCos, labelY, zStartsWith);
            } else {
                guiGraphics.drawCenteredString(this.font, YsmText.literal(ModelMetadataPresenter.getLocalizedModelString(this.renderContext, String.format("properties.extra_animation.%s", this.currentProperties.getKeyAt(iIntValue)), String.valueOf(iIntValue))), iCos, labelY - 8, 15986656);
            }
            if (this.currentNavEntry.getRight().intValue() == 0 && navigationStack.size() == 1) {
                renderKeyBindings(guiGraphics, iIntValue, iCos, labelY);
            }
            angle += 0.7853982f;
        }
    }

    private void renderKeyBindings(YsmGui guiGraphics, int slotIndex, int x, int y) {
        MutableComponent mutableComponentWithStyle = YsmText.literal("[ ").withStyle(ChatFormatting.YELLOW);
        KeyMapping keyMapping = ExtraAnimationKey.KEY_MAPPINGS.get(slotIndex);
        if (keyMapping.isUnbound()) {
            mutableComponentWithStyle.append(YsmText.translatable("key.yes_steve_model.extra_animation.none"));
        } else {
            mutableComponentWithStyle.append(keyMapping.getTranslatedKeyMessage());
        }
        mutableComponentWithStyle.append(" ]");
        guiGraphics.drawCenteredString(this.font, mutableComponentWithStyle, x, y + 4, 15986656);
    }

    private void renderWrappedLabel(YsmGui guiGraphics, MutableComponent mutableComponent, int x, int y, boolean isSubmenu) {
        Objects.requireNonNull(this.font);
        if (isSubmenu) {
            mutableComponent = mutableComponent.withStyle(ChatFormatting.RED);
        }
        List listSplit = this.font.split(mutableComponent, 50);
        int lineY = (y - (listSplit.size() * 9)) + 2;
        if (this.currentNavEntry.getRight().intValue() != 0 || navigationStack.size() > 1) {
            lineY += 9;
        }
        Iterator it = listSplit.iterator();
        while (it.hasNext()) {
            guiGraphics.drawCenteredString(this.font, (FormattedCharSequence) it.next(), x, lineY, 15986656);
            lineY += 9;
        }
    }

    private void renderRadialBackground(PoseStack poseStack, int mouseX, int mouseY) {
        if (this.currentProperties.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
// 1.17+ shader 管线绑定 + VertexFormat.Mode.QUADS ↔ 1.16.5 固定管线（POSITION_COLOR 走固定管线，GL_QUADS=7）
        Tesselator tesselator = Tesselator.getInstance();
        // 1.21 Tesselator.getBuilder/end 删除 → begin(Mode,Format) 直接返回 BufferBuilder，
        // 收尾走 buildOrThrow()+BufferUploader（vanilla-1.21.1 Tesselator.java:38 实证）
        //? if <1.21
        BufferBuilder builder = tesselator.getBuilder();
        //? if >=1.21
        /*BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);*/
        //? if <1.17 {
        /*builder.begin(7, DefaultVertexFormat.POSITION_COLOR);
         *///?} else {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        //? if <1.21
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        //?}
        Matrix4f matrix4fPose = poseStack.last().pose();
        float pointerAngle = (float) Mth.atan2(mouseY - this.centerY, mouseX - this.centerX);
        if (pointerAngle < 0.0f) {
            pointerAngle = 6.2831855f + pointerAngle;
        }
        float pointerRadius = Mth.sqrt(Mth.square(mouseY - this.centerY) + Mth.square(mouseX - this.centerX));
        boolean hoveredAny = false;
        boolean hoveredConfig = false;
        for (int i = 0; i < Math.min(8, this.currentProperties.size() - (this.currentNavEntry.getRight().intValue() * 8)); i++) {
            float startAngle = ((6.2831855f / 8) * i) + 0.034906585f;
            float endAngle = ((6.2831855f / 8) * (i + 1)) - 0.034906585f;
            int iIntValue = i + (this.currentNavEntry.getRight().intValue() * 8);
            boolean zStartsWith = this.currentProperties.getValueAt(iIntValue).startsWith(SUBMENU_PREFIX);
            hoveredAny = checkRadialHover(startAngle, pointerAngle, endAngle, pointerRadius, hoveredAny, zStartsWith, i, builder, matrix4fPose);
            boolean isConfigSliceHovered = startAngle < pointerAngle && pointerAngle < endAngle && 20.0f < pointerRadius && pointerRadius < 50.0f;
            if (zStartsWith) {
                if (isConfigSliceHovered) {
                    drawRadialSegment(builder, matrix4fPose, 15.0f, 50.0f, startAngle, endAngle, -268382465);
                    hoveredConfig = true;
                    this.hoveredConfigIndex = iIntValue;
                } else {
                    drawRadialSegment(builder, matrix4fPose, 25.0f, 50.0f, startAngle, endAngle, 1879101183);
                }
            }
        }
        if (!hoveredAny) {
            this.hoveredIndex = -1;
        }
        if (!hoveredConfig) {
            this.hoveredConfigIndex = -1;
        }
        //? if <1.21
        tesselator.end();
        //? if >=1.21
        /*com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.buildOrThrow());*/
        RenderSystem.disableBlend();
    }

    private boolean checkRadialHover(float startAngle, float pointerAngle, float endAngle, float pointerRadius, boolean alreadyHovered, boolean isSubmenu, int index, BufferBuilder bufferBuilder, Matrix4f matrix4f) {
        boolean isHovered = startAngle < pointerAngle && pointerAngle < endAngle && 50.0f < pointerRadius && pointerRadius < 100.0f;
        if (isHovered) {
            alreadyHovered = true;
            this.hoveredIndex = index + (this.currentNavEntry.getRight().intValue() * 8);
        }
        if (isHovered && index < this.currentProperties.size()) {
            if (isSubmenu) {
                drawRadialSegment(bufferBuilder, matrix4f, 50.0f, 115.0f, startAngle, endAngle, -251678464);
                drawRadialSegment(bufferBuilder, matrix4f, 25.0f, 50.0f, startAngle, endAngle, -1879048192);
            } else {
                drawRadialSegment(bufferBuilder, matrix4f, 25.0f, 115.0f, startAngle, endAngle, -251678464);
            }
        } else {
            drawRadialSegment(bufferBuilder, matrix4f, 25.0f, 105.0f, startAngle, endAngle, -1879048192);
        }
        return alreadyHovered;
    }

    private void drawRadialSegment(BufferBuilder bufferBuilder, Matrix4f matrix4f, float innerRadius, float outerRadius, float startAngle, float endAngle, int color) {
        float alpha = ((color >> 24) & 255) / 255.0f;
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        // 1.21 vertex/color/endVertex → addVertex/setColor（无 endVertex）
        //? if >=1.21
        /*bufferBuilder.addVertex(matrix4f, this.centerX + (outerRadius * Mth.cos(startAngle)), this.centerY + (outerRadius * Mth.sin(startAngle)), 0.0f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, this.centerX + (innerRadius * Mth.cos(startAngle)), this.centerY + (innerRadius * Mth.sin(startAngle)), 0.0f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, this.centerX + (innerRadius * Mth.cos(endAngle)), this.centerY + (innerRadius * Mth.sin(endAngle)), 0.0f).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(matrix4f, this.centerX + (outerRadius * Mth.cos(endAngle)), this.centerY + (outerRadius * Mth.sin(endAngle)), 0.0f).setColor(red, green, blue, alpha);*/
        //? if <1.21
        /*bufferBuilder.vertex(matrix4f, this.centerX + (outerRadius * Mth.cos(startAngle)), this.centerY + (outerRadius * Mth.sin(startAngle)), 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, this.centerX + (innerRadius * Mth.cos(startAngle)), this.centerY + (innerRadius * Mth.sin(startAngle)), 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, this.centerX + (innerRadius * Mth.cos(endAngle)), this.centerY + (innerRadius * Mth.sin(endAngle)), 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix4f, this.centerX + (outerRadius * Mth.cos(endAngle)), this.centerY + (outerRadius * Mth.sin(endAngle)), 0.0f).color(red, green, blue, alpha).endVertex();*/
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
