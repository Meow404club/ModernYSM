package com.elfmcys.yesstevemodel.client.upload;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.texture.ITextureMap;
import com.elfmcys.yesstevemodel.util.ResourceCleanupHelper;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ReferenceIntMutablePair;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UploadManager {

    private static final int UPLOAD_TIME_LIMIT_MS = 20;

    private static long textureCounter = 0;

    private static final IdentityHashMap<AbstractTexture, WeakReference<TextureLocatable>> textureCache = new IdentityHashMap<>();

    //? if <1.17
    /*private static final java.util.ArrayDeque<TexPair> pendingUploads = new java.util.ArrayDeque<>();*/
    //? if >=1.17
    private static final Queue<Pair<TextureLocatable, AbstractTexture>> pendingUploads = Queues.newArrayDeque();

    //? if <1.17
    /*private static final ConcurrentHashMap<AbstractTexture, RlInt> expiredTextures = new ConcurrentHashMap<>();*/
    //? if >=1.17
    private static final ConcurrentHashMap<AbstractTexture, ReferenceIntMutablePair<ResourceLocation>> expiredTextures = new ConcurrentHashMap<>();

    private static final Queue<ResourceLocation> pendingReleases = Queues.newArrayDeque();

    public static IResourceLocatable getOrCreateLocatable(AbstractTexture texture, boolean register) {
        return getOrCreateLocatableWithSize(texture, register, 200);
    }

    public static IResourceLocatable getOrCreateLocatableWithSize(AbstractTexture texture, boolean register, int sizeHint) {
        //? if <1.17
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.17
        RenderSystem.assertOnRenderThread();
        WeakReference<TextureLocatable> weakReference = textureCache.get(texture);
        if (weakReference != null) {
            TextureLocatable locatable = weakReference.get();
            if (locatable != null) {
                if (register && !locatable.registered) {
                    registerTexture(texture, locatable);
                }
                return locatable;
            }
            textureCache.remove(texture);
        }
        //? if <1.17
        /*RlInt removed = expiredTextures.remove(texture);*/
        //? if >=1.17
        ReferenceIntMutablePair<ResourceLocation> removed = expiredTextures.remove(texture);
        TextureLocatable locatable;
        if (removed != null) {
            locatable = new TextureLocatable(removed.first(), sizeHint);
        } else {
            locatable = new TextureLocatable(sizeHint);
        }
        if (texture instanceof ITextureMap) {
            for (AbstractTexture suffixTexture : ((ITextureMap) texture).getSuffixTextures().values()) {
                if (locatable.suffixTextures == null)
                    locatable.suffixTextures = new ArrayList<>(2);

                locatable.suffixTextures.add(getOrCreateLocatableWithSize(suffixTexture, register, sizeHint));
            }
        }
        textureCache.put(texture, new WeakReference<>(locatable));
        if (register) {
            registerTexture(texture, locatable);
        } else {
            //? if <1.17
            /*pendingUploads.add(new TexPair(locatable, texture));*/
            //? if >=1.17
            pendingUploads.add(Pair.of(locatable, texture));
        }
        return locatable;
    }

    public static void removeTexture(AbstractTexture abstractTexture) {
        //? if <1.17
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.17
        RenderSystem.assertOnRenderThread();
        textureCache.remove(abstractTexture);
    }

    public static void processPendingUploads() {
        //? if <1.17
        /*RenderSystem.assertThread(RenderSystem::isOnRenderThread);*/
        //? if >=1.17
        RenderSystem.assertOnRenderThread();
        if (!expiredTextures.isEmpty()) {
            //? if <1.17
            /*Iterator<Map.Entry<AbstractTexture, RlInt>> it = expiredTextures.entrySet().iterator();*/
            //? if >=1.17
            Iterator<Map.Entry<AbstractTexture, ReferenceIntMutablePair<ResourceLocation>>> it = expiredTextures.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<AbstractTexture, ?> next = it.next();
                //? if <1.17 {
                /*RlInt rlInt = (RlInt) next.getValue();
                if (rlInt.second <= 0) {
                    pendingReleases.add(rlInt.first);
                    it.remove();
                } else {
                    rlInt.second = rlInt.second - 1;
                }
                 *///?} else {
                int iSecondInt = ((ReferenceIntMutablePair<ResourceLocation>) next.getValue()).secondInt();
                if (iSecondInt <= 0) {
                    pendingReleases.add(((ReferenceIntMutablePair<ResourceLocation>) next.getValue()).first());
                    it.remove();
                } else {
                    ((ReferenceIntMutablePair<ResourceLocation>) next.getValue()).second(iSecondInt - 1);
                }
                //?}
            }
        }
        StopWatch stopWatchCreateStarted = StopWatch.createStarted();
        do {
            //? if <1.17
            /*TexPair pairPoll = pendingUploads.poll();*/
            //? if >=1.17
            Pair<TextureLocatable, AbstractTexture> pairPoll = pendingUploads.poll();
            if (pairPoll != null) {
                registerTexture(pairPoll.right(), pairPoll.left());
            } else {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                do {
                    ResourceLocation resourceLocationPoll = pendingReleases.poll();
                    if (resourceLocationPoll != null) {
                        textureManager.release(resourceLocationPoll);
                    } else {
                        return;
                    }
                } while (stopWatchCreateStarted.getTime() < UPLOAD_TIME_LIMIT_MS);
                return;
            }
        } while (stopWatchCreateStarted.getTime() < UPLOAD_TIME_LIMIT_MS);
    }

    private static void registerTexture(AbstractTexture texture, TextureLocatable locatable) {
        if (!locatable.registered) {
            Minecraft.getInstance().getTextureManager().register(locatable.resourceLocation, texture);
            ResourceCleanupHelper.registerBiCleanup(locatable, locatable.resourceLocation, locatable.resolution, (resourceLocation, rlcNum) -> {
                //? if <1.17 {
                /*expiredTextures.put(texture, new RlInt(resourceLocation, rlcNum));
                 *///?} else {
                expiredTextures.put(texture, ReferenceIntMutablePair.of(resourceLocation, rlcNum));
                //?}
            });
            locatable.markRegistered();
        }
    }

    private static class TextureLocatable implements IResourceLocatable {

        private final ResourceLocation resourceLocation;

        private final int resolution;

        private List<IResourceLocatable> suffixTextures;

        private volatile boolean registered;

        public TextureLocatable(ResourceLocation resourceLocation, int resolution) {
            this.resourceLocation = resourceLocation;
            this.resolution = resolution;
        }

        TextureLocatable(int resolution) {
            this.resourceLocation = new ResourceLocation(YesSteveModel.MOD_ID, "textures/" + ++textureCounter);
            this.resolution = resolution;
            this.registered = false;
        }

        @Override
        public Optional<ResourceLocation> getResourceLocation() {
            return this.registered ? Optional.of(this.resourceLocation) : Optional.empty();
        }

        public void markRegistered() {
            this.registered = true;
        }
    }

    // fastutil Pair/ReferenceIntMutablePair 为 8.3.0+ API（1.16.5 打包 8.2.1）：
    // <1.17 轴用文件内私有 holder 同构替代（字段语义/时序一致）
    //? if <1.17 {
    /*private static final class TexPair {
        final TextureLocatable left;
        final AbstractTexture right;

        TexPair(TextureLocatable left, AbstractTexture right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class RlInt {
        final ResourceLocation first;
        int second;

        RlInt(ResourceLocation first, int second) {
            this.first = first;
            this.second = second;
        }
    }
     *///?}
}
