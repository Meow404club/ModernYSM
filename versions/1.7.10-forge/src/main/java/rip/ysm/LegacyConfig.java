package rip.ysm;

import net.minecraftforge.common.config.Configuration;
import rip.ysm.legacy1710.LegacyModelRegistry;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7.10 配置面（legacy1710-l2a-model-load）。
 *
 * 1.12.2 LegacyConfig 的 1.7.10 twin：forge 1.7.10 Configuration 类同名同包
 *（net.minecraftforge.common.config，两代包名相同）且所用 API 签名逐一相同
 *（forge-1.7.10 Configuration.java:85 File 构造 / :1469 getString / :1566
 * getStringList / :629 get(String[],comment) / Property.java:1058 set(String[])
 * 实证）。
 * ponytail: 配置文件面（GUI 全量太重，L3a 再做）；
 * - model.id：全局默认模型 id；
 * - model.assignments：服务端按玩家指派（"uuid=modelId" 行，getStringList 承载），
 *   未指派的玩家回退 model.id（即全 default = L2a 行为不变）；
 * - general.*（wave-d-b4 五项真缺配置补齐，与 1.12.2 LegacyConfig 同构，键名/默认值/
 *   区间与主线逐字对位）：CanSwitchModel/DisableSelfModel/DisableOtherModel/
 *   ShowModelIdFirst 四项有消费点（Select 包/渲染 hook/Yui 屏卡名）；
 *   SearchSuggestionCount/SoundVolume 仅补配置面占位，N/A（声明在 cfg 注释）。
 */
public final class LegacyConfig {

    private static final String CATEGORY = "model";
    private static final String MODEL_ID_KEY = "id";
    private static final String ASSIGNMENTS_KEY = "assignments";
    private static final String GENERAL_CATEGORY = "general";

    private static String modelId = LegacyModelRegistry.DEFAULT_MODEL_ID;
    private static boolean canSwitchModel = true;
    private static boolean disableSelfModel = false;
    private static boolean disableOtherModel = false;
    private static boolean showModelIdFirst = false;
    private static File cfgFile;
    // 1.7.10 C2S handler 跑 netty 线程（无 MinecraftServer.addScheduledTask）→并发化
    private static final Map<UUID, String> ASSIGNMENTS = new ConcurrentHashMap<>();

    private LegacyConfig() {
    }

    /** preInit 调：读 config/openysm-legacy1710.cfg 的 model 面。 */
    public static void load(File configDir) {
        cfgFile = new File(configDir, "openysm-legacy1710.cfg");
        Configuration cfg = new Configuration(cfgFile);
        cfg.load();
        modelId = cfg.getString(MODEL_ID_KEY, CATEGORY,
                LegacyModelRegistry.DEFAULT_MODEL_ID, "YSM model id (builtin folder under assets/yes_steve_model/builtin)");
        canSwitchModel = cfg.getBoolean("CanSwitchModel", GENERAL_CATEGORY, true,
                "Allow players to switch models (mainline ServerConfig.CanSwitchModel)");
        disableSelfModel = cfg.getBoolean("DisableSelfModel", GENERAL_CATEGORY, false,
                "Prevents rendering of self player's model (mainline GeneralConfig)");
        disableOtherModel = cfg.getBoolean("DisableOtherModel", GENERAL_CATEGORY, false,
                "Prevents rendering of other players' models (mainline GeneralConfig)");
        showModelIdFirst = cfg.getBoolean("ShowModelIdFirst", GENERAL_CATEGORY, false,
                "Show model ID first in the selection screen instead of author-filled name (mainline GeneralConfig)");
        // N/A 项（主线键名/区间对位，legacy 无消费面）：SearchSuggestionCount 依赖主线
        // 搜索 GUI（SearchSuggestions 建议列表，legacy Yui 屏无搜索框）；SoundVolume 依赖
        // 动画音效播放链（主线 YSMTickableSoundInstance，legacy 世界路径无音频）。仅落键
        // 占位保持配置面 parity
        cfg.get("SearchSuggestionCount", GENERAL_CATEGORY, 8.0,
                "N/A in legacy: no search GUI (mainline: max entries shown at search list)", 1.0, 30.0);
        cfg.get("SoundVolume", GENERAL_CATEGORY, 100.0,
                "N/A in legacy: no animation sound playback chain (mainline: volume percent)", 0.0, 100.0);
        String[] lines = cfg.getStringList(ASSIGNMENTS_KEY, CATEGORY, new String[0],
                "Per-player model assignment, entries formatted uuid=modelId. Players not listed fall back to model.id");
        ASSIGNMENTS.clear();
        for (String line : lines) {
            int sep = line.indexOf('=');
            if (sep <= 0) {
                continue;
            }
            try {
                ASSIGNMENTS.put(UUID.fromString(line.substring(0, sep).trim()),
                        line.substring(sep + 1).trim());
            } catch (IllegalArgumentException e) {
                System.out.println("[ysm-legacy1710] config bad assignment ignored: " + line);
            }
        }
        if (cfg.hasChanged()) {
            cfg.save();
        }
        System.out.println("[ysm-legacy1710] config loaded: model.id=" + modelId
                + " assignments=" + ASSIGNMENTS.size()
                + " canSwitchModel=" + canSwitchModel
                + " disableSelfModel=" + disableSelfModel
                + " disableOtherModel=" + disableOtherModel
                + " showModelIdFirst=" + showModelIdFirst);
    }

    public static String modelId() {
        return modelId;
    }

    /** 选择门禁（wave-d-b4）：主线 C2SRequestSwitchModelPacket:35 同语义。 */
    public static boolean canSwitchModel() {
        return canSwitchModel;
    }

    public static boolean disableSelfModel() {
        return disableSelfModel;
    }

    public static boolean disableOtherModel() {
        return disableOtherModel;
    }

    public static boolean showModelIdFirst() {
        return showModelIdFirst;
    }

    /** 服务端登录指派（L2b）：uuid=model 行优先，缺省回退全局 model.id。 */
    public static String modelIdFor(UUID uuid) {
        String id = uuid == null ? null : ASSIGNMENTS.get(uuid);
        return id == null || id.isEmpty() ? modelId : id;
    }

    /**
     * GUI 选择落盘（L3a）：更新 uuid 指派并回写 cfg（default/空=移除指派回退
     * model.id）。Property.set+save 面。
     */
    public static void assign(UUID uuid, String modelId) {
        if (uuid == null) {
            return;
        }
        if (modelId == null || modelId.isEmpty()
                || LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)) {
            ASSIGNMENTS.remove(uuid);
        } else {
            ASSIGNMENTS.put(uuid, modelId);
        }
        saveAssignments();
    }

    private static void saveAssignments() {
        if (cfgFile == null) {
            return;
        }
        Configuration cfg = new Configuration(cfgFile);
        cfg.load();
        String[] lines = new String[ASSIGNMENTS.size()];
        int i = 0;
        for (Map.Entry<UUID, String> e : ASSIGNMENTS.entrySet()) {
            lines[i++] = e.getKey() + "=" + e.getValue();
        }
        cfg.get(CATEGORY, ASSIGNMENTS_KEY, lines, "Per-player model assignment, entries formatted uuid=modelId").set(lines);
        if (cfg.hasChanged()) {
            cfg.save();
        }
    }
}
