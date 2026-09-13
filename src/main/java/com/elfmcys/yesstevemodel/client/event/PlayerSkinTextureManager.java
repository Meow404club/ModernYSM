package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.api.EventResult;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.PlatformAPI;

import java.util.Map;

public class PlayerSkinTextureManager {

    //? if >=1.21
    /*private static final ResourceLocation STEVE_SKIN = ResourceLocation.parse("textures/entity/player/wide/steve.png");*/
    //? if <1.21
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/player/wide/steve.png");

    //? if >=1.21
    /*private static final ResourceLocation ALEX_SKIN = ResourceLocation.parse("textures/entity/player/slim/alex.png");*/
    //? if <1.21
    private static final ResourceLocation ALEX_SKIN = new ResourceLocation("textures/entity/player/slim/alex.png");

    private static final String STEVE_TEXTURE_ID = "misc/2_steve";

    private static final String ALEX_TEXTURE_ID = "misc/1_alex";

    private PlayerSkinTextureManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        SpecialPlayerRenderEvent.EVENT.register(PlayerSkinTextureManager::onRenderTexture);
    }

    private static EventResult onRenderTexture(SpecialPlayerRenderEvent event) {
        ResourceLocation location;
        if (!YesSteveModel.isAvailable()) {
            return EventResult.pass();
        }
        Player player = event.getPlayer();
        if (isDefaultSkin(event.getModelId()) && (player instanceof AbstractClientPlayer)) {
            AbstractClientPlayer abstractClientPlayer = (AbstractClientPlayer) player;
            location = resolveSkinLocation(abstractClientPlayer, event.getModelId());
            event.setTextureLocation(location);
        }
        return EventResult.pass();
    }

    // 1.21.9 皮肤管线换代：PlayerSkin 移 world.entity.player 包且访问器重组（texture()→
    // body().texturePath()）、SkinManager.getInsecureSkin 删（neoforge-21.10.64 SkinManager.java
    // 方法面实证）→ 同步路径取 AbstractClientPlayer.getSkin()（PlayerInfo→PlayerSkin，即玩家
    // 可见皮肤贴图；未加载时 vanilla 回落 DefaultPlayerSkin）。原 if(false) 语义等价改写：
    // neoforge 各代均不走 getSkinTexture 兜底
    //? if neoforge && >=21.9 {
    /*private static ResourceLocation resolveSkinLocation(AbstractClientPlayer abstractClientPlayer, String modelId) {
        return abstractClientPlayer.getSkin().body().texturePath();
    }
    *///?}
    //? if neoforge && <21.9 {
    /*private static ResourceLocation resolveSkinLocation(AbstractClientPlayer abstractClientPlayer, String modelId) {
        Minecraft minecraft = Minecraft.getInstance();
        net.minecraft.client.resources.PlayerSkin insecureSkin = minecraft.getSkinManager().getInsecureSkin(abstractClientPlayer.getGameProfile());
        return insecureSkin.texture();
    }
    *///?}
    //? if forge {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ResourceLocation resolveSkinLocation(AbstractClientPlayer abstractClientPlayer, String modelId) {
        Minecraft minecraft = Minecraft.getInstance();
        Map insecureSkinInformation = minecraft.getSkinManager().getInsecureSkinInformation(abstractClientPlayer.getGameProfile());
        if (insecureSkinInformation.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            return minecraft.getSkinManager().registerTexture((MinecraftProfileTexture) insecureSkinInformation.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
        }
        return getSkinTexture(modelId);
    }
    //?}

    private static boolean isDefaultSkin(String str) {
        return str.equals(STEVE_TEXTURE_ID) || str.equals(ALEX_TEXTURE_ID);
    }

    private static ResourceLocation getSkinTexture(String str) {
        return str.equals(STEVE_TEXTURE_ID) ? STEVE_SKIN : ALEX_SKIN;
    }
}
