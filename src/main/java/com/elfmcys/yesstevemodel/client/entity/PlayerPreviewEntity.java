package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.animation.AnimationTracker;
import com.elfmcys.yesstevemodel.client.animation.molang.PhysicsManager;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.log.ILogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PlayerPreviewEntity extends CustomPlayerEntity implements IPreviewAnimatable {

    private final AnimationTracker animationStateMachine;

    private boolean customAnimationActive;

    public PlayerPreviewEntity() {
        super(new DummyPlayer(), false, false);
        this.animationStateMachine = new AnimationTracker();
    }

    @Override
    public void resetModel() {
        this.animationStateMachine.setQueuedAnimation(StringPool.EMPTY);
        this.animationStateMachine.setCurrentAnimation(StringPool.EMPTY);
        this.animationStateMachine.setPreviousAnimation(StringPool.EMPTY);
        this.customAnimationActive = false;
        super.resetModel();
    }

    @Override
    @NotNull
    public AnimationTracker getAnimationStateMachine() {
        return this.animationStateMachine;
    }

    @Override
    public PhysicsManager getPhysicsManager() {
        return this.physicsManager;
    }

    @Override
    public void setCustomAnimationActive(boolean active) {
        this.customAnimationActive = active;
    }

    @Override
    public boolean isDebugMode() {
        return true;
    }

    @Override
    public boolean shouldRenderOverlay() {
        return this.customAnimationActive;
    }

    @Override
    public int getRefreshRate() {
        return ClientTickEvent.getRefreshRate();
    }

    @Override
    public boolean hasCustomTexture() {
        return true;
    }

    @Override
    public AnimationEvent<?> processAnimationImpl(float partialTick, boolean isFirstPerson) {
        Entity entity2 = this.entity;
        if ((entity2 instanceof DummyPlayer) && !((DummyPlayer) entity2).ensureLevel()) {
            return null;
        }
        return super.processAnimationImpl(partialTick, isFirstPerson);
    }

    public static boolean isPreviewPlayer(Player player) {
        return player instanceof DummyPlayer;
    }

    @Override
    public boolean shouldSkipAnimation(AnimationEvent<?> event) {
        return true;
    }

    @Override
    public ILogger getLogger() {
        return null;
    }

    @Override
    @NotNull
    public LivingAnimatable<Player>.TexturedModelWrapper buildRenderShape(ModelAssembly modelAssembly, boolean isActive) {
        return new TexturedModelWrapper(modelAssembly, isActive, false, true, 300);
    }

    private static class DummyPlayer extends AbstractClientPlayer {
        public DummyPlayer() {
            // AbstractClientPlayer 构造器仅 1.19.2 带 ProfilePublicKey 三参（1192:35，可空），
            // 1165:??/1182:42/1194:36/1201 均为 (ClientLevel, GameProfile) 两参
            //? if >=1.19.2 && <1.19.4 {
            /*super(Minecraft.getInstance().level, createGameProfile(), (net.minecraft.world.entity.player.ProfilePublicKey) null);
             *///?} else {
            super(Minecraft.getInstance().level, createGameProfile());
            //?}
        }

        private static GameProfile createGameProfile() {
            UUID uuidRandomUUID = UUID.randomUUID();
            return new GameProfile(uuidRandomUUID, "ysm_" + uuidRandomUUID.toString().replace('-', '_'));
        }

        public boolean isSpectator() {
            return false;
        }

        public boolean isCreative() {
            return false;
        }

        public boolean ensureLevel() {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                // Entity.setLevel 仅 1.16.5（1165:1102 public）与 1.20.1（1201:3360 protected，子类内可调）
                // 存在；1.17~1.19.4 无此方法（1182/1192/1194 Entity.java 全文无 setLevel）→ 反射落
                // level 字段（生产 SRG=field_70170_p，1.8~1.19 全段稳定；MatrixBridge 1165 反射双名先例）。
                // 反射失败=预览停在跳过态（false），不崩。
                //? if <1.17 {
                /*setLevel(clientLevel);
                return true;
                 *///?}
                //? if >=1.17 && <1.20 {
                /*return reflectSetLevel(clientLevel);
                 *///?}
                //? if >=1.20 {
                setLevel(clientLevel);
                return true;
                //?}
            }
            return false;
        }

        //? if >=1.17 && <1.19.4 {
        /*
        private boolean reflectSetLevel(ClientLevel level) {
            for (String fieldName : new String[]{"level", "field_70170_p"}) {
                try {
                    java.lang.reflect.Field f = net.minecraft.world.entity.Entity.class.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(this, level);
                    return true;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return false;
        }
         *///?}
    }
}