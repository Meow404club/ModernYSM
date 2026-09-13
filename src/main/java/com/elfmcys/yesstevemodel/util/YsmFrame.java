package com.elfmcys.yesstevemodel.util;

import net.minecraft.client.Minecraft;

/**
 * 部分刻（partial tick）取值跨版本工厂。1.20.x 的 {@code Minecraft.getFrameTime()} 在 1.21 删除
 * （vanilla-1.21.1 Minecraft.java:2649 getTimer() + DeltaTracker.getGameTimeDeltaPartialTick(boolean)
 * 实证），语义等价（当前帧内插值）。全仓调用点收编本工厂，后续版本只需改本文件一处。
 * 说明文字置条件块外：活跃分支内裸 // 会被 stonecutter 剥前缀变代码。
 */
public final class YsmFrame {
    private YsmFrame() {
    }

    public static float partialTick(Minecraft minecraft) {
        //? if >=1.21 {
        /*return minecraft.getTimer().getGameTimeDeltaPartialTick(false);
         *///?} else {
        return minecraft.getFrameTime();
        //?}
    }
}
