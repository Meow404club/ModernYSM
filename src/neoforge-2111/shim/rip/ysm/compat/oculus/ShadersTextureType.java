// 1.16.5 版本独有 shim（m2-gate-compat）：与共享源 oculus/ShadersTextureType 全等拷贝
// （纯原版依赖，无第三方触点；core 的 ITextureMap/OuterFileTexture/JsonTextureUtils
// 在签名与常量上直接引用此枚举）。
package rip.ysm.compat.oculus;

import net.minecraft.resources.Identifier;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String str) {
        this.suffix = str;
    }

    public Identifier appendSuffix(Identifier resourceLocation) {
        return Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);
    }
}
