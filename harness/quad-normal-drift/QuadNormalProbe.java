import com.elfmcys.yesstevemodel.resource.YSMBinaryDeserializer;
import com.elfmcys.yesstevemodel.resource.YSMBinarySerializer;
import com.elfmcys.yesstevemodel.resource.YSMClientMapper.TranslucencyScanner;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel.RawCube;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel.RawFace;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel.RawBone;
import com.elfmcys.yesstevemodel.resource.pojo.RawYsmModel.RawTexture;
import rip.ysm.security.YSMByteBuf;

import javax.imageio.ImageIO;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * quad-normal-drift 离线数值探针。
 *
 * 跑真实烘焙链（生产同类）：
 *   文件夹 json --YSMFolderDeserializer--> RawYsmModel --YSMBinarySerializer(32,true)-->
 *   bytes --YSMBinaryDeserializer(32)--> RawYsmModel（=ServerModelManager.java:1268 的产线参数）
 * 然后按 YSMClientMapper.buildMesh:441-502 的遍历序+TranslucencyScanner 过滤收集 quad，
 * 按 RealCameraApiBinder.findQuadCandidates:564-592 的量化+Polygon 谓词做三槽 UV 命中，
 * 打印命中面法线（quadDirection:635-636 读的就是该值）。
 *
 * 用途：①烘焙层跨 JVM/跨路径（folder vs binary roundtrip）逐位对账；
 *      ②同一 bind UV 配置对不同模型（不同 geo/贴图分辨率）的命中面法线对账——资源层定界。
 */
public final class QuadNormalProbe {
    static final int UV_RESOLUTION = 1000000; // RealCameraApiBinder.java:200

    /** dev 环境 + 用户真机 realcamera.json 的 trissy target（两处同值）。 */
    static final float[][] TRISSY_UV = {
            {0.21289062f, 0.27148438f},   // pos
            {0.21289062f, 0.27148438f},   // fwd
            {0.013671875f, 0.27539062f},  // up
    };
    /** 用户真机 target 3/4（bindRotation=true）的 UV。 */
    static final float[][] VANILLA_UV = {
            {0.1875f, 0.2f},
            {0.1875f, 0.2f},
            {0.1875f, 0.075f},
    };

    static final class Quad {
        String boneName;
        int cubeIdx, faceIdx;
        float[] u = new float[4], v = new float[4], normal = new float[3];
        float[][] positions = new float[4][3];
    }

    public static void main(String[] args) throws Exception {
        System.out.println("env java=" + System.getProperty("java.version")
                + " vm=" + System.getProperty("java.vm.version")
                + " vendor=" + System.getProperty("java.vendor")
                + " arch=" + System.getProperty("os.arch")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        for (String dir : args) {
            probe(Path.of(dir));
        }
    }

    static void probe(Path dir) throws Exception {
        System.out.println("==== model " + dir);
        // 烘焙第 1 遍（folder）
        RawYsmModel rawF1;
        try (YSMFolderDeserializer d = new YSMFolderDeserializer(dir)) {
            rawF1 = d.deserialize();
        }
        // 产线二进制 roundtrip（ServerModelManager.java:1268 同参）
        YSMByteBuf buf = YSMBinarySerializer.serialize(rawF1, 32, true);
        byte[] bytes = buf.toArray();
        buf.close();
        RawYsmModel rawB1;
        try (YSMBinaryDeserializer d = new YSMBinaryDeserializer(bytes, 32)) {
            rawB1 = d.deserialize();
        }
        // 烘焙第 2 遍（folder，进程内重跑）
        RawYsmModel rawF2;
        try (YSMFolderDeserializer d = new YSMFolderDeserializer(dir)) {
            rawF2 = d.deserialize();
        }

        collectAndQuery(rawF1, "folder#1");
        collectAndQuery(rawB1, "binary#1");
        collectAndQuery(rawF2, "folder#2");
    }

    /** mapper 序收集 + sha256 + 三槽命中查询（真实 TranslucencyScanner + 真实量化谓词语义）。 */
    static void collectAndQuery(RawYsmModel raw, String tag) throws Exception {
        // 贴图解码序=LinkedHashMap 迭代序（YSMClientMapper.buildParsedBundle:370-399）
        List<BufferedImage> images = new ArrayList<>();
        for (RawTexture rt : raw.mainEntity.textures.values()) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(rt.data));
            images.add(img);
        }
        TranslucencyScanner scanner = new TranslucencyScanner(images.toArray(new BufferedImage[0]),
                Math.max(1, raw.mainEntity.textures.size()));

