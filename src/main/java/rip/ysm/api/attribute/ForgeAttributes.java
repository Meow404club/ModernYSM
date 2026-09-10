package rip.ysm.api.attribute;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.attribute.platform.forge.ForgeAttributesImpl;

public final class ForgeAttributes {

    private ForgeAttributes() {
    }

    @ExpectPlatform
    @Nullable
    public static Attribute blockReach() {
        return ForgeAttributesImpl.blockReach();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute entityReach() {
        return ForgeAttributesImpl.entityReach();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute swimSpeed() {
        return ForgeAttributesImpl.swimSpeed();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute entityGravity() {
        return ForgeAttributesImpl.entityGravity();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute stepHeightAddition() {
        return ForgeAttributesImpl.stepHeightAddition();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute nametagDistance() {
        return ForgeAttributesImpl.nametagDistance();
    }

    public static double getValue(LivingEntity entity, @Nullable Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        return entity.getAttributeValue(attribute);
    }
}
