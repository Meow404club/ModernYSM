package rip.ysm.util;

import net.minecraft.resources.ResourceLocation;

/**
 * ResourceLocation 构造跨版本工厂。1.21 起 {@code new ResourceLocation(ns, path)} 构造器
 * 收紧为私有（vanilla 1.21 反编译口径，1.21.1 线接续时按实测收口），全仓构造点统一收编本工厂，
 * 后续版本只需改本文件一处。当前共享源 1.20.1 展开态 = 构造器形态（forge 全线与 1.20.4 裸写）。
 */
public final class Rl {
    private Rl() {
    }

    public static ResourceLocation of(String namespace, String path) {
        //? if neoforge && >=1.20.5 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
         *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }
}
