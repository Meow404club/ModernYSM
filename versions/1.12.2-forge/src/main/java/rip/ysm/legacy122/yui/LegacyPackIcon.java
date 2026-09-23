package rip.ysm.legacy122.yui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import rip.ysm.yui.YuiBackend;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 包图标装载（M-U2 r3 语义B：ysm-pack.png → DynamicTexture）。
 *
 * <p>主线 PackIconButton.java:76-89 经 TextureRegistry 取包图标、缺失回退
 * default_pack_icon——1.12.2 的 builtin 资源不在 textures/ 命名空间下
 * （assets/yes_steve_model/builtin/…），走 ImageIO+DynamicTexture 注册到
 * 稳定 ResourceLocation（vanilla loadTexture 面板，TextureManager.java:51），
 * 后端 blit 按 rl 正常绑定。图标按包路径进程内缓存。
 */
@SideOnly(Side.CLIENT)
public final class LegacyPackIcon {

    private static final String DEFAULT_ICON = "assets/yes_steve_model/texture/default_pack_icon.png";

    private static final Map<String, YuiBackend.Texture> CACHE =
            new ConcurrentHashMap<String, YuiBackend.Texture>();

    private LegacyPackIcon() {
    }

    /** 包图标（缺失回退 default_pack_icon；两者皆缺=null，调用方画实底）。 */
    public static YuiBackend.Texture icon(String packPath) {
        YuiBackend.Texture cached = CACHE.get(packPath);
        if (cached == null) {
            cached = load(packPath);
            CACHE.put(packPath, cached);
        }
        return cached;
    }

    private static YuiBackend.Texture load(String packPath) {
        BufferedImage img = read("assets/yes_steve_model/builtin/" + packPath + "/ysm-pack.png");
        if (img == null) {
            img = read(DEFAULT_ICON);
        }
        if (img == null) {
            return null;
        }
        ResourceLocation rl = new ResourceLocation("openysmlegacy122", "packicon/" + packPath);
        Minecraft.getMinecraft().getTextureManager().loadTexture(rl, new DynamicTexture(img));
        return new YuiBackend.Texture(rl.toString(), img.getWidth(), img.getHeight());
    }

    private static BufferedImage read(String resourcePath) {
        try (InputStream in = LegacyPackIcon.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            System.out.println("[ysm-legacy122] pack icon read failed for " + resourcePath + ": " + e);
            return null;
        }
    }
}
