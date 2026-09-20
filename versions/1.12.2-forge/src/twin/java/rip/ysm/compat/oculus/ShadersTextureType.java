package rip.ysm.compat.oculus;

// 1.12.2 twin（不走 stonecutter，直接单 import）：
// ResourceLocation 在 net.minecraft.util（vanilla-mc-1.12.2 Render.java:96
// bindTexture(ResourceLocation) 实证）
import net.minecraft.util.ResourceLocation;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String str) {
        this.suffix = str;
    }

    public ResourceLocation appendSuffix(ResourceLocation resourceLocation) {
        return new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);
    }
}
