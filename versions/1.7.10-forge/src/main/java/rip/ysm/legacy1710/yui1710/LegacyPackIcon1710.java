package rip.ysm.legacy1710.yui1710;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import rip.ysm.yui.YuiBackend;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 包图标装载（122 LegacyPackIcon 的 1.7.10 twin，PackIconButton.java:76-89 语义）：
 * ysm-pack.png → ImageIO+DynamicTexture 注册到稳定 ResourceLocation，后端 blit 按
 * rl 正常绑定；缺失回退 default_pack_icon。图标按包路径进程内缓存。
 *
 * <p>1.7.10 API 面（vanilla-mc-1710 反编译求证）：TextureManager.loadTexture
 * （TextureManager.java:62 loadTexture(ResourceLocation,ITextureObject)）+
 * DynamicTexture(BufferedImage)（DynamicTexture.java:11 构造即上传）+
 * ResourceLocation(String,String)（ResourceLocation.java:8）。
 */
public final class LegacyPackIcon1710 {

    private static final String DEFAULT_ICON = "assets/yes_steve_model/texture/default_pack_icon.png";

    private static final Map<String, YuiBackend.Texture> CACHE =
            new ConcurrentHashMap<String, YuiBackend.Texture>();

    private LegacyPackIcon1710() {
    }

    /** 包图标（缺失回退 default_pack_icon；两者皆缺=null，调用方画实底）。 */
    public static YuiBackend.Texture icon(String packPath) {
        // containsKey 守卫：load 可能返回 null，ConcurrentHashMap 不容 null value
        if (CACHE.containsKey(packPath)) {
            return CACHE.get(packPath);
        }
        YuiBackend.Texture texture = load(packPath);
        CACHE.put(packPath, texture);
        return texture;
    }

    private static YuiBackend.Texture load(String packPath) {
        BufferedImage img = read("assets/yes_steve_model/builtin/" + packPath + "/ysm-pack.png");
        if (img == null) {
            img = read(DEFAULT_ICON);
        }
        if (img == null) {
            return null;
        }
        ResourceLocation rl = new ResourceLocation("openysmlegacy1710", "packicon/" + packPath);
        Minecraft.getMinecraft().getTextureManager().loadTexture(rl, new DynamicTexture(img));
        return new YuiBackend.Texture(rl.toString(), img.getWidth(), img.getHeight());
    }

    private static BufferedImage read(String resourcePath) {
        try (InputStream in = LegacyPackIcon1710.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            System.out.println("[ysm-legacy1710] pack icon read failed for " + resourcePath + ": " + e);
            return null;
        }
    }
}
