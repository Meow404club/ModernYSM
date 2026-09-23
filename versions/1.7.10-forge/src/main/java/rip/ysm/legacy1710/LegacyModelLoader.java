package rip.ysm.legacy1710;

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
 * 1.7.10 真实 .ysm 装载链（legacy1710-l2a-model-load）。
 *
 * 1.12.2 LegacyModelLoader（versions/1.12.2-forge legacy122 包）的 1.7.10 twin，
 * 链路对齐 1.20.1 ClientModelManager.loadDefaultModel：内置 builtin 解压到
 * config/yes_steve_model/built/<id> → YSMFolderDeserializer.deserialize →
 * YSMClientMapper.buildParsedBundle → GeoModel → LegacyGeoModelConverter 机械映射 →
 * LegacyModelState（真实烘焙几何+真实贴图）。
 * 1710 净差异：Minecraft 触点 gameDir→mcDataDir（vanilla-mc-1710 Minecraft.java:215
 * public final File mcDataDir 实证，MCP stable_12 定名）；失败回退 LegacyTestModel。
 */
public final class LegacyModelLoader {

    private static final String BUILTIN_PREFIX = "assets/yes_steve_model/builtin/";

    private LegacyModelLoader() {
    }

    /** 装载真实模型；成功 true（state 已喂真实 bundle），失败 false（fallback 生效）。 */
    public static boolean loadDefaultModel() {
        // 配置面：config/openysm-legacy1710.cfg model.id 行切换 builtin 目录；
        // 非 default 的全局默认 id 也入按 id 缓存，主面 setBundle 语义保持
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

    // 装载失败负缓存——渲染帧路径兜底装载失败后不再每帧重跑 extract+deserialize+打印
    //（122 线 run5.log "fallback default" 307 条实证教训）
    private static final java.util.Map<String, Boolean> LOAD_FAILED =
            new java.util.concurrent.ConcurrentHashMap<>();

    // 热重载同位（122 L3-3）：已装载指纹（提取目录内文件 max mtime）——extractBuiltin
    // "ysm.json 在即复用" 幂等缓存对运行期文件变化是盲的，重载面以指纹失效判定是否重解析
    private static final java.util.Map<String, Long> LOADED_FP =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 热重载（触发制；122 reloadLoadedModels 语义同构，主线 /ysm model reload
     * ModelCommand.java:59 命令触发语义适形，不做文件 watcher）。范围=default 主面
     * +全部已装载 id+曾失败 id（负缓存一并清，之前失败的模型重载后重试）。
     * 返回成功重载的模型数。
     */
    public static int reloadLoadedModels() {
        java.util.SortedSet<String> ids = new java.util.TreeSet<>();
        ids.add(LegacyModelRegistry.DEFAULT_MODEL_ID);
        ids.addAll(LegacyModelState.loadedModelIds());
        ids.addAll(LOAD_FAILED.keySet());
        int ok = 0;
        for (String id : ids) {
            if (reload(id)) {
                ok++;
            }
        }
        return ok;
    }

    private static boolean reload(String modelId) {
        // 负缓存清（验收面：重载后之前失败的模型可重试成功）
        LOAD_FAILED.remove(modelId);
        long fp = fingerprintOf(builtDirOf(modelId));
        Long prev = LOADED_FP.get(modelId);
        if (prev != null && prev.longValue() == fp) {
            System.out.printf(
                    "[ysm-legacy1710] reload id=%s: mtime unchanged (fp=%d), skip re-parse%n",
                    modelId, fp);
            return true;
        }
        // 旧纹理先记录（getGlTextureId 首调会申请 GL id，hasGlId 无副作用读取）；
        // 装载失败时旧 state 原样存活（loadInto 失败路径不触碰 state）
        OuterFileTexture oldTex = LegacyModelState.textureOf(modelId);
        Integer oldId = oldTex != null && oldTex.hasGlId()
                ? Integer.valueOf(oldTex.getGlTextureId()) : null;
        boolean main = LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId);
        if (!loadInto(modelId, main)) {
            System.out.println("[ysm-legacy1710] reload id=" + modelId
                    + ": re-parse failed, previous state kept");
            return false;
        }
        LOADED_FP.put(modelId, Long.valueOf(fp));
        OuterFileTexture newTex = LegacyModelState.textureOf(modelId);
        // GL 防泄漏：旧纹理 id 删除（未上传过=无 id，deleteGlTexture 自身 no-op+复位，
        // vanilla-mc-1710 AbstractTexture.java:12-17）；新旧实例必异体（buildParsedBundle
        // 每次 toTexture 新建，122 审查同款论证）
        if (oldId != null) {
            oldTex.deleteGlTexture();
        }
        // 新纹理即时上传（触发点=client tick/键盘路径，与渲染同线程，GL 合法）；
        // 新旧纹理 id 配对进日志=泄漏证据（连续重载 id 有界复用）
        Integer newId = null;
        if (newTex != null) {
            newTex.ensureUploaded();
            newId = Integer.valueOf(newTex.getGlTextureId());
        }
        System.out.printf(
                "[ysm-legacy1710] reload id=%s: mtime %s -> %d invalidated, "
                        + "oldTex=%s, newTex=%s%n",
                modelId,
                prev == null ? "none" : Long.toString(prev.longValue()),
                fp,
                oldId == null ? "none" : "texId " + oldId + " released",
                newTex == null ? "none" : "texId " + newId);
        // 卡预览播放态（hover/fadeout/focus 解析缓存）随旧 bundle 作废
        rip.ysm.legacy1710.yui1710.LegacyPreview1710.onModelReloaded(modelId);
        return true;
    }

