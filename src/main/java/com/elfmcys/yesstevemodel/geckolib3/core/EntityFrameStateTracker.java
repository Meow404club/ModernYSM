package com.elfmcys.yesstevemodel.geckolib3.core;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
// 1.12.2=Mth→无 lerp（vanilla-mc-1122 MathHelper.java 零 lerp 实证，手写插值）/
// Entity→net.minecraft.entity.Entity/Vec3→Vec3d（Vec3d.java:6 ZERO/:50 subtract(Vec3d)；
// Entity posX/lastTickPosX public，compile jar javap 实证）
//? if >=1.13 {
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
//?}
//? if <1.13 {
/*import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;*/
//?}
import org.jetbrains.annotations.Nullable;

//? if <1.13
/*public class EntityFrameStateTracker<T extends Entity> {*/
//? if >=1.13
public class EntityFrameStateTracker<T extends Entity> {

    public T entity;

    private int currentTick;

    //? if <1.13
    /*private Vec3d lastPosition;*/
    //? if >=1.13
    private Vec3 lastPosition;

    private String cachedModelId;

    public float currentTime;

    public float timeDelta;

    //? if <1.13
    /*private Vec3d positionDelta = Vec3d.ZERO;*/
    //? if >=1.13
    private Vec3 positionDelta = Vec3.ZERO;

    private final IntOpenHashSet animatedEntities = new IntOpenHashSet();

    public EntityFrameStateTracker(T entity) {
        this.entity = entity;
    }

    public void reset() {
        this.animatedEntities.clear();
        this.currentTick = 0;
        this.lastPosition = null;
        //? if <1.13
        /*this.positionDelta = Vec3d.ZERO;*/
        //? if >=1.13
        this.positionDelta = Vec3.ZERO;
        this.cachedModelId = null;
        this.currentTime = 0.0f;
        this.timeDelta = 0.0f;
    }

    public final void updateState(int tickCount, float seekTime, float frameTime) {
        if (this.currentTick < tickCount) {
            onTickUpdate(tickCount, this.currentTick);
            this.currentTick = tickCount;
        }
        if (this.currentTime < seekTime) {
            onTimeUpdate(seekTime, this.currentTime, frameTime);
            this.currentTime = seekTime;
        }
    }

    public void setEntity(T t) {
        this.entity = t;
    }

    public void onTimeUpdate(float f, float f2, float f3) {
        this.timeDelta = f - f2;
        updatePosition(f3);
        this.cachedModelId = null;
    }

    public void onTickUpdate(int i, int i2) {
        this.animatedEntities.clear();
    }

    private void updatePosition(float f) {
        // 1.12.2 无 MathHelper.lerp → 手写 f 内插（lerp(a,b,f)=a+(b-a)*f 同语义）
        //? if <1.13 {
        /*Vec3d vec3 = new Vec3d(this.entity.lastTickPosX + (this.entity.posX - this.entity.lastTickPosX) * f,
                this.entity.lastTickPosY + (this.entity.posY - this.entity.lastTickPosY) * f,
                this.entity.lastTickPosZ + (this.entity.posZ - this.entity.lastTickPosZ) * f);*/
        //?}
        //? if >=1.13 {
        Vec3 vec3 = new Vec3(Mth.lerp(f, this.entity.xo, this.entity.getX()), Mth.lerp(f, this.entity.yo, this.entity.getY()), Mth.lerp(f, this.entity.zo, this.entity.getZ()));
        //?}
        if (this.lastPosition != null) {
            this.positionDelta = vec3.subtract(this.lastPosition);
        }
        this.lastPosition = vec3;
    }

    public boolean markProcessed(int i) {
        return this.animatedEntities.add(i);
    }

    public boolean isProcessed(int i) {
        return this.animatedEntities.contains(i);
    }

    //? if <1.13 {
    /*public Vec3d getPositionDelta() {
        return this.positionDelta;
    }*/
    //?}
    //? if >=1.13 {
    public Vec3 getPositionDelta() {
        return this.positionDelta;
    }
    //?}

    @Nullable
    public String getCachedModelId() {
        return this.cachedModelId;
    }

    public void setCachedModelId(String str) {
        this.cachedModelId = str;
    }

    public float getTimeDelta() {
        return this.timeDelta;
    }
}