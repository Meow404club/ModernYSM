package rip.ysm.compat.oculus;

// 1.12.2 ResourceLocation 在 net.minecraft.util（vanilla-mc-1.12.2 Render.java:96
// bindTexture(ResourceLocation) import net.minecraft.util.ResourceLocation 实证）；
// 1.14.4 起迁 net.minecraft.resources。raw 面恒 1.20.1 合法：<1.13 分支整行注释
//? if <1.13
// import net.minecraft.util.ResourceLocation;
//? if >=1.13
import net.minecraft.resources.ResourceLocation;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String str) {
        this.suffix = str;
    }

    public ResourceLocation appendSuffix(ResourceLocation resourceLocation) {
        //? if >=1.21
        /*return ResourceLocation.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);*/
        //? if <1.21
        return new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);
    }
}
