package rip.ysm.compat.touhoulittlemaid;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.elfmcys.yesstevemodel.network.message.FeedbackData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import rip.ysm.compat.touhoulittlemaid.platform.forge.TouhouMaidCompatImpl;

public final class TouhouMaidCompat {

    private TouhouMaidCompat() {
    }

    public static boolean isLoaded() {
        return TouhouMaidCompatImpl.isLoaded();
    }

    public static void init() {
        TouhouMaidCompatImpl.init();
    }

    public static boolean isMaidEntity(Entity entity) {
        return TouhouMaidCompatImpl.isMaidEntity(entity);
    }

    public static void handleProjectileOwner(Projectile projectile, Entity entity) {
        TouhouMaidCompatImpl.handleProjectileOwner(projectile, entity);
    }

    public static void registerAnimationRoulette(Entity entity, String str, int i) {
        TouhouMaidCompatImpl.registerAnimationRoulette(entity, str, i);
    }

    public static void applyFeedback(Entity entity, FeedbackData message) {
        TouhouMaidCompatImpl.applyFeedback(entity, message);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playMaidAnimation(Entity entity, String str) {
        TouhouMaidCompatImpl.playMaidAnimation(entity, str);
    }
}
