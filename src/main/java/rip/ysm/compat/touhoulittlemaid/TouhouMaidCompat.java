package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.network.message.FeedbackData;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import rip.ysm.compat.touhoulittlemaid.platform.forge.TouhouMaidCompatImpl;

public final class TouhouMaidCompat {

    private TouhouMaidCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        return TouhouMaidCompatImpl.isLoaded();
    }

    @ExpectPlatform
    public static void init() {
        TouhouMaidCompatImpl.init();
    }

    @ExpectPlatform
    public static boolean isMaidEntity(Entity entity) {
        return TouhouMaidCompatImpl.isMaidEntity(entity);
    }

    @ExpectPlatform
    public static void handleProjectileOwner(Projectile projectile, Entity entity) {
        TouhouMaidCompatImpl.handleProjectileOwner(projectile, entity);
    }

    @ExpectPlatform
    public static void registerAnimationRoulette(Entity entity, String str, int i) {
        TouhouMaidCompatImpl.registerAnimationRoulette(entity, str, i);
    }

    @ExpectPlatform
    public static void applyFeedback(Entity entity, FeedbackData message) {
        TouhouMaidCompatImpl.applyFeedback(entity, message);
    }

    @ExpectPlatform    @Environment(EnvType.CLIENT)
    public static void playMaidAnimation(Entity entity, String str) {
        throw new AssertionError();
    }
}
