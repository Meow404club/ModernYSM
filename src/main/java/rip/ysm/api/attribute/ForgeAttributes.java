package rip.ysm.api.attribute;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;
//? if neoforge
/*import rip.ysm.api.attribute.platform.neoforge.ForgeAttributesImpl;*/
//? if forge
import rip.ysm.api.attribute.platform.forge.ForgeAttributesImpl;

public final class ForgeAttributes {

    private ForgeAttributes() {
    }

    @Nullable
    public static Attribute blockReach() {
        return ForgeAttributesImpl.blockReach();
    }

    @Nullable
    public static Attribute entityReach() {
        return ForgeAttributesImpl.entityReach();
    }

    @Nullable
    public static Attribute swimSpeed() {
        return ForgeAttributesImpl.swimSpeed();
    }

    @Nullable
    public static Attribute entityGravity() {
        return ForgeAttributesImpl.entityGravity();
    }

    @Nullable
    public static Attribute stepHeightAddition() {
        return ForgeAttributesImpl.stepHeightAddition();
    }

    @Nullable
    public static Attribute nametagDistance() {
        return ForgeAttributesImpl.nametagDistance();
    }

    public static double getValue(LivingEntity entity, @Nullable Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        // 1.20.5+ vanilla getAttributeValue 收 Holder<Attribute>（vanilla-1.20.6 LivingEntity.java:1897）
        // → neoforge >=1.20.5 委托分代孪生 wrapAsHolder 包裹；其余线 Attribute 直取
        //? if neoforge && >=1.20.5
        /*return ForgeAttributesImpl.getValue(entity, attribute, defaultValue);*/
        //? if forge || neoforge && <1.20.5
        return entity.getAttributeValue(attribute);
    }
}
