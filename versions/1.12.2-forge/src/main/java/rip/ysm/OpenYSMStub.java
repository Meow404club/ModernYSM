package rip.ysm;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyModelLoader;
import rip.ysm.legacy122.LegacyRenderHook;
import rip.ysm.legacy122.LegacySyncChannel;
import rip.ysm.legacy122.LegacyTestModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * 1.12.2 线入口（legacy-1222-l1-render，L2 legacy-1222-l2-full 接真实装载）。
 *
 * init：注册 LegacyRenderHook（RenderPlayerEvent.Pre 拦截→翻译层）。
 * L2 模型面：真实 .ysm 装载链（内置 builtin/default 解压→YSMFolderDeserializer→
 * YSMClientMapper.buildParsedBundle→真实 GeoModel+真实贴图）成功即替换 state；
 * 失败回退 L1 LegacyTestModel 程序化人形（fallback 保留）。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LegacyRenderHook.class);
        MinecraftForge.EVENT_BUS.register(LegacySyncChannel.class);
        LegacySyncChannel.init();
        boolean real = LegacyModelLoader.loadDefaultModel();
        if (!real) {
            LegacyModelState.setBundle(null, LegacyTestModel.build());
        }
        System.out.println("[ysm-legacy122] init done: hook registered, realModel=" + real);
    }
}
