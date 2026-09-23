package rip.ysm;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyModelLoader;
import rip.ysm.legacy122.LegacyModelSelectScreen;
import rip.ysm.legacy122.LegacySyncChannel;
import rip.ysm.legacy122.LegacyTestModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * 1.12.2 线入口（legacy-1222-l1-render，L2 legacy-1222-l2-full 接真实装载；
 * wave-d-b2：渲染接缝改 RenderLivingBase.renderModel mixin——RenderLivingBaseMixin
 * 经 jar 清单 MixinConfigs→mixinbooter 装载，无需事件注册）。
 *
 * L2 模型面：真实 .ysm 装载链（内置 builtin/default 解压→YSMFolderDeserializer→
 * YSMClientMapper.buildParsedBundle→真实 GeoModel+真实贴图）成功即替换 state；
 * 失败回退 L1 LegacyTestModel 程序化人形（fallback 保留）。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LegacyConfig.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LegacySyncChannel.class);
        LegacySyncChannel.init();
        // 模型装载面仅客户端（run3 实证：builtDirOf→Minecraft.getMinecraft() 触
        // SideTransformer 服务端拒绝 net/minecraft/client/Minecraft）；服务端只持
        // uuid→id 登记（LegacyModelRegistry），同步链发 id 字符串不需 GeoModel
        boolean real = false;
        if (event.getSide().isClient()) {
            real = LegacyModelLoader.loadDefaultModel();
            if (!real) {
                LegacyModelState.setBundle(null, LegacyTestModel.build());
            }
            // L3-2 GUI：模型选择键位+列表屏仅客户端（SideOnly 类引用收进 dist 分支）
            LegacyModelSelectScreen.init();
        }
        System.out.println("[ysm-legacy122] init done: hook registered, realModel=" + real
                + ", side=" + (event.getSide().isClient() ? "client" : "server"));
    }
}
