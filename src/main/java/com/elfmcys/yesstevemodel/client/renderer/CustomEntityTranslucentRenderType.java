package com.elfmcys.yesstevemodel.client.renderer;

// 1.21.11 RenderType 移 net.minecraft.client.renderer.rendertype 子包
//? if >=21.11
/*import net.minecraft.client.renderer.rendertype.RenderType;*/
//? if <21.11
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

// 1.16.5 无 Util（本文件 CACHE 仅 >=1.17 分支触及）；1.17~1.21.10 = net.minecraft.Util；
// 1.21.11 移 net.minecraft.util.Util 子包。1.20.1 根活动节点原文直编译、条件仅注释 →
// import 行不可版本化，改单点 FQN（见 CACHE 行）
//? if >=21.11
/*import net.minecraft.util.Util;*/


import java.util.function.Function;

//? if <21.5 {

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CustomEntityTranslucentRenderType extends RenderType {

    // 1.16.5 无 Util.memoize（1.17+），ConcurrentHashMap+computeIfAbsent 等价缓存。
    // 铁律：非活跃块包裹区内禁「行条件假→裸行」（stitcher /^ 转义损坏，21.5 生成树实证），
    // 须用已注释态/存储态形式
    //? if <1.17
    // private static final java.util.concurrent.ConcurrentMap<ResourceLocation, CustomEntityTranslucentRenderType> CACHE_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    //? if >=1.17 && <21.11
    private static final Function<ResourceLocation, CustomEntityTranslucentRenderType> CACHE = net.minecraft.Util.memoize(CustomEntityTranslucentRenderType::new);
    //? if >=21.11
    /*private static final Function<ResourceLocation, CustomEntityTranslucentRenderType> CACHE = net.minecraft.util.Util.memoize(CustomEntityTranslucentRenderType::new);*/

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
}
//?}
//? if >=21.5 {
/*// 1.21.5 RenderPipeline 化：RenderType 变抽象基类（mode() 抽象化、ctor 变形
//(String,int,boolean,boolean,Runnable,Runnable)），「包一层改 isOutline/outline」形态不可达
//→ 降级直用原版 RenderType.entityTranslucent（neoforge-21.5.98-sources RenderType.java:769）。
//三处调用点（RenderFirstPlayerBackground/HandItemRenderer/IGeoRenderer）只消费 RenderType。
public final class CustomEntityTranslucentRenderType {

    private static final Function<ResourceLocation, RenderType> CACHE = rl -> RenderType.entityTranslucent(rl);

    private CustomEntityTranslucentRenderType() {
    }

    public static RenderType get(ResourceLocation resourceLocation) {
        return CACHE.apply(resourceLocation);
    }
}*/
//?}
