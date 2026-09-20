package rip.ysm;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * 1.12.2 线入口（legacy-1222-l1-render；L0 stub 替换）。
 *
 * init：注册 LegacyRenderHook（RenderPlayerEvent.Pre 拦截→翻译层）+ 装载默认模型。
 * 装载链复用共享面最小路径：YSMFolderDeserializer+YSMClientMapper.buildParsedBundle
 * →LegacyModelState（ClientModelManager.onModelDataReceived 的现代网络/装配分发链
 * L1 不接）。built 目录优先（ClientModelManager.java:203 同款），jar 内置 fallback
 * 走 ClientModelManager.java:222-241 同款 zipfs。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        YesSteveModel.init();
        MinecraftForge.EVENT_BUS.register(LegacyRenderHook.class);
        loadDefaultModelL1();
    }

    private void loadDefaultModelL1() {
        java.nio.file.Path built = com.elfmcys.yesstevemodel.model.ServerModelManager.BUILT.resolve("default");
        if (java.nio.file.Files.isDirectory(built)) {
            if (loadFrom(built)) {
                YesSteveModel.LOGGER.info("[ysm-legacy122] default model loaded from built dir");
            }
            return;
        }
        // jar 内置 fallback（ClientModelManager.java:222-241 同款 zipfs 链）
        try {
            java.net.URL url = YesSteveModel.class.getResource("/assets/yes_steve_model/builtin/default");
            if (url == null) {
                YesSteveModel.LOGGER.error("[ysm-legacy122] builtin default model not found on classpath");
                return;
            }
            java.net.URI uri = url.toURI();
            java.nio.file.Path path;
            java.nio.file.FileSystem jarFs = null;
            if ("jar".equals(uri.getScheme())) {
                try {
                    jarFs = java.nio.file.FileSystems.getFileSystem(uri);
                } catch (java.nio.file.FileSystemNotFoundException e) {
                    jarFs = java.nio.file.FileSystems.newFileSystem(uri, java.util.Collections.emptyMap());
                }
                path = jarFs.getPath("/assets/yes_steve_model/builtin/default");
            } else {
                path = java.nio.file.Paths.get(uri);
            }
            if (loadFrom(path)) {
                YesSteveModel.LOGGER.info("[ysm-legacy122] default model loaded from classpath");
            }
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[ysm-legacy122] failed to load default model (classpath)", e);
        }
    }

    private boolean loadFrom(java.nio.file.Path dir) {
        try (com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer deserializer =
                     new com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer(dir)) {
            com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel rawModel = deserializer.deserialize();
            ClientModelInfo parsed = com.elfmcys.yesstevemodel.resource.YSMClientMapper
                    .buildParsedBundle(rawModel, "default");
            LegacyModelState.setBundle(parsed);
            return true;
        } catch (Exception e) {
            YesSteveModel.LOGGER.error("[ysm-legacy122] failed to load default model from " + dir, e);
            return false;
        }
    }
}
