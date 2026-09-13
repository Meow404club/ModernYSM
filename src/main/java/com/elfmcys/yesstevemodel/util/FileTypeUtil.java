package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.google.common.collect.Sets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public final class FileTypeUtil {
    private static final Set<String> ARCHIVE_EXTENSIONS = Sets.newHashSet(".zip", ".7z", ".ysm");

    public static int parseHexId(String str) {
        return Integer.parseUnsignedInt(str.substring(0, 8), 16);
    }

    // fastutil Pair 接口 8.3.0 才有（1.16.5 打包 8.2.1 / 1.17.1 打包 8.2.1 实证无 it.unimi.dsi.fastutil.Pair；
    // 1182 起打包 8.3.1 有 Pair）：
    // 1.20.1 轴保持 fastutil Pair 原样；<1.18.2 轴换 YsmPair.StrPair（同名 left()/right() 消费面）
    //? if <1.18.2 {
    /*public static YsmPair.StrPair splitFileNameAndParentDir(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return YsmPair.of(filePath, StringPool.EMPTY);
        }
        return YsmPair.of(filePath.substring(lastSlashIndex + 1), filePath.substring(0, lastSlashIndex + 1));
    }
     *///?}
    //? if >=1.18.2 {
    public static it.unimi.dsi.fastutil.Pair<String, String> splitFileNameAndParentDir(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return it.unimi.dsi.fastutil.Pair.of(filePath, StringPool.EMPTY);
        }
        return it.unimi.dsi.fastutil.Pair.of(filePath.substring(lastSlashIndex + 1), filePath.substring(0, lastSlashIndex + 1));
    }
    //?}

    public static String getNameWithoutArchiveExtension(String filePath) {
        String fileName;
        int lastSlashIndex = filePath.lastIndexOf('/');

        if (lastSlashIndex == -1) {
            fileName = filePath;
        } else {
            fileName = filePath.substring(lastSlashIndex + 1);
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || !ARCHIVE_EXTENSIONS.contains(fileName.substring(dotIndex).toLowerCase())) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    public static String getFinalPathSegment(String path) {
        if (path == null || path.isEmpty()) {
            return StringPool.EMPTY;
        }

        String trimmedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlashIndex = trimmedPath.lastIndexOf('/');

        return lastSlashIndex >= 0 ? trimmedPath.substring(lastSlashIndex + 1) : trimmedPath;
    }

    public static ResourceLocation getPackIconLocation(String str) {
        //? if >=1.21
        /*return ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "model_pack_icon/" + str.hashCode());*/
        //? if <1.21
        return new ResourceLocation(YesSteveModel.MOD_ID, "model_pack_icon/" + str.hashCode());
    }

    /**
     * 解析 "match"字段的
     *  "match": [
     *     "minecraft:arrow",
     *     "#minecraft:arrows"
     *  ],
     *  带#的是实体 Tag
     */
    public static Set<ResourceLocation> resolveEntityTypes(String[] strArr) {
        HashSet<ResourceLocation> hashSet = new HashSet<>();
        for (String str : strArr) {
            if (str.startsWith("#")) {
                ResourceLocation resourceLocation = ResourceLocation.tryParse(str.substring(1));
                if (resourceLocation != null) {
                    // 1.20.1 BuiltInRegistries.ENTITY_TYPE.getTag(TagKey)（Holder 链）↔
                    // 1.16.5 EntityTypeTags.getAllTags().getTagOrEmpty(rl)（Tag 直查，getValues 为实体类型清单）
                    //? if <1.18.2 {
                    /*for (EntityType<?> type : net.minecraft.tags.EntityTypeTags.getAllTags().getTagOrEmpty(resourceLocation).getValues()) {
                        ResourceLocation key = net.minecraft.core.Registry.ENTITY_TYPE.getKey(type);
                        if (key != null) {
                            hashSet.add(key);
                        }
                    }
                     *///?}
                    //? if >=1.18.2 && <1.19.4 {
                    /*
                    // Registries/BuiltInRegistries 1.19.4 起；1.18.2~1.19.2 用 Registry 静态字段（1192 Registry.java:190/143 形）
                    net.minecraft.tags.TagKey<EntityType<?>> tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.Registry.ENTITY_TYPE_REGISTRY, resourceLocation);
                    net.minecraft.core.Registry.ENTITY_TYPE.getTag(tagKey).ifPresent(holderSet ->
                        holderSet.forEach(holder -> holder.unwrapKey().ifPresent(rk -> hashSet.add(rk.location())))
                    );
                     *///?}
                    //? if >=1.19.4 {
                    net.minecraft.tags.TagKey<EntityType<?>> tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, resourceLocation);
                    // 1.21.2 Registry.getTag(TagKey) 删除 → HolderGetter.get(TagKey)（同 Optional<Named> 返回）
                    //? if <1.21.2 {
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getTag(tagKey).ifPresent(holderSet ->
                        holderSet.forEach(holder -> holder.unwrapKey().ifPresent(rk -> hashSet.add(rk.location())))
                    );
                    //?}
                    //? if >=1.21.2 {
                    /*net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(tagKey).ifPresent(holderSet ->
                        holderSet.forEach(holder -> holder.unwrapKey().ifPresent(rk -> hashSet.add(rk.location())))
                    );*/
                    //?}
                    //?}
                }
            } else {
                ResourceLocation resourceLocation = ResourceLocation.tryParse(str);
                if (resourceLocation != null) {
                    hashSet.add(resourceLocation);
                }
            }
        }
        return hashSet;
    }
}