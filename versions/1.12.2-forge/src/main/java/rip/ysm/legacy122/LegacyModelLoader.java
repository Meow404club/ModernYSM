package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.client.model.MainModelData;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.resource.YSMClientMapper;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 1.12.2 真实 .ysm 装载链（legacy-1222-l2-full）。
 *
 * 链路对齐 1.20.1 ClientModelManager.loadDefaultModel（ClientModelManager.java:190-240）：
 * 内置 builtin/default 解压到 config/yes_steve_model/built/default（生产 modjar 协议
 * 无法 zipfs 直读，1.20.1 ServerModelManager 同款规避）→ YSMFolderDeserializer.deserialize
 * → YSMClientMapper.buildParsedBundle → LegacyModelState（真实 GeoModel+真实贴图）。
 * 失败回退 LegacyTestModel 程序化人形（L1 fallback 保留）。
 */
public final class LegacyModelLoader {

    private static final String BUILTIN_PREFIX = "assets/yes_steve_model/builtin/";

    private LegacyModelLoader() {
    }

    /** 装载真实模型；成功 true（state 已喂真实 bundle），失败 false（fallback 生效）。 */
    public static boolean loadDefaultModel() {
        // 批④配置面：config/openysm-legacy122.cfg model.id 行切换 builtin 目录。
        // L3-1：非 default 的全局默认 id 也入按 id 缓存；主面 setBundle 语义保持
        String modelId = rip.ysm.LegacyConfig.modelId();
        if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)) {
            return loadInto(modelId, true);
        }
        if (!loadModel(modelId)) {
            return false;
        }
        LegacyModelState.setBundle(LAST_BUNDLE.get(modelId),
                LegacyModelState.modelOf(modelId), LegacyModelState.textureOf(modelId));
        return true;
    }

    // ponytail: loadModel→loadDefaultModel 回传 bundle 用进程内 map（ClientModelInfo
    // 无轻量回读面；双实体量级下单条目 map 足够）
    private static final java.util.Map<String, ClientModelInfo> LAST_BUNDLE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** L3-1：按 id 装载任意 builtin 模型入 LegacyModelState 缓存（已装载幂等跳过）。 */
    public static boolean loadModel(String modelId) {
        if (modelId == null || LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)) {
            return false; // default 由 loadDefaultModel 主面装载
        }
        if (LegacyModelState.modelOf(modelId) != null) {
            return true;
        }
        return loadInto(modelId, false);
    }

    /** 装载 builtin <id>；main=同时喂主面（loadDefaultModel 路径，日志/回退 L2 语义）。 */
    private static boolean loadInto(String modelId, boolean main) {
        String builtinPath = BUILTIN_PREFIX + modelId;
        Path builtDir = extractBuiltinDefault(modelId, builtinPath);
        if (builtDir == null) {
            return false;
        }
        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(builtDir)) {
            RawYsmModel raw = deserializer.deserialize();
            ClientModelInfo bundle = YSMClientMapper.buildParsedBundle(raw, modelId);
            MainModelData data = bundle.getMainModelData();
            if (data == null || data.getModels().isEmpty()) {
                System.out.println("[ysm-legacy122] parsed bundle has no main model for id=" + modelId
                        + ", fallback to test model");
                return false;
            }
            GeoModel mainModel = data.getModels().get(0);
            OuterFileTexture texture = resolveMainTexture(data);
            if (main) {
                LegacyModelState.setBundle(bundle, mainModel, texture);
            } else {
                LAST_BUNDLE.put(modelId, bundle);
                LegacyModelState.registerModel(modelId, bundle, mainModel, texture);
            }
            System.out.printf(
                    "[ysm-legacy122] real model loaded: id=%s bones=%d textures=%d tex=%s%n",
                    modelId,
                    mainModel.bakedBones == null ? -1 : mainModel.bakedBones.size(),
                    data.getTextureMap() == null ? 0 : data.getTextureMap().size(),
                    texture == null ? "none" : "bound");
            return true;
        } catch (Exception e) {
            System.out.println("[ysm-legacy122] real model load failed for id=" + modelId
                    + ", fallback to test model: " + e);
            return false;
        }
    }

    /**
     * 主贴图挑选：textureMap 首个条目（RawYsmModel.textures LinkedHashMap 序，
     * buildParsedBundle→buildTextureMap 保序——首个即主实体贴图）。
     */
    private static OuterFileTexture resolveMainTexture(MainModelData data) {
        Map<String, OuterFileTexture> map = data.getTextureMap();
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map.values().iterator().next();
    }

    /** builtin 模型解压到 config/yes_steve_model/built/<id>（幂等：ysm.json 在即复用）。 */
    private static Path extractBuiltinDefault(String modelId, String builtinPath) {
        try {
            File gameDir = Minecraft.getMinecraft().gameDir;
            Path built = new File(gameDir, "config/yes_steve_model/built/" + modelId).toPath();
            if (Files.isDirectory(built) && Files.exists(built.resolve("ysm.json"))) {
                return built;
            }
            if (Files.exists(built)) {
                try (java.util.stream.Stream<Path> walk = Files.walk(built)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
            Files.createDirectories(built);
            // classpath 物理定位（dev=文件系统目录 / 生产=jar 条目，两态覆盖）
            ClassLoader cl = LegacyModelLoader.class.getClassLoader();
            URL url = cl.getResource(builtinPath);
            if (url == null) {
                System.out.println("[ysm-legacy122] builtin model not on classpath: " + builtinPath);
                return null;
            }
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                extractFromJar((JarURLConnection) uri.toURL().openConnection(), built);
            } else {
                copyDir(Paths.get(uri), built);
            }
            System.out.println("[ysm-legacy122] builtin default extracted to " + built);
            return built;
        } catch (Exception e) {
            System.out.println("[ysm-legacy122] builtin extract failed: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private static void extractFromJar(JarURLConnection conn, Path target) throws IOException {
        conn.setUseCaches(false);
        try (ZipFile zip = new ZipFile(conn.getJarFileURL().getFile())) {
            String prefix = conn.getEntryName() + "/";
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || entry.isDirectory()) {
                    continue;
                }
                Path out = target.resolve(name.substring(prefix.length()));
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(out.toFile())) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                }
            }
        }
    }

    private static void copyDir(Path src, Path target) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(src)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                Path out = target.resolve(src.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(p, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
