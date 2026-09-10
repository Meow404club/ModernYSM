package rip.ysm.compat.touhoulittlemaid.platform.forge;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.platform.forge.MaidEventHandler;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;

final class MaidPreviewRendererAccess {

    private MaidPreviewRendererAccess() {
    }

    static GeoReplacedEntityRenderer<?, ?> get() {
        Object renderer = MaidEventHandler.getMaidRendererRaw();
        return (GeoReplacedEntityRenderer<?, ?>) renderer;
    }
}
