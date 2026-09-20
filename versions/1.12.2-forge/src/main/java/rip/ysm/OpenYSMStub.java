package rip.ysm;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import rip.ysm.legacy122.LegacyModelState;
import rip.ysm.legacy122.LegacyRenderHook;
import rip.ysm.legacy122.LegacyTestModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * 1.12.2 线入口（legacy-1222-l1-render）。
 *
 * init：注册 LegacyRenderHook（RenderPlayerEvent.Pre 拦截→翻译层）。
 * L1 模型面：.ysm 文件装载链（YSMFolderDeserializer→YSMClientMapper）的传递闭包
 * 落在重 MC 面（NativeLibLoader Component/GeckoLibCache client.animation.molang，
 * 排除收敛实测 51+ 锁死），L1 不接——程序化盒状人形 GeoModel 走完整
 * 烘焙几何→骨矩阵→GL11 翻译层管线（几何/动画/渲染链全为真）。
 * 文件装载+完整 molang 状态机=L2（如实标注）。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LegacyRenderHook.class);
        LegacyModelState.setBundle(null, LegacyTestModel.build());
        System.out.println("[ysm-legacy122] init done: hook registered, test model built");
    }
}
