package rip.ysm;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.util.ResourceLocation;
import rip.ysm.legacy1710.LegacyModelState;
import rip.ysm.legacy1710.LegacyTestModel;

/**
 * 1.7.10 线入口（legacy-1710-l0-poc → l1-render）。
 *
 * L1：init 挂程序化人形（LegacyTestModel）到渲染状态，RenderPlayer 接缝走
 * RendererLivingEntity.renderModel mixin（1.7.10 无 RenderPlayerEvent）。
 * .ysm 文件解析链与网络同步本卡不做（L2+）。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LegacyModelState.setModel(LegacyTestModel.build(),
                new ResourceLocation("yes_steve_model", "textures/entity/default.png"));
        System.out.println("[ysm-legacy1710] init done: L1 render hook armed, bones="
                + LegacyModelState.boneCount());
    }
}
