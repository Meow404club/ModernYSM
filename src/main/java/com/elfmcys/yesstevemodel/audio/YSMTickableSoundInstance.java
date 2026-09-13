package com.elfmcys.yesstevemodel.audio;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import net.minecraft.client.Minecraft;
//? if <1.17 {
/*import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
 *///?} else {
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
//?}
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.elfmcys.yesstevemodel.util.YsmEntity;
import net.minecraft.world.entity.Entity;

// 1.16.5 无 AbstractTickableSoundInstance（javap：TickableSoundInstance 为 interface）：
// <1.17 轴 extends SimpleSoundInstance implements TickableSoundInstance（补 isStopped 状态位）
//? if <1.17 {
/*public class YSMTickableSoundInstance extends net.minecraft.client.resources.sounds.AbstractSoundInstance
        implements net.minecraft.client.resources.sounds.TickableSoundInstance, IAudioPlayer {

    private boolean stopped;
     *///?} else {
public class YSMTickableSoundInstance extends AbstractTickableSoundInstance implements IAudioPlayer {
    //?}

    public final Entity entity;

    public float targetVolume;

    public YSMTickableSoundInstance(SoundEvent soundEvent, Entity entity) {
        // AbstractTickableSoundInstance 3 参（带 RandomSource）1.19.2 起（1182:9 = 2 参）
        //? if <1.19.2 {
        /*super(soundEvent, SoundSource.PLAYERS);
         *///?} else {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        //?}
        this.targetVolume = 1.0f;
        this.entity = entity;
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }

    //? if <1.17 {
    /*@Override
    public boolean isStopped() {
        return this.stopped;
    }
     *///?}

    public void tick() {
        this.volume = (this.targetVolume * GeneralConfig.SOUND_VOLUME.get().floatValue()) / 100.0f;
        if (YsmEntity.isRemoved(this.entity)) {
            // 1.16.5 无 SoundInstance.stop()（1.19+）：经 SoundManager 停播并置 stopped
            //? if <1.17 {
            /*this.stopped = true;
            Minecraft.getInstance().getSoundManager().stop(this);
            return;
             *///?} else {
            stop();
            return;
            //?}
        }
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }

    public void setVolume(float f) {
        this.targetVolume = f;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public void stopSound() {
        this.attenuation = Attenuation.NONE;
        this.relative = true;
    }

    @Override
    public void release() {
        //? if <1.17 {
        /*this.stopped = true;
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getSoundManager().stop(this);
        });
         *///?} else {
        stop();
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getSoundManager().stop(this);
        });
        //?}
    }

    public void setLooping(boolean z) {
        this.looping = z;
    }
}