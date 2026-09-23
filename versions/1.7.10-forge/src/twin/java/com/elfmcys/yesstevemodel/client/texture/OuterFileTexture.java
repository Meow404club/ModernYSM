package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * 1.7.10 分代 OuterFileTexture（legacy1710-l2a-model-load）。
 *
 * 共享版（src/main/.../client/texture/OuterFileTexture.java）绑定现代
 * AbstractTexture/NativeImage/RenderSystem 链。1710 AbstractTexture（vanilla-mc-1710
 * AbstractTexture.java:3-20）= getGlTextureId 惰性 + deleteGlTexture，
 * loadTexture(IResourceManager) 由 ITextureObject（ITextureObject.java:6）声明同签名；
 * TextureUtil.allocateTexture:160/uploadTexture:38/glGenTextures:26 MCP 同名——
 * 1.12.2 twin（versions/1.12.2-forge twin OuterFileTexture.java）几乎直拷，
 * 净差异=零（纹理绑定侧不用 GlStateManager——1710 无此类）。
 *
 * 契约对齐共享版：getData()/构造/后缀贴图 Map——翻译层与装载链消费点不变。
 */
public class OuterFileTexture extends net.minecraft.client.renderer.texture.AbstractTexture implements ITextureMap {
    private final BufferedImage image;

    // 解析链：共享 YSMClientMapper.toTexture 的 byte[] 单参形态（内嵌 png/bmp 面）。
    // 字节暂存，解码推迟到 getImage() 消费侧
    private final byte[] encodedData;

    // ensureUploaded 幂等哨兵（解析态纹理首次绑定前就地解码+上传）
    private boolean uploaded;

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
        // byte[] 形与共享版构造签名兼容（装载侧 YSMClientMapper.toTexture 传已解码图）
        this.image = decoded;
        this.encodedData = null;
    }

    @Override
    public void loadTexture(net.minecraft.client.resources.IResourceManager resourceManager) {
        // DynamicTexture 同款：分配+上传一次（vanilla-mc-1710 TextureUtil:160/:38）
        BufferedImage img = ensureDecoded();
        if (img == null) {
            return;
        }
        upload(img);
        this.uploaded = true;
    }

    /**
     * 解析态 OuterFileTexture（byte[] 构造）不走 TextureManager 装载面——首次渲染
     * 绑定前就地完成 解码+分配+上传（DynamicTexture 同款 GL 路径），glTextureId
     * 惰性分配（AbstractTexture 首调自申请，vanilla-mc-1710 AbstractTexture.java:8-12）。
     * 幂等：uploaded 哨兵。
     */
    public void ensureUploaded() {
        if (this.uploaded) {
            return;
        }
        BufferedImage img = ensureDecoded();
        if (img == null) {
            return;
        }
        upload(img);
        this.uploaded = true;
    }

    private void upload(BufferedImage img) {
        net.minecraft.client.renderer.texture.TextureUtil.allocateTexture(
                this.getGlTextureId(), img.getWidth(), img.getHeight());
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
        net.minecraft.client.renderer.texture.TextureUtil.uploadTexture(
                this.getGlTextureId(), pixels, img.getWidth(), img.getHeight());
    }


    private BufferedImage ensureDecoded() {
        BufferedImage img = this.image;
        if (img == null && this.encodedData != null) {
            try {
                img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(this.encodedData));
            } catch (java.io.IOException e) {
                return null;
            }
        }
        return img;
    }

    /** 热重载面：GL id 已分配与否（getGlTextureId 首调会申请 id，本读取无副作用；122 twin 同名同义）。 */
    public boolean hasGlId() {
        return this.glTextureId != -1;
    }

    public BufferedImage getImage() {
        return ensureDecoded();
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = map;
    }


    public Map<ShadersTextureType, OuterFileTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
