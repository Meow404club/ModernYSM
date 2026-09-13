package rip.ysm.api.attribute.platform.neoforge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.20.5+ 断裂：NeoForgeMod 的 BLOCK_REACH/ENTITY_REACH/ENTITY_GRAVITY/STEP_HEIGHT 被 vanilla
 * 吸收（vanilla-1.20.6 Attributes.java：BLOCK_INTERACTION_RANGE:27/ENTITY_INTERACTION_RANGE:30/
 * GRAVITY:42/STEP_HEIGHT:72），SWIM_SPEED/NAMETAG_DISTANCE 留守 NeoForgeMod（:204-205，类型
 * Holder&lt;Attribute&gt;）→ .value() 还原 Attribute 门面。共享 ForgeAttributes.getValue 对
 * >=1.20.5 分支委托本类 getValue（vanilla getAttributeValue 1.20.5+ 收
 * Holder&lt;Attribute&gt;，LivingEntity.java:1897 → BuiltInRegistries.ATTRIBUTE.wrapAsHolder 包裹，
 * Registry 内值取回 canonical reference holder，与 forge .get() 语义对齐）。
 */
public final class ForgeAttributesImpl {
    private ForgeAttributesImpl() {
    }

    public static Attribute blockReach() {
        return Attributes.BLOCK_INTERACTION_RANGE.value();
    }

    public static Attribute entityReach() {
        return Attributes.ENTITY_INTERACTION_RANGE.value();
    }

    public static Attribute swimSpeed() {
        return net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED.value();
    }

    public static Attribute entityGravity() {
        return Attributes.GRAVITY.value();
    }

    public static Attribute stepHeightAddition() {
        return Attributes.STEP_HEIGHT.value();
    }

    public static Attribute nametagDistance() {
        return net.neoforged.neoforge.common.NeoForgeMod.NAMETAG_DISTANCE.value();
    }

    public static double getValue(net.minecraft.world.entity.LivingEntity entity, Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        return entity.getAttributeValue(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
    }
}