        List<Quad> quads = new ArrayList<>();
        int dropped = 0;
        List<RawBone> bones = raw.mainEntity.mainModel.bones;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (int bi = 0; bi < bones.size(); bi++) {
            RawBone bone = bones.get(bi);
            for (int ci = 0; ci < bone.cubes.size(); ci++) {
                RawCube cube = bone.cubes.get(ci);
                for (int fi = 0; fi < cube.faces.size(); fi++) {
                    RawFace rf = cube.faces.get(fi);
                    int faceState = scanner.scan(rf);
                    if ((faceState & TranslucencyScanner.FLAG_VISIBLE) == 0) {
                        dropped++;
                        continue;
                    }
                    Quad q = new Quad();
                    q.boneName = bone.name;
                    q.cubeIdx = ci;
                    q.faceIdx = fi;
                    System.arraycopy(rf.normal, 0, q.normal, 0, 3);
                    for (int i = 0; i < 4; i++) {
                        q.u[i] = rf.u[i];
                        q.v[i] = rf.v[i];
                        q.positions[i] = rf.positions[i].clone();
                    }
                    quads.add(q);
                    md.update((bi + ":" + ci + ":" + fi + ";").getBytes(StandardCharsets.UTF_8));
                    for (float f : q.normal) md.update(f32Bytes(f));
                    for (float[] p : q.positions) for (float f : p) md.update(f32Bytes(f));
                    for (float f : q.u) md.update(f32Bytes(f));
                    for (float f : q.v) md.update(f32Bytes(f));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        System.out.println("[" + tag + "] bones=" + bones.size() + " quads=" + quads.size()
                + " droppedInvisible=" + dropped + " sha256(quads)=" + sb);

        query(quads, "trissyUV", TRISSY_UV);
        query(quads, "vanillaUV", VANILLA_UV);
    }

    /** RealCameraApiBinder.findQuadCandidates:564-592 的量化+Polygon 谓词（同常量同序）。 */
    static void query(List<Quad> quads, String label, float[][] slots) {
        String[] slotNames = {"pos", "fwd", "up"};
        for (int s = 0; s < 3; s++) {
            float u = slots[s][0], v = slots[s][1];
            int px = (int) (UV_RESOLUTION * u);
            int pv = (int) (UV_RESOLUTION * v);
            List<Quad> hits = new ArrayList<>();
            for (Quad q : quads) {
                int[] us = new int[4], vs = new int[4];
                for (int i = 0; i < 4; i++) {
                    us[i] = (int) (UV_RESOLUTION * q.u[i]);
                    vs[i] = (int) (UV_RESOLUTION * q.v[i]);
                }
                if (new Polygon(us, vs, 4).contains(px, pv)) {
                    hits.add(q);
                }
            }
            StringBuilder line = new StringBuilder("  [" + label + "/" + slotNames[s] + "] u=" + u + " v=" + v
                    + " hits=" + hits.size());
            for (int h = 0; h < Math.min(hits.size(), 3); h++) {
                Quad q = hits.get(h);
                line.append(" #").append(h).append("=").append(q.boneName)
                        .append(":").append(q.cubeIdx).append(":").append(q.faceIdx)
                        .append(" n=(").append(f(q.normal[0])).append(',').append(f(q.normal[1])).append(',')
                        .append(f(q.normal[2])).append(") hex=(")
                        .append(Float.toHexString(q.normal[0])).append(',')
                        .append(Float.toHexString(q.normal[1])).append(',')
                        .append(Float.toHexString(q.normal[2])).append(')');
            }
            System.out.println(line);
        }
    }

    static String f(float v) {
        return String.format("%+.6f", v);
    }

    static byte[] f32Bytes(float f) {
        int bits = Float.floatToIntBits(f);
        return new byte[]{(byte) bits, (byte) (bits >> 8), (byte) (bits >> 16), (byte) (bits >> 24)};
    }
}
