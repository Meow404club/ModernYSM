package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.util.Map;

/**
 * 1.7.10 分代 ITextureMap（legacy1710-l2a-model-load；共享版同名接口的孪生，
 * 1.12.2 twin ITextureMap.java 逐字同构——AbstractTexture 两代同包同名）。
 */
public interface ITextureMap {
    Map<ShadersTextureType, ? extends AbstractTexture> getSuffixTextures();
}
