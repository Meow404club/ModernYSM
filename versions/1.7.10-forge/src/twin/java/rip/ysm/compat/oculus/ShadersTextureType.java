package rip.ysm.compat.oculus;

// 1.7.10 twin（不走 stonecutter，直接单 import）：
// ResourceLocation 在 net.minecraft.util（vanilla-mc-1710 同 1.12.2 包位）；
// 共享版是 mojmap net.minecraft.resources 面（1.14.4+），1710 无对应。
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
        // 1.7.10 MCP stable_12 定名 getResourceDomain/getResourcePath（vanilla-mc-1710
        // ResourceLocation.java 实证；mojmap 面 getNamespace/getPath 在本代不存在）
        return new ResourceLocation(resourceLocation.getResourceDomain(), resourceLocation.getResourcePath() + this.suffix);
    }
}
