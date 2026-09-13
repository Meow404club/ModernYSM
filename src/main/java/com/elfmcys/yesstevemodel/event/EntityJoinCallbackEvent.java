package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if >=1.19.2 && neoforge
/*import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;*/
//? if >=1.19.2 && forge
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
//? if <1.19.2 {
/*import net.minecraftforge.event.entity.EntityJoinWorldEvent;*/
//?}
import rip.ysm.api.PlatformAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EntityJoinCallbackEvent {

    private static final Cache<Integer, List<Consumer<Entity>>> callbackCache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();

    private EntityJoinCallbackEvent() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(EntityJoinCallbackEvent::onEntityJoinLevel);*/
        //? if forge
        MinecraftForge.EVENT_BUS.addListener(EntityJoinCallbackEvent::onEntityJoinLevel);
    }

    // 事件名反向差：1.16.x 为 EntityJoinWorldEvent（1.19+ 才改名 JoinLevel），且 world 访问器
    // getWorld()（1.16.x）vs getLevel()（1.19+）；getEntity() 两侧同名（javap 实证）。
    //? if >=1.19.2 {
    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        onEntityJoin(event.getEntity(), event.getLevel().isClientSide());
    }
    //?} else {
    /*private static void onEntityJoinLevel(EntityJoinWorldEvent event) {
        onEntityJoin(event.getEntity(), event.getWorld().isClientSide());
    }*/
//?}

    private static void onEntityJoin(Entity entity, boolean clientSide) {
        if (!YesSteveModel.isAvailable() || !clientSide) {
            return;
        }
        List<Consumer<Entity>> list = callbackCache.getIfPresent(entity.getId());
        if (list != null) {
            for (Consumer<Entity> entityConsumer : list) {
                entityConsumer.accept(entity);
            }
        }
        callbackCache.invalidate(entity.getId());
    }

    public static void addCallback(int i, Consumer<Entity> consumer) {
        Minecraft.getInstance().execute(() -> {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                Entity entity = clientLevel.getEntity(i);
                if (entity != null) {
                    consumer.accept(entity);
                } else {
                    addToCallbackList(i, consumer);
                }
            }
        });
    }

    private static void addToCallbackList(int i, Consumer<Entity> consumer) {
        List<Consumer<Entity>> arrayList = callbackCache.getIfPresent(i);
        if (arrayList == null) {
            arrayList = new ArrayList<>(3);
            callbackCache.put(i, arrayList);
        }
        arrayList.add(consumer);
    }
}