package rip.ysm.util;

import net.minecraft.resources.ResourceLocation;

/**
 * ResourceLocation 构造跨版本工厂。1.21 起 {@code new ResourceLocation(ns, path)} 构造器
 * 收紧为私有（vanilla 1.21.1 ResourceLocation.java:38 private 构造 + :51
 * fromNamespaceAndPath 静态工厂实证）；1.20.6 仍是 public 构造器（vanilla-1.20.6:41）。
 * 全仓构造点统一收编本工厂，后续版本只需改本文件一处。
 */
public final class Rl {
    private Rl() {
    }

    /**
     * 1.21 起 {@code ResourceLocation.isValidResourceLocation(String)} 静态方法删除
     * （vanilla-1.21.1 ResourceLocation.java 仅余 isValidNamespace/isValidPath 实证）；
     * tryParse 可空语义与原「双段均合法」判定等价。
     */
    public static boolean isValid(String location) {
        //? if >=1.21 {
        /*return ResourceLocation.tryParse(location) != null;
         *///?} else {
        return ResourceLocation.isValidResourceLocation(location);
        //?}
    }

    public static ResourceLocation of(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
         *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }
}
