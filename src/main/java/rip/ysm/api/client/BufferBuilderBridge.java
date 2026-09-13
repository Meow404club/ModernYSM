package rip.ysm.api.client;

import com.mojang.blaze3d.vertex.BufferBuilder;

import java.nio.ByteBuffer;
//? if neoforge
/*import rip.ysm.api.client.platform.neoforge.BufferBuilderBridgeImpl;*/
//? if forge
import rip.ysm.api.client.platform.forge.BufferBuilderBridgeImpl;

public final class BufferBuilderBridge {

    private BufferBuilderBridge() {
    }

    public static boolean putBulkData(BufferBuilder builder, ByteBuffer buffer) {
        return BufferBuilderBridgeImpl.putBulkData(builder, buffer);
    }

    public static boolean supportsDirectTransfer() {
        return BufferBuilderBridgeImpl.supportsDirectTransfer();
    }
}
