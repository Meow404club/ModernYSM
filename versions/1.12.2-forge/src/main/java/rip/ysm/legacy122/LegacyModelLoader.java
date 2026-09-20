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

    private static final String BUILTIN_PATH = "assets/yes_steve_model/builtin/default";

    private LegacyModelLoader() {
    }

    /** 装载真实模型；成功 true（state 已喂真实 bundle），失败 false（fallback 生效）。 */
    public static boolean loadDefaultModel() {
        Path builtDir = extractBuiltinDefault();
        if (builtDir == null) {
            return false;
        }
        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(builtDir)) {
            RawYsmModel raw = deserializer.deserialize();
            ClientModelInfo bundle = YSMClientMapper.buildParsedBundle(raw, "default");
            MainModelData data = bundle.getMainModelData();
            if (data == null || data.getModels().isEmpty()) {
                System.out.println("[ysm-legacy122] parsed bundle has no main model, fallback to test model");
                return false;
            }
            GeoModel mainModel = data.getModels().get(0);
            OuterFileTexture texture = resolveMainTexture(data);
            LegacyModelState.setBundle(bundle, mainModel, texture);
            System.out.printf(
                    "[ysm-legacy122] real model loaded: id=default bones=%d textures=%d tex=%s%n",
                    mainModel.bakedBones == null ? -1 : mainModel.bakedBones.size(),
                    data.getTextureMap() == null ? 0 : data.getTextureMap().size(),
                    texture == null ? "none" : "bound");
            return true;
        } catch (Exception e) {
            System.out.println("[ysm-legacy122] real model load failed, fallback to test model: " + e);
            e.printStackTrace();
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

    /** 内置 default 解压到 config/yes_steve_model/built/default（幂等：ysm.json 在即复用）。 */
    private static Path extractBuiltinDefault() {
        try {
            File gameDir = Minecraft.getMinecraft().gameDir;
            Path built = new File(gameDir, "config/yes_steve_model/built/default").toPath();
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
            URL url = cl.getResource(BUILTIN_PATH);
            if (url == null) {
                System.out.println("[ysm-legacy122] builtin default not on classpath: " + BUILTIN_PATH);
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
