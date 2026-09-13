package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.vertex.BufferBuilder;

import java.nio.ByteBuffer;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。纯 vanilla BufferBuilder.putBulkData 面（1.20.4/1.20.6 同；
 * 1.21 起 BufferBuilder 重构该方法移除——1.21.1 线接续时按 JNI 映射机制实测收口，见 ADR M0 native 条）。
 */
public final class BufferBuilderBridgeImpl {
    private BufferBuilderBridgeImpl() {
    }

    public static boolean putBulkData(BufferBuilder builder, ByteBuffer buffer) {
        builder.putBulkData(buffer);
        return true;
    }

    public static boolean supportsDirectTransfer() {
        return true;
    }
}
