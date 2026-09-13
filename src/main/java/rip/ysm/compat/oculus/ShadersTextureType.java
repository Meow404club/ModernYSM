package rip.ysm.compat.oculus;

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
