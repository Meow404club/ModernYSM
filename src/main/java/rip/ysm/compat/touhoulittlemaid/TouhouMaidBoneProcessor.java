package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import rip.ysm.compat.touhoulittlemaid.platform.forge.TouhouMaidBoneProcessorImpl;

public final class TouhouMaidBoneProcessor {

    private TouhouMaidBoneProcessor() {
    }

    public static Object createLocationBone(AnimatedGeoBone bone) {
        return TouhouMaidBoneProcessorImpl.createLocationBone(bone);
    }

    public static Object createLocationModel(AnimatedGeoModel model) {
        return TouhouMaidBoneProcessorImpl.createLocationModel(model);
    }
}
