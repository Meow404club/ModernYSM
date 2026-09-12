package rip.ysm.harness;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.gui.AnimationRouletteScreen;
import com.elfmcys.yesstevemodel.client.gui.DisclaimerScreen;
import com.elfmcys.yesstevemodel.client.gui.DownloadScreen;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerConfigScreen;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.client.gui.ModelInfoScreen;
import com.elfmcys.yesstevemodel.client.gui.ModelUploadScreen;
import com.elfmcys.yesstevemodel.client.gui.OpenModelFolderScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerTextureScreen;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import net.minecraft.client.Minecraft;
import rip.ysm.gui.ModernAnimationRouletteScreen;
import rip.ysm.gui.ModelSettingsScreen;
import rip.ysm.gui.ModernModelInfoScreen;
import rip.ysm.gui.ModernPlayerTextureScreen;

/**
 * 走查屏名 → Screen 实例。所有目标类为共享源（同一 .java 双版本编译），
 * 构造器签名双版本一致，故本表零 stonecutter 条件。
 * OptionScreen 为抽象基类，其覆盖由 modern_texture/modern_info/extraconfig/settings 四屏承担。
 */
final class HarnessScreens {

    private HarnessScreens() {
    }

    static net.minecraft.client.gui.screens.Screen forName(Minecraft mc, String name) {
        PlayerCapability cap = mc.player == null ? null : PlayerCapability.get(mc.player).orElse(null);
        if (cap == null) {
            return null;
        }
        ModelAssembly asm = cap.getModelAssembly();
        String modelId = cap.getModelId();
        switch (name) {
            case "disclaimer": return new DisclaimerScreen();
            case "playermodel": return new PlayerModelScreen();
            case "texture": return new PlayerTextureScreen(new PlayerModelScreen(), modelId, asm);
            case "modern_texture": return new ModernPlayerTextureScreen(new PlayerModelScreen(), modelId, asm);
            case "info": return new ModelInfoScreen(new PlayerModelScreen(), asm);
            case "modern_info": return new ModernModelInfoScreen(new PlayerModelScreen(), asm);
            case "upload": return new ModelUploadScreen(new PlayerModelScreen());
            case "download": return new DownloadScreen(new PlayerModelScreen());
            case "folder": return new OpenModelFolderScreen(new PlayerModelScreen());
            case "extrarender": return new ExtraPlayerRenderScreen();
            case "extraconfig": return new ExtraPlayerConfigScreen(new PlayerModelScreen());
            case "settings": return new ModelSettingsScreen(asm, cap, null, null);
            case "roulette": {
                AnimationRouletteScreen.setInitialSubmenu(
                        com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool.EMPTY);
                return new AnimationRouletteScreen(
                        asm.getModelData().getModelProperties().getExtraAnimationButtons(),
                        asm.getModelData().getModelProperties().getExtraAnimationClassify(), asm, cap);
            }
            case "modern_roulette": return new ModernAnimationRouletteScreen(modelId, asm, cap);
            default: return null;
        }
    }
}