    /** 提取目录定位（extractBuiltin 同式：'/' 归一 '_'；1710 唯一 MC 触点=mcDataDir）。 */
    private static Path builtDirOf(String modelId) {
        File gameDir = Minecraft.getMinecraft().mcDataDir;
        return new File(gameDir, "config/yes_steve_model/built/"
                + modelId.replace('/', '_')).toPath();
    }

    // ponytail: 指纹=目录内文件 max mtime（编辑后还原旧 mtime 的边角不覆盖，
    // 显式触发制下漏检代价=再触发一次）；文件数等更复合指纹同理 YAGNI（122 同注）
    private static long fingerprintOf(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return -1L;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.getLastModifiedTime(p).toMillis();
                } catch (IOException e) {
                    return 0L;
                }
            }).max().orElse(-1L);
        } catch (Exception e) {
            System.out.println("[ysm-legacy1710] fingerprint failed for " + dir + ": " + e);
            return -1L;
        }
    }

    /** 按 id 装载任意 builtin 模型入 LegacyModelState 缓存（已装载/已败幂等跳过）。 */
    public static boolean loadModel(String modelId) {
        if (modelId == null || LegacyModelRegistry.DEFAULT_MODEL_ID.equals(modelId)) {
            return false; // default 由 loadDefaultModel 主面装载
        }
        if (LegacyModelState.modelOf(modelId) != null) {
            return true;
        }
        if (LOAD_FAILED.containsKey(modelId)) {
            return false;
        }
        return loadInto(modelId, false);
    }

    /**
     * builtin 装载链支持 ysm-pack.json 打包根（122 线修②同款）：wine_fox 等包是
     * ysm-pack.json 在根、子模组 01_taisho_maid… 各带 ysm.json 的结构——顶层直找
     * ysm.json 必败。解析规则：id 直下有 ysm.json → 原样；id 是打包根 → 取首个含
     * ysm.json 的子目录；也接受显式两级 "wine_fox/01_taisho_maid"。状态一律按请求
     * id 登记。
     */
    private static String resolveLoadablePath(String modelId) {
        String direct = BUILTIN_PREFIX + modelId + "/ysm.json";
        if (resourceExists(direct)) {
            return direct;
        }
        String packRoot = BUILTIN_PREFIX + modelId + "/ysm-pack.json";
        if (!resourceExists(packRoot)) {
            return null;
        }
        String dir = BUILTIN_PREFIX + modelId + "/";
        for (String child : listSubdirs(dir)) {
            if (resourceExists(dir + child + "/ysm.json")) {
                System.out.println("[ysm-legacy1710] pack root resolved: id=" + modelId
                        + " -> submodel=" + child);
                return dir + child + "/ysm.json";
            }
        }
        System.out.println("[ysm-legacy1710] pack root has no submodel ysm.json: " + modelId);
        return null;
    }

    private static boolean resourceExists(String path) {
        return LegacyModelLoader.class.getClassLoader().getResource(path) != null;
    }

    /**
     * 枚举可用 builtin 模型 id（两级形式 "包/子模组"；打包根展开为各含 ysm.json 的
     * 子模组 id）。保留 id "default"（主面隐式）不入枚举防 GUI 双行。
     * classpath 纯读无客户端依赖——L2b 网络卡服务端选择校验共用此面。
     */
    public static java.util.List<String> listBuiltinModels() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String child : listSubdirs(BUILTIN_PREFIX)) {
            if (LegacyModelRegistry.DEFAULT_MODEL_ID.equals(child)) {
                continue;
            }
            if (resourceExists(BUILTIN_PREFIX + child + "/ysm.json")) {
                out.add(child);
            } else if (resourceExists(BUILTIN_PREFIX + child + "/ysm-pack.json")) {
                for (String sub : listSubdirs(BUILTIN_PREFIX + child + "/")) {
                    if (resourceExists(BUILTIN_PREFIX + child + "/" + sub + "/ysm.json")) {
                        out.add(child + "/" + sub);
                    }
                }
            }
        }
        return out;
    }

    /** 顶层包路径列表（builtin 直下带 ysm-pack.json 的目录；String compareTo 序）。 */
    public static java.util.List<String> listPackPaths() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String child : listSubdirs(BUILTIN_PREFIX)) {
            if (resourceExists(BUILTIN_PREFIX + child + "/ysm-pack.json")) {
                out.add(child);
            }
        }
        return out;
    }

    /** 包内子模型 id 列表（两级 "包/子模组"，带 ysm.json；compareTo 序）。 */
    public static java.util.List<String> listPackModels(String packPath) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (packPath == null || !resourceExists(BUILTIN_PREFIX + packPath + "/ysm-pack.json")) {
            return out;
        }
        for (String sub : listSubdirs(BUILTIN_PREFIX + packPath + "/")) {
            if (resourceExists(BUILTIN_PREFIX + packPath + "/" + sub + "/ysm.json")) {
                out.add(packPath + "/" + sub);
            }
        }
        return out;
    }

    /** ysm-pack.json 元数据（纯 Gson 解析，无客户端依赖；null=无包或解析失败）。 */
    public static PackMeta packMeta(String packPath) {
        if (packPath == null) {
            return null;
        }
        try (InputStream in = LegacyModelLoader.class.getClassLoader()
                .getResourceAsStream(BUILTIN_PREFIX + packPath + "/ysm-pack.json")) {
            if (in == null) {
                return null;
            }
            com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            String name = obj.has("name") ? obj.get("name").getAsString() : packPath;
            String desc = obj.has("description") ? obj.get("description").getAsString() : "";
            java.util.Map<String, String[]> lang = new java.util.LinkedHashMap<String, String[]>();
            if (obj.has("lang") && obj.get("lang").isJsonObject()) {
                com.google.gson.JsonObject langObj = obj.getAsJsonObject("lang");
                for (Map.Entry<String, com.google.gson.JsonElement> e : langObj.entrySet()) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    com.google.gson.JsonObject entry = e.getValue().getAsJsonObject();
                    lang.put(e.getKey(), new String[] {
                            entry.has("name") ? entry.get("name").getAsString() : null,
                            entry.has("description") ? entry.get("description").getAsString() : null});
                }
            }
            return new PackMeta(name, desc, lang);
        } catch (Exception e) {
            System.out.println("[ysm-legacy1710] pack meta parse failed for " + packPath + ": " + e);
            return null;
        }
    }

    /** ysm-pack.json 内容（ModelPackData 主线同构的最小面）。 */
    public static final class PackMeta {
        public final String name;
        public final String description;
        /** locale → {name, description}（可空项）。 */
        public final Map<String, String[]> lang;

        PackMeta(String name, String description, Map<String, String[]> lang) {
            this.name = name;
            this.description = description;
            this.lang = lang;
        }

        /** 本地化名（lang[locale].name → name 回退，主线 getLocalizedString 同语义）。 */
        public String localizedName(String locale) {
            String[] l = locale == null ? null : this.lang.get(locale);
            return l != null && l[0] != null ? l[0] : this.name;
        }
    }

    /** 枚举 classpath 目录子项（dev=文件系统 / 生产=jar 条目，两态覆盖）。 */
    private static java.util.SortedSet<String> listSubdirs(String dirPath) {
        java.util.SortedSet<String> out = new java.util.TreeSet<>();
        try {
            URL url = LegacyModelLoader.class.getClassLoader().getResource(dirPath);
            if (url == null) {
                return out;
            }
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                JarURLConnection conn = (JarURLConnection) uri.toURL().openConnection();
                conn.setUseCaches(false);
                // getEntryName 对目录 URL 保留尾斜杠（122 线 jshell 实证），直接拼 "/"
                // 会得到 "...//" 零匹配——去尾斜杠再拼（122 线 70d92a6 同款修正）
                String entry = conn.getEntryName();
                if (entry.endsWith("/")) {
                    entry = entry.substring(0, entry.length() - 1);
                }
                String prefix = entry + "/";
                try (ZipFile zip = new ZipFile(conn.getJarFileURL().getFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(prefix) && name.length() > prefix.length()) {
                            String rest = name.substring(prefix.length());
                            out.add(rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest);
                        }
                    }
                }
            } else {
                try (java.util.stream.Stream<Path> list = Files.list(Paths.get(uri))) {
                    list.filter(Files::isDirectory).forEach(p -> out.add(p.getFileName().toString()));
                }
            }
        } catch (Exception e) {
            System.out.println("[ysm-legacy1710] listSubdirs failed for " + dirPath + ": " + e);
        }
        return out;
    }

    /** 装载 builtin <id>；main=同时喂主面（loadDefaultModel 路径，日志/回退语义）。 */
    private static boolean loadInto(String modelId, boolean main) {
        String ysmPath = resolveLoadablePath(modelId);
        if (ysmPath == null) {
            return failOnce(modelId, "builtin model not resolvable: " + BUILTIN_PREFIX + modelId);
        }
        // 提取目录按请求 id 落盘（'/' 归一 '_'），装载成功即按请求 id 登记
        Path builtDir = extractBuiltin(modelId, ysmPath.substring(0, ysmPath.length() - "/ysm.json".length()));
        if (builtDir == null) {
            return failOnce(modelId, "builtin extract failed: " + modelId);
        }
        try (YSMFolderDeserializer deserializer = new YSMFolderDeserializer(builtDir)) {
            RawYsmModel raw = deserializer.deserialize();
            ClientModelInfo bundle = YSMClientMapper.buildParsedBundle(raw, modelId);
            MainModelData data = bundle.getMainModelData();
            if (data == null || data.getModels().isEmpty()) {
                System.out.println("[ysm-legacy1710] parsed bundle has no main model for id=" + modelId
                        + ", fallback to test model");
                return false;
            }
            GeoModel geoModel = data.getModels().get(0);
            LegacyBakedModel mainModel = LegacyGeoModelConverter.convert(geoModel);
            OuterFileTexture texture = resolveMainTexture(data);
            if (main) {
                LegacyModelState.setBundle(bundle, mainModel, texture);
            } else {
                LAST_BUNDLE.put(modelId, bundle);
                LegacyModelState.registerModel(modelId, bundle, mainModel, texture);
            }
            LOAD_FAILED.remove(modelId);
            LOADED_FP.put(modelId, Long.valueOf(fingerprintOf(builtDir)));
            System.out.printf(
                    "[ysm-legacy1710] real model loaded: id=%s bones=%d quads=%d textures=%d tex=%s%n",
                    modelId,
                    mainModel.bones.size(),
                    quadCount(mainModel),
                    data.getTextureMap() == null ? 0 : data.getTextureMap().size(),
                    texture == null ? "none" : "bound");
            return true;
        } catch (Exception e) {
            return failOnce(modelId, "real model load failed: " + e);
        }
    }

    /** 采证面：全模型 quad 总数（翻译层消费面计数）。 */
    private static int quadCount(LegacyBakedModel model) {
        int n = 0;
        for (LegacyBakedModel.BakedBone bone : model.bones) {
            for (LegacyBakedModel.BakedCube cube : bone.cubes) {
                n += cube.quads.size();
            }
        }
        return n;
    }

    /** 失败入负缓存（成功 remove），返回 false；日志每次失败只打一次。 */
    private static boolean failOnce(String modelId, String why) {
        Boolean first = LOAD_FAILED.putIfAbsent(modelId, Boolean.TRUE);
        if (first == null) {
            System.out.println("[ysm-legacy1710] model load failed for id=" + modelId
                    + ", fallback (negative-cached): " + why);
        }
        return false;
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

    /**
     * builtin 模型资源解压到 config/yes_steve_model/built/<id>（幂等：ysm.json 在即复用）。
     * resourcePath 为解析后的模型目录（…/ysm.json 去尾）。
     */
    private static Path extractBuiltin(String modelId, String resourcePath) {
        try {
            // 1710 唯一 MC 触点：mcDataDir（MCP stable_12 定名；1.12.2 是 gameDir）
            Path built = builtDirOf(modelId);
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
            URL url = cl.getResource(resourcePath);
            if (url == null) {
                System.out.println("[ysm-legacy1710] builtin model not on classpath: " + resourcePath);
                return null;
            }
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                extractFromJar((JarURLConnection) uri.toURL().openConnection(), built);
            } else {
                copyDir(Paths.get(uri), built);
            }
            System.out.println("[ysm-legacy1710] builtin default extracted to " + built);
            return built;
        } catch (Exception e) {
            System.out.println("[ysm-legacy1710] builtin extract failed: " + e);
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
