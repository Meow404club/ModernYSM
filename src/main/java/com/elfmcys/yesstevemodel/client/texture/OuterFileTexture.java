package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;
import com.mojang.blaze3d.platform.NativeImage;
// 1.21.5 TextureUtil 类删除（21.5 sources 无 com.mojang.blaze3d.platform.TextureUtil）
//? if <21.5
import com.mojang.blaze3d.platform.TextureUtil;
// 26.2 TextureFormat 删（com.mojang.blaze3d.textures 包清空）→ GpuFormat.RGBA8_UNORM
//（/tmp/vanilla-262 GpuFormat.java:14 实证）
//? if >=21.5 && <26.2
/*import com.mojang.blaze3d.textures.TextureFormat;*/
// 26.3 GpuFormat 随 renderpearl 迁移 com.mojang.renderpearl.api（/tmp/vanilla-263 api/GpuFormat.java:14 RGBA8_UNORM 实证）
//? if >=26.2 && <26.3
/*import com.mojang.blaze3d.GpuFormat;*/
//? if >=26.3
/*import com.mojang.renderpearl.api.GpuFormat;*/
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class OuterFileTexture extends AbstractTexture implements ITextureMap {
    private final byte[] data;

    private Map<ShadersTextureType, OuterFileTexture> suffixTextures = Reference2ReferenceMaps.emptyMap();

    public OuterFileTexture(byte[] data) {
        this.data = data;
    }

    // 1.21.4 AbstractTexture.load 抽象删除（vanilla-1.21.4 AbstractTexture 无 load，
    // 纹理注册不再回调）→ 方法降级为本类自有 API，注册点显式调用
    //? if <21.4
    @Override
    public void load(@NotNull ResourceManager resourceManager) {
        //? if <21.5 {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::doLoad);
        } else {
            doLoad();
        }
        //?}
        // 1.21.5 RenderSystem.isOnRenderThreadOrInit/recordRenderCall 删除（21.5 RenderSystem
        //仅 isOnRenderThread/assertOnRenderThread:105-109）。显式注册点（ClientModelManager.java:982、
        //UploadManager.java:170）均经 Minecraft.submit 调度到主线程=渲染线程，直呼安全
        //? if >=21.5 {
        /*doLoad();*/
        //?}
    }

    public void doLoad() {
        try (NativeImage imageIn = NativeImage.read(new ByteArrayInputStream(data))) {
            int width = imageIn.getWidth();
            int height = imageIn.getHeight();
            //? if <21.5 {
            TextureUtil.prepareImage(this.getId(), 0, width, height);
            // NativeImage.upload 两代重载：11 参（≤1.21.3，21.3 NativeImage.java:343/OverlayTexture.java:34
            // 同参范式）→ 8 参（1.21.4+，NativeImage.java:347）；中间的 9/4 参是委托便捷形
            //? if <21.4
            imageIn.upload(0, 0, 0, 0, 0, width, height, false, true, false, false);
            //? if >=21.4
            /*imageIn.upload(0, 0, 0, 0, 0, width, height, false);*/
            //?}
            // 1.21.5 AbstractTexture.getId 删、NativeImage.upload 删、纹理分配 GpuDevice 化：
            //仿 DynamicTexture（neoforge-21.5.98-sources DynamicTexture.java:29-39）
            //? if >=21.5 && <21.6 {
            /*this.texture = RenderSystem.getDevice().createTexture(() -> "ysm_outer_file_texture", TextureFormat.RGBA8, width, height, 1);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, imageIn);*/
            //?}
            // 1.21.6 createTexture 七参形（usage flags, depth, mips——21.6 GpuDevice.java:29 实证，
            // Supplier/String 双形）；1.21.8 分界证伪（原注释 DynamicTexture.java:42 为 21.8 行号）
            // 1.21.11 GpuTexture.setTextureFilter 删（neoforge-21.11.45-sources GpuTexture.java
            // 零 filter 方法，过滤改由 GpuSampler+GpuDevice.createSampler 承担）——>=21.11 剥离
            // 该行（纹理 NEAREST 采样让位管线默认，功能差入债）；setTextureFilter 分界实证：
            // 21.9/21.10 runServer 全过 vs 21.11 compileJava 找不到符号（server-audit 取证）
            //? if >=21.6 && <21.11 {
            /*this.texture = RenderSystem.getDevice().createTexture(() -> "ysm_outer_file_texture", 5, TextureFormat.RGBA8, width, height, 1, 1);
            this.texture.setTextureFilter(com.mojang.blaze3d.textures.FilterMode.NEAREST, false);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, imageIn);*/
            //?}
            //? if >=21.11 && <26.2 {
            /*this.texture = RenderSystem.getDevice().createTexture(() -> "ysm_outer_file_texture", 5, TextureFormat.RGBA8, width, height, 1, 1);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, imageIn);*/
            //?}
            //? if >=26.2 {
            /*this.texture = RenderSystem.getDevice().createTexture(() -> "ysm_outer_file_texture", 5, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, imageIn);*/
            //?}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(map));
    }

    public Map<ShadersTextureType, ? extends AbstractTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
