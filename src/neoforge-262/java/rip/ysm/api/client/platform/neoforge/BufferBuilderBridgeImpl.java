package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.21.1 代）。1.20.6 版在 src/neoforge-1205/java。
 * 1.21 BufferBuilder.putBulkData(ByteBuffer) 删除（vanilla-1.21.1 BufferBuilder 无该方法，
 * putBulkData 仅余 BakedQuad 变体）→ 原生直传路径在 1.21.1 声明不支持，回落逐顶点路径；
 * supportsDirectTransfer=false 与 forge 原类的原生探测契约一致（JNI 侧运行时读取）。
 */
public final class BufferBuilderBridgeImpl {
    private BufferBuilderBridgeImpl() {
    }

    public static boolean putBulkData(BufferBuilder builder, ByteBuffer buffer) {
        // 1.21.1 无原生直传面：返回 false 交由调用方回落（本方法当前无共享源调用点，
        // 仅 JNI 原生库反射消费，保留签名防 NoSuchMethodError）
        return false;
    }

    public static boolean supportsDirectTransfer() {
        return false;
    }
}
