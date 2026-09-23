package rip.ysm;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.util.ResourceLocation;
import rip.ysm.legacy1710.LegacyModelLoader;
import rip.ysm.legacy1710.LegacyModelState;
import rip.ysm.legacy1710.LegacySyncChannel;
import rip.ysm.legacy1710.LegacyTestModel;
import rip.ysm.legacy1710.yui1710.YuiModelSelectScreen1710;

/**
 * 1.7.10 线入口（legacy-1710-l0-poc → l1-render → L2a 真实装载）。
 *
 * preInit：配置装载（FMLPreInitializationEvent.getModConfigurationDirectory，
 * forge-1.7.10 FMLPreInitializationEvent.java:70 实证）。
 * init：真实 .ysm 装载链（builtin <model.id> 解压→YSMFolderDeserializer→
 * YSMClientMapper→GeoModel→converter→LegacyModelState+真实贴图）；失败回退
 * L1 LegacyTestModel 程序化人形。渲染接缝=RendererLivingEntity.renderModel
 * mixin（1.7.10 无 RenderPlayerEvent）。网络同步 L2b（LegacySyncChannel 双端共面）、
 * GUI 选择 L3a。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LegacyConfig.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 网络同步（L2b）：四包注册+登录/登出事件配对——双端共面
        LegacySyncChannel.init();
        // 真实装载链绑定 Minecraft（mcDataDir 解压落盘）——仅客户端面执行
        boolean real = false;
        if (event.getSide().isClient()) {
            real = LegacyModelLoader.loadDefaultModel();
        }
        if (!real) {
            LegacyModelState.setModel(LegacyTestModel.build(),
                    new ResourceLocation("yes_steve_model", "textures/entity/default.png"));
        }
        System.out.println("[ysm-legacy1710] init done: realModel=" + real
                + " bones=" + LegacyModelState.boneCount()
                + " tex=" + (LegacyModelState.realTexture() != null ? "bound" : "placeholder"));
        // 模型选择 GUI（L3a，Y 键；M-U3 冒烟屏已整屏收编撤除）：仅客户端
        if (event.getSide().isClient()) {
            YuiModelSelectScreen1710.init();
        }
    }
}
