package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.resource.models.AuthorInfo;
import com.elfmcys.yesstevemodel.resource.models.Metadata;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.resource.models.MainModelInfo;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModelMetadataPresenter {

    public static final String DEFAULT_LOCALE = "en_us";

    public static String getLocalizedString(ModelPackData modelPackData, String key, @Nullable String defaultValue) {
        if (defaultValue == null) {
            defaultValue = StringPool.EMPTY;
        }
        // LanguageManager.getSelected() 1.19.4 起返回 String（1194:70）；1.16.5~1.19.2 返回
        // LanguageInfo（1192:69）→ getCode()（1192 LanguageInfo.java:19）
        //? if <1.17
        /*String selectedLocale = Minecraft.getInstance().getLanguageManager().getSelected().getCode();*/
        //? if >=1.17 && <1.19.4
        /*String selectedLocale = Minecraft.getInstance().getLanguageManager().getSelected().getCode();*/
        //? if >=1.19.4
        String selectedLocale = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<String, Map<String, String>> translations = modelPackData.getTranslations();

        if (translations == null || translations.isEmpty()) {
            return defaultValue;
        }
        if (translations.containsKey(selectedLocale)) {
            return translations.get(selectedLocale).getOrDefault(key, defaultValue);
        }
        if (translations.containsKey(DEFAULT_LOCALE)) {
            return translations.get(DEFAULT_LOCALE).getOrDefault(key, defaultValue);
        }
        return defaultValue;
    }

    public static String getLocalizedModelString(ModelAssembly modelAssembly, String key, String defaultValue) {
        //? if <1.17 {
        /*return getLocalizedModelStringForLocale(modelAssembly, Minecraft.getInstance().getLanguageManager().getSelected().getCode(), key, defaultValue);
         *///?}
        //? if >=1.17 && <1.19.4 {
        /*return getLocalizedModelStringForLocale(modelAssembly, Minecraft.getInstance().getLanguageManager().getSelected().getCode(), key, defaultValue);
         *///?}
        //? if >=1.19.4 {
        return getLocalizedModelStringForLocale(modelAssembly, Minecraft.getInstance().getLanguageManager().getSelected(), key, defaultValue);
        //?}
    }

    public static String getLocalizedModelStringForLocale(ModelAssembly modelAssembly, String locale, String key, String defaultValue) {
        Map<String, Map<String, String>> metadataMap = modelAssembly.getExpressionCache().getMetadata();

        if (metadataMap.isEmpty()) {
            return defaultValue;
        }
        if (metadataMap.containsKey(locale)) {
            return metadataMap.get(locale).getOrDefault(key, defaultValue);
        }
        if (metadataMap.containsKey(DEFAULT_LOCALE)) {
            return metadataMap.get(DEFAULT_LOCALE).getOrDefault(key, defaultValue);
        }
        return defaultValue;
    }

    public static List<Component> buildModelTooltip(ModelAssembly modelAssembly, String locale, String fileName, boolean showAdvancedInfo) {
        List<Component> tooltipLines = Lists.newArrayList();
        Metadata extraInfo = modelAssembly.getModelData().getExtraInfo();

        if (extraInfo != null) {
            String localizedName = getLocalizedModelStringForLocale(modelAssembly, locale, "metadata.name", extraInfo.getName());
            if (StringUtils.isNoneBlank(localizedName)) {
                tooltipLines.add(YsmText.literal(localizedName).withStyle(ChatFormatting.GOLD));
            }

            String localizedTips = getLocalizedModelStringForLocale(modelAssembly, locale, "metadata.tips", extraInfo.getTips());
            if (StringUtils.isNoneBlank(localizedTips)) {
                Arrays.stream(localizedTips.replace("\r", StringPool.EMPTY).split("\n")).forEach(tipLine -> {
                    tooltipLines.add(YsmText.literal(tipLine).withStyle(ChatFormatting.GRAY));
                });
            }

            if (!extraInfo.getAuthors().isEmpty() || StringUtils.isNoneBlank(extraInfo.getLicense().getFirst())) {
                //? if <1.19.4
                /*tooltipLines.add(YsmText.literal(" "));*/
                //? if >=1.19.4
                tooltipLines.add(CommonComponents.space());
            }

            if (!extraInfo.getAuthors().isEmpty()) {
                String authorsString = StringUtils.join(extraInfo.getAuthors().stream()
                        .map(createAuthorNameMapper(modelAssembly, locale, new int[]{-1}))
                        .toArray(String[]::new), "丨");

                tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.authors",
                        YsmText.literal(authorsString).withStyle(ChatFormatting.DARK_GRAY)));
            }

            if (StringUtils.isNoneBlank(extraInfo.getLicense().getFirst())) {
                tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.license",
                        YsmText.literal(extraInfo.getLicense().getFirst()).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        if (showAdvancedInfo) {
            tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.file", YsmText.literal(fileName).withStyle(ChatFormatting.DARK_GRAY)));
            tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.hash", YsmText.literal(modelAssembly.getModelData().getModelHash()).withStyle(ChatFormatting.DARK_GRAY)));

            if (StringUtils.isNoneBlank(modelAssembly.getModelData().getExtra())) {
                tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.extra", YsmText.literal(modelAssembly.getModelData().getExtra()).withStyle(ChatFormatting.DARK_GRAY)));
            }

            if (modelAssembly.getModelData().getTimestamp() != 0) {
                String formattedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(modelAssembly.getModelData().getTimestamp() * 1000), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.timestamp", YsmText.literal(formattedDate).withStyle(ChatFormatting.DARK_GRAY)));
            }

            if (StringUtils.isNoneBlank(modelAssembly.getModelData().getRand())) {
                tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.rand", YsmText.literal(modelAssembly.getModelData().getRand()).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        MainModelInfo info = modelAssembly.getModelData().getMainModelInfo();
        if (info != null) {
                //? if <1.19.4
                /*tooltipLines.add(YsmText.literal(" "));*/
                //? if >=1.19.4
                tooltipLines.add(CommonComponents.space());
            tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.main_model_info", info.getBones(), info.getCubes(), info.getFaces()).withStyle(ChatFormatting.GRAY));
            tooltipLines.add(YsmText.translatable("gui.yes_steve_model.model.texture_info", modelAssembly.getAnimationBundle().getTextures().size()).withStyle(ChatFormatting.GRAY));
        }

        return tooltipLines;
    }

    @NotNull
    private static Function<AuthorInfo, String> createAuthorNameMapper(ModelAssembly modelAssembly, String locale, int[] index) {
        return authorInfo -> {
            index[0] = index[0] + 1;
            String localizedAuthorName = getLocalizedModelStringForLocale(modelAssembly, locale, String.format("metadata.authors.%d.name", Integer.valueOf(index[0])), authorInfo.getName());

            if (authorInfo.getRole().isEmpty()) {
                return localizedAuthorName;
            }

            String localizedRole = getLocalizedModelStringForLocale(modelAssembly, locale, String.format("metadata.authors.%d.role", Integer.valueOf(index[0])), authorInfo.getRole());
            return localizedRole + ": " + localizedAuthorName;
        };
    }
}