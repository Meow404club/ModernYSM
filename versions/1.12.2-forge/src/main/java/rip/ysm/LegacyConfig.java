package rip.ysm;

import net.minecraftforge.common.config.Configuration;
import rip.ysm.legacy122.LegacyModelRegistry;

import java.io.File;

/**
 * 1.12.2 配置面（legacy-1222-l2-full 批④）。
 *
 * forge 1.12.x Configuration 类（Configuration.java:105 File 构造 /
 * :1495 getString(name, category, default, comment) 实证）。
 * ponytail: 配置文件面（GUI 全量太重，L2 降级——任务卡明示可降级）；
 * 改 config/openysm-legacy122.cfg 的 model.id 行 + 重启即切换（重载面不做）。
 */
public final class LegacyConfig {

    private static final String CATEGORY = "model";
    private static final String MODEL_ID_KEY = "id";

    private static String modelId = LegacyModelRegistry.DEFAULT_MODEL_ID;

    private LegacyConfig() {
    }

    /** preInit 调：读 config/openysm-legacy122.cfg 的 model.id 行。 */
    public static void load(File configDir) {
        Configuration cfg = new Configuration(new File(configDir, "openysm-legacy122.cfg"));
        cfg.load();
        modelId = cfg.getString(MODEL_ID_KEY, CATEGORY,
                LegacyModelRegistry.DEFAULT_MODEL_ID, "YSM model id (builtin folder under assets/yes_steve_model/builtin)");
        if (cfg.hasChanged()) {
            cfg.save();
        }
        System.out.println("[ysm-legacy122] config loaded: model.id=" + modelId);
    }

    public static String modelId() {
        return modelId;
    }
}
