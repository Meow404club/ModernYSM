package com.elfmcys.yesstevemodel.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class YSMNativeHelper {
    public static Object createTranslatableComponent(String str, @Nullable Object[] objArr) {
        if (objArr == null || objArr.length == 0) {
            return YsmText.translatable(str);
        }
        return YsmText.translatable(str, objArr);
    }

    public static Object createLiteralComponent(@Nullable String str) {
        return YsmText.literal(str == null ? StringPool.EMPTY : str);
    }

    public static Object appendComponents(Object obj, Object obj2) {
        return ((MutableComponent) obj).append((Component) obj2);
    }

    public static int[] parseTextureIndices(String[] textureNames) {
        HashMap<String, Integer> indexMap = new HashMap<>();
        for(int i = 0; i < textureNames.length; ++i) {
            indexMap.put(textureNames[i] + ".png", i);
        }

        return indexMap.keySet().stream().mapToInt(indexMap::get).toArray();
    }

    @OnlyIn(Dist.CLIENT)
    public static UUID getClientPlayerUUID() {
        // getProfileId() 为 1.20.1 forge User 扩展（1.16.5 无）：退化取 GameProfile UUID
        //? if <1.17 {
        /*return Minecraft.getInstance().getUser().getGameProfile().getId();
         *///?} else {
        return Minecraft.getInstance().getUser().getProfileId();
        //?}
    }

    public static int getAvailableCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}