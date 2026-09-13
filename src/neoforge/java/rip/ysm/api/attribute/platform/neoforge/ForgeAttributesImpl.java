package rip.ysm.api.attribute.platform.neoforge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。forge 原类 1.20 展开态活跃面 =
 * ForgeMod.{BLOCK_REACH,ENTITY_REACH,SWIM_SPEED,ENTITY_GRAVITY}.get()；neoforge 对应
 * NeoForgeMod 同名常量，类型为 Holder&lt;Attribute&gt;（20.4.251 javap 实证）→ .value()；
 * NAMETAG_DISTANCE 对应 forge NAMETAG_DISTANCE。
 */
public final class ForgeAttributesImpl {
    private ForgeAttributesImpl() {
    }

    public static Attribute blockReach() {
        return NeoForgeMod.BLOCK_REACH.value();
    }

    public static Attribute entityReach() {
        return NeoForgeMod.ENTITY_REACH.value();
    }

    public static Attribute swimSpeed() {
        return NeoForgeMod.SWIM_SPEED.value();
    }

    public static Attribute entityGravity() {
        return NeoForgeMod.ENTITY_GRAVITY.value();
    }

    public static Attribute stepHeightAddition() {
        return NeoForgeMod.STEP_HEIGHT.value();
    }

    public static Attribute nametagDistance() {
        return NeoForgeMod.NAMETAG_DISTANCE.value();
    }

    public static double getValue(net.minecraft.world.entity.LivingEntity entity, Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        return entity.getAttributeValue(attribute);
    }
}
