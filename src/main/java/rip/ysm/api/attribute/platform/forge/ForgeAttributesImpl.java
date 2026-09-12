package rip.ysm.api.attribute.platform.forge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;

public final class ForgeAttributesImpl {

    private ForgeAttributesImpl() {
    }

    // 1.16.5 ForgeMod 字段面（javap 实证）：仅 REACH_DISTANCE/SWIM_SPEED/NAMETAG_DISTANCE/ENTITY_GRAVITY。
    // BLOCK_REACH/ENTITY_REACH 1.20 才拆分（<1.17 共用 REACH_DISTANCE，语义差记回报）；
    // STEP_HEIGHT_ADDITION 1.20 才有（<1.17 无此属性 → null → ForgeAttributes.getValue 走默认值 0）
    public static Attribute blockReach() {
        //? if <1.17 {
        /*return ForgeMod.REACH_DISTANCE.get();
         *///?}
        //? if >=1.17 && <1.19.4 {
        /*return ForgeMod.REACH_DISTANCE.get();
         *///?}
        //? if >=1.19.4 {
        return ForgeMod.BLOCK_REACH.get();
        //?}
    }

    public static Attribute entityReach() {
        //? if <1.17 {
        /*return ForgeMod.REACH_DISTANCE.get();
         *///?}
        //? if >=1.17 && <1.19.4 {
        /*return ForgeMod.REACH_DISTANCE.get();
         *///?}
        //? if >=1.19.4 {
        return ForgeMod.ENTITY_REACH.get();
        //?}
    }

    public static Attribute swimSpeed() {
        return ForgeMod.SWIM_SPEED.get();
    }

    public static Attribute entityGravity() {
        return ForgeMod.ENTITY_GRAVITY.get();
    }

    public static Attribute stepHeightAddition() {
        //? if <1.17 {
        /*return null;
         *///?} else {
        return ForgeMod.STEP_HEIGHT_ADDITION.get();
        //?}
    }

    public static Attribute nametagDistance() {
        return ForgeMod.NAMETAG_DISTANCE.get();
    }
}
