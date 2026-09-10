package rip.ysm.api.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.ByteBuffer;
import rip.ysm.api.client.platform.forge.BufferBuilderBridgeImpl;

public final class BufferBuilderBridge {

    private BufferBuilderBridge() {
    }

    @ExpectPlatform
    public static boolean putBulkData(BufferBuilder builder, ByteBuffer buffer) {
        return BufferBuilderBridgeImpl.putBulkData(builder, buffer);
    }

    @ExpectPlatform
    public static boolean supportsDirectTransfer() {
        return BufferBuilderBridgeImpl.supportsDirectTransfer();
    }
}
