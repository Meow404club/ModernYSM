package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

//? if >=26.1 && <26.2
/*import com.elfmcys.yesstevemodel.platform.neoforge.firstperson.FirstPersonCompat;*/
//? if <26.1 || >=26.2
import rip.ysm.compat.firstperson.FirstPersonCompat;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.variable.IValueEvaluator;
import com.elfmcys.yesstevemodel.util.CameraUtil;
//? if >=1.16.2
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.player.Player;

public class FirstPersonModHide implements IValueEvaluator<Boolean, IContext<Player>> {
    @Override
    public Boolean eval(IContext<Player> ctx) {
        //? if <1.16.2 {
        /*if (!ctx.animationEvent().isFirstPerson() && FirstPersonCompat.isLoaded() && CameraUtil.getCameraType(ctx) == 0) {
            return FirstPersonCompat.shouldHideHead();
        }
        return false;*/
        //?}
        //? if (>=1.16.2 && <26.1) || >=26.2 {
        if (!ctx.animationEvent().isFirstPerson() && FirstPersonCompat.isLoaded() && CameraUtil.getCameraType(ctx) == CameraType.FIRST_PERSON.ordinal()) {
            return FirstPersonCompat.shouldHideHead();
        }
        return false;
        //?}
        // 26.1 三线（fpm-26x-pr659 逆向追加单）：官方 first_person_mod_hide=FPM 旗标
        // 直读、无动画事件门（ysm2612-reverse OO00o0OOoO0oo0000oOoo0oO.java:11）；
        // shouldHideHead()=261 孪生提取期快照，FPM 缺席/非 FP 相位恒 false 同现状。
        //? if >=26.1 && <26.2 {
        /*return FirstPersonCompat.shouldHideHead();*/
        //?}
    }
}