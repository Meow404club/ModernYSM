// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.network.message.FeedbackData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
public final class TouhouMaidCompat {

    private TouhouMaidCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void init() {
    }

    public static boolean isMaidEntity(Entity entity) {
        return false;
    }

    public static void handleProjectileOwner(Projectile projectile, Entity entity) {
    }

    public static void registerAnimationRoulette(Entity entity, String str, int i) {
    }

    public static void applyFeedback(Entity entity, FeedbackData message) {
    }

    public static void playMaidAnimation(Entity entity, String str) {
    }
}
