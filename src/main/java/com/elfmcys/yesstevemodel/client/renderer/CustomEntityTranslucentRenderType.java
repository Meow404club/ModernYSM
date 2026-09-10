package com.elfmcys.yesstevemodel.client.renderer;

//? if <1.17 {
// import net.minecraft.Util;
//? } else {
import net.minecraft.Util;
//? }
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CustomEntityTranslucentRenderType extends RenderType {

    // 1.16.5 无 Util.memoize（1.17+），ConcurrentHashMap+computeIfAbsent 等价缓存
    //? if <1.17 {
    // private static final java.util.concurrent.ConcurrentMap<ResourceLocation, CustomEntityTranslucentRenderType> CACHE_MAP = new ConcurrentHashMap<>();
    //? } else {
    private static final Function<ResourceLocation, CustomEntityTranslucentRenderType> CACHE = Util.memoize(CustomEntityTranslucentRenderType::new);
    //? }

    private final boolean useBlend;

    private final Optional<RenderType> renderType;

    private CustomEntityTranslucentRenderType(ResourceLocation resourceLocation) {
        this(RenderType.entityTranslucent(resourceLocation));
    }

    private CustomEntityTranslucentRenderType(RenderType renderType) {
        super("entity_translucent_ysm", renderType.format(), renderType.mode(), renderType.bufferSize(), renderType.affectsCrumbling(), false, renderType::setupRenderState, renderType::clearRenderState);
        this.useBlend = renderType.isOutline();
        this.renderType = renderType.outline();
    }

    public boolean isOutline() {
        return this.useBlend;
    }

    @NotNull
    public Optional<RenderType> outline() {
        return this.renderType;
    }

    public static CustomEntityTranslucentRenderType get(ResourceLocation resourceLocation) {
        //? if <1.17
        // return CACHE_MAP.computeIfAbsent(resourceLocation, CustomEntityTranslucentRenderType::new);
        //? if >=1.17
        return CACHE.apply(resourceLocation);
    }

    //? if <1.17 {
    //? }
}
