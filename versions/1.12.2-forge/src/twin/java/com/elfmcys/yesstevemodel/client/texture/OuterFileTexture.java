package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * 1.12.2 分代 OuterFileTexture（legacy-1222-l1-render）。
 *
 * 共享版（src/main/.../client/texture/OuterFileTexture.java）绑定现代 AbstractTexture/
 * NativeImage/RenderSystem 链，1.12.2 无对应面。本类走 1.12.2 原生 DynamicTexture 同款
 * （vanilla-mc-1.12.2 DynamicTexture.java:7-36：TextureUtil.allocateTexture 上传 + 
 * loadTexture(IResourceManager) 抽象方法），像素解码由装载侧给 BufferedImage。
 *
 * 契约对齐共享版：getData()/构造/后缀贴图 Map——翻译层与装载链消费点不变。
 */
public class OuterFileTexture extends net.minecraft.client.renderer.texture.AbstractTexture implements ITextureMap {
    private final BufferedImage image;

    // L2 解析链：共享 YSMClientMapper.toTexture 的 byte[] 单参形态（内嵌 png/bmp 面）。
    // 1.12.2 无 NativeImage 解码面——字节暂存，解码推迟到 getImage() 消费侧
    @Nullable
    private final byte[] encodedData;

    private Map<ShadersTextureType, OuterFileTexture> suffixTextures = java.util.Collections.emptyMap();

    public OuterFileTexture(BufferedImage image) {
        this.image = image;
        this.encodedData = null;
    }

    public OuterFileTexture(byte[] data) {
        this.image = null;
        this.encodedData = data;
    }

    public OuterFileTexture(byte[] data, int imageFormat, int width, int height, BufferedImage decoded) {
        // byte[] 形与共享版构造签名兼容（装载侧 YSMClientMapper.toTexture 传已解码图）；
        // 1.12.2 无 NativeImage 解码面，解码由 ImageStream 侧完成
        this.image = decoded;
        this.encodedData = null;
    }

    @Override
    public void loadTexture(net.minecraft.client.resources.IResourceManager resourceManager) {
        // DynamicTexture 同款：分配+上传一次（vanilla-mc-1.12.2 DynamicTexture.java:24/33）
        net.minecraft.client.renderer.texture.TextureUtil.allocateTexture(
                this.getGlTextureId(), image.getWidth(), image.getHeight());
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        // 1.12.2 GL 纹理行序：ABGR 上传（TextureUtil.uploadTexture 内 moveTextureData 同款），
        // getRGB 默认 TYPE_INT_ARGB 需翻 alpha 位移——走 uploadTexturePage 前置转换
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        net.minecraft.client.renderer.texture.TextureUtil.uploadTexture(
                this.getGlTextureId(), pixels, image.getWidth(), image.getHeight());
    }

    public int getGlTextureId() {
        return super.getGlTextureId();
    }

    public BufferedImage getImage() {
        BufferedImage img = this.image;
        if (img == null && this.encodedData != null) {
            // 惰性解码：内嵌 png/bmp 字节（javax.imageio 兼容 1.12.2 Java8 面）
            try {
                return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(this.encodedData));
            } catch (java.io.IOException e) {
                return null;
            }
        }
        return img;
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = map;
    }

    @Nullable
    public Map<ShadersTextureType, OuterFileTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
