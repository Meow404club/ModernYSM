package com.elfmcys.yesstevemodel.client.compat.immersivemelodies.platform.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingEntityFrameState;
import immersive_melodies.client.MelodyProgress;
import immersive_melodies.client.MelodyProgressManager;
import immersive_melodies.client.animation.EntityModelAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
//? if >=1.21 {
/*import com.elfmcys.yesstevemodel.util.YsmFrame;*/
//?}

public class ImmersiveMelodiesBinding {
    public static void registerControllerFunctions(CtrlBinding binding) {
        binding.livingEntityVar("im_pitch", ctx -> {
            Object tracker = ctx.geoInstance().getPositionTracker();
            if (tracker instanceof LivingEntityFrameState) {
                return ((LivingEntityFrameState<?>) tracker).getImmersiveMelodiesData().pitch;
            }
            return 0.0f;
        });
        binding.livingEntityVar("im_volume", ctx -> {
            Object tracker = ctx.geoInstance().getPositionTracker();
            if (tracker instanceof LivingEntityFrameState) {
                return ((LivingEntityFrameState<?>) tracker).getImmersiveMelodiesData().volume;
            }
            return 0.0f;
        });
        binding.livingEntityVar("im_current", ctx -> {
            Object tracker = ctx.geoInstance().getPositionTracker();
            if (tracker instanceof LivingEntityFrameState) {
                return ((LivingEntityFrameState<?>) tracker).getImmersiveMelodiesData().current;
            }
            return 0.0f;
        });
        binding.livingEntityVar("im_delta", ctx -> {
            Object tracker = ctx.geoInstance().getPositionTracker();
            if (tracker instanceof LivingEntityFrameState) {
                return ((LivingEntityFrameState<?>) tracker).getImmersiveMelodiesData().delta;
            }
            return 0L;
        });
        binding.livingEntityVar("im_time", ctx -> {
            Object tracker = ctx.geoInstance().getPositionTracker();
            if (tracker instanceof LivingEntityFrameState) {
                return ((LivingEntityFrameState<?>) tracker).getImmersiveMelodiesData().time;
            }
            return 0L;
        });
    }

    public static void updateInstrumentData(LivingEntity entity, ImmersiveMelodiesCompat.ImmersiveMelodiesData imData) {
        if (EntityModelAnimator.getInstrument(entity) != null) {
            //? if >=1.21
            /*float frameTime = (Minecraft.getInstance().isPaused() ? 0.0f : YsmFrame.partialTick(Minecraft.getInstance())) + entity.tickCount;*/
            //? if <1.21
            float frameTime = (Minecraft.getInstance().isPaused() ? 0.0f : Minecraft.getInstance().getFrameTime()) + entity.tickCount;
            MelodyProgress progress = MelodyProgressManager.INSTANCE.getProgress(entity);
            progress.visualTick(frameTime);
            imData.pitch = progress.getCurrentPitch();
            imData.volume = progress.getCurrentVolume();
            imData.current = progress.getCurrent();
            imData.delta = progress.delta();
            imData.time = progress.getTime();
        }
    }
}