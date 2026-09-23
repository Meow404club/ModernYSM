package rip.ysm.util;

//? if >=1.14
import net.minecraft.resources.ResourceLocation;
//? if <1.14
/*import net.minecraft.util.ResourceLocation;*/

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
     * <p>1.12.2 无任何资源路径校验（vanilla-mc-1122 ResourceLocation.java:24-28 构造器
     * 零异常路径，splitObjectName 仅切分+小写）——平台语义即「非 null 即合法」。
     */
    public static boolean isValid(String location) {
        //? if <1.13
        /*return location != null;*/
        //? if >=1.13 && <1.21
        return ResourceLocation.isValidResourceLocation(location);
        //? if >=1.21
        /*return ResourceLocation.tryParse(location) != null;*/
    }

    public static ResourceLocation of(String namespace, String path) {
        // 1.12.2/1.16~1.20 双段 public 构造器（vanilla-mc-1122 ResourceLocation.java:28 实证）
        //? if >=1.21
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);*/
        //? if <1.21
        return new ResourceLocation(namespace, path);
    }
}
