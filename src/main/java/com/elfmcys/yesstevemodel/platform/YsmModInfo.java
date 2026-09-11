package com.elfmcys.yesstevemodel.platform;

import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.fml.ModList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * architectury {@code Platform.Mod} 的 Forge 原生对位物，
 * 仅暴露项目实际消费面（getVersion/getName/getModId/findResource），语义逐一对齐：
 * <ul>
 *   <li>{@code getVersion()} → String（IModInfo.getVersion() 为 ArtifactVersion，
 *       toString 展开为 "1.2.3" 形态，与 architectury Forge impl 一致）</li>
 *   <li>{@code findResource(String...)} → Optional&lt;Path&gt;（资源不存在时为 empty；
 *       forge 原生 ModFile.findResource 不判存在，此处补齐 architectury 的 Optional 语义）。
 *       dev 运行时 mod 来自 build/resources 目录，同样可解析（IModFile 对目录型 mod
 *       走 SecureJar 路径映射）。</li>
 * </ul>
 */
public final class YsmModInfo {
    private final IModInfo info;

    public YsmModInfo(IModInfo info) {
        this.info = info;
    }

    public IModInfo info() {
        return this.info;
    }

    public String getModId() {
        return info.getModId();
    }

    public String getName() {
        return info.getDisplayName();
    }

    public String getVersion() {
        return info.getVersion().toString();
    }

    /** 原 Mod.findResource：相对 mod 根的资源路径（"assets", "ysm", "builtin" 形态）。 */
    public Optional<Path> findResource(String... path) {
        // forgespi 3.x（1.16.5）IModFileInfo 无 getFile()：<1.17 经类加载器锚点解析（同 ServerModelManager 口径）
        //? if <1.17 {
        /*try {
            java.net.URL url = YsmModInfo.class.getClassLoader().getResource(String.join("/", path));
            if (url == null || !"file".equals(url.getProtocol())) {
                return Optional.empty();
            }
            Path resolved = java.nio.file.Paths.get(url.toURI());
            return Files.exists(resolved) ? Optional.of(resolved) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
         *///?} else {
        IModFileInfo fileInfo = ModList.get().getModFileById(info.getModId());
        if (fileInfo == null || fileInfo.getFile() == null) {
            return Optional.empty();
        }
        Path resolved = fileInfo.getFile().findResource(path);
        return Files.exists(resolved) ? Optional.of(resolved) : Optional.empty();
        //?}
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof YsmModInfo)) {
            return false;
        }
        YsmModInfo other = (YsmModInfo) obj;
        return Objects.equals(this.info, other.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.info);
    }

    @Override
    public String toString() {
        return "YsmModInfo[info=" + this.info + "]";
    }
}
