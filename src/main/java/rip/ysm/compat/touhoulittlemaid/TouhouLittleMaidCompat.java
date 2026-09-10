package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.model.ModelResourceBundle;
import com.elfmcys.yesstevemodel.client.model.PlayerModelBundle;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import rip.ysm.compat.touhoulittlemaid.platform.forge.TouhouLittleMaidCompatImpl;

public final class TouhouLittleMaidCompat {

    private TouhouLittleMaidCompat() {
    }

    public static boolean isLoaded() {
        return TouhouLittleMaidCompatImpl.isLoaded();
    }

    public static boolean isMaidEntity(Entity entity) {
        return TouhouLittleMaidCompatImpl.isMaidEntity(entity);
    }

    public static boolean isMaidRideable(Entity entity) {
        return TouhouLittleMaidCompatImpl.isMaidRideable(entity);
    }

    public static boolean isSimplePlanesEntity(Entity entity) {
        return TouhouLittleMaidCompatImpl.isSimplePlanesEntity(entity);
    }

    public static boolean isImmersiveAircraftEntity(Entity entity) {
        return TouhouLittleMaidCompatImpl.isImmersiveAircraftEntity(entity);
    }

    public static boolean isMaidItem(Item item) {
        return TouhouLittleMaidCompatImpl.isMaidItem(item);
    }

    public static String getMaidEntityId(Entity entity) {
        return TouhouLittleMaidCompatImpl.getMaidEntityId(entity);
    }

    public static boolean isMaidSitting(LivingEntity livingEntity) {
        return TouhouLittleMaidCompatImpl.isMaidSitting(livingEntity);
    }

    public static void registerMaidAnimStates(TLMBinding tlmBinding) {
        TouhouLittleMaidCompatImpl.registerMaidAnimStates(tlmBinding);
    }

    public static PlayState handleMaidInteraction(AnimationEvent<LivingAnimatable<?>> event, LivingEntity livingEntity, Entity entity) {
        return TouhouLittleMaidCompatImpl.handleMaidInteraction(event, livingEntity, entity);
    }

    public static boolean isMaidChatAvailable() {
        return TouhouLittleMaidCompatImpl.isMaidChatAvailable();
    }

    public static void openMaidChat() {
        TouhouLittleMaidCompatImpl.openMaidChat();
    }

    public static Object buildControllers(PlayerModelBundle modelBundle, ModelResourceBundle resourceBundle) {
        return TouhouLittleMaidCompatImpl.buildControllers(modelBundle, resourceBundle);
    }

    @Nullable
    public static GeoReplacedEntityRenderer<?, ?> getMaidPreviewRenderer(LivingAnimatable<?> animatable) {
        return TouhouLittleMaidCompatImpl.getMaidPreviewRenderer(animatable);
    }
}
