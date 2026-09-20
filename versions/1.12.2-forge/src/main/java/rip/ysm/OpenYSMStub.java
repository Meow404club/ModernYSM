package rip.ysm;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * 1.12.2 线构建 POC stub（legacy-1222-l0-poc）：@Mod + FMLInitializationEvent 引用
 * 证明 forge 1.12.2 编译面（SRG 映射）可用。L1 渲染翻译层另卡，不在此平铺。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // ponytail: stub 无逻辑，L1 翻译层落地后本类即替换
    }
}
