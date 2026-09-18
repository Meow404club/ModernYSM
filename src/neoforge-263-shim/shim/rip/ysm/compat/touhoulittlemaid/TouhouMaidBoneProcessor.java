// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
// core（AnimatedGeoBone/AnimatedGeoModel）在 TouhouMaidCompat.isLoaded() 守卫内调用，
// createLocation* 返回 null 即"无女仆骨骼数据"。
package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;

public final class TouhouMaidBoneProcessor {

    private TouhouMaidBoneProcessor() {
    }

    public static Object createLocationBone(AnimatedGeoBone bone) {
        return null;
    }

    public static Object createLocationModel(AnimatedGeoModel model) {
        return null;
    }
}
