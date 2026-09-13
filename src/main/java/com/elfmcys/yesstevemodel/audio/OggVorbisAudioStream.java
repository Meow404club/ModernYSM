package com.elfmcys.yesstevemodel.audio;

// 1.20.5+ OggAudioStream(blaze3d) 删除 → JOrbisAudioStream（FloatSampleSource，read(int) 默认方法
// ChunkedSampleByteBuf 同款 float→PCM16 clamp 公式，构造器/getFormat/read/close 同形）。
// 注释态包裹（说明文字置块外：活跃分支内裸 // 会被剥前缀变代码）
//? if <1.20.5 {
import com.mojang.blaze3d.audio.OggAudioStream;
//?} else {
/*import net.minecraft.client.sounds.JOrbisAudioStream;*/
//?}
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OggVorbisAudioStream implements IAudioStreamSupport {

    private static final ByteBuffer EMPTY_BUFFER = BufferUtils.createByteBuffer(0);

//? if <1.20.5
    private final OggAudioStream oggStream;
//? if >=1.20.5
/*    private final JOrbisAudioStream oggStream;*/

    private final AudioFormat audioFormat;

    @Nullable
    private final AudioCacheBuilder cacheBuilder;

    private volatile boolean isClosed;

    private boolean isEndOfStream;

    public OggVorbisAudioStream(ByteBuffer byteBuffer, @Nullable AudioCacheBuilder cacheBuilder) throws UnsupportedAudioFileException, IOException {
//? if <1.20.5
        this.oggStream = new OggAudioStream(new ByteBufInputStream(Unpooled.wrappedBuffer(byteBuffer)));
//? if >=1.20.5
/*        this.oggStream = new JOrbisAudioStream(new ByteBufInputStream(Unpooled.wrappedBuffer(byteBuffer)));*/
        if (this.oggStream.getFormat().getChannels() != 1 && this.oggStream.getFormat().getChannels() != 2) {
            throw new UnsupportedAudioFileException();
        }
        this.audioFormat = new AudioFormat(this.oggStream.getFormat().getSampleRate(), 16, 1, true, false);
        this.cacheBuilder = cacheBuilder;
    }

    @NotNull
    public AudioFormat getFormat() {
        return this.audioFormat;
    }

    @NotNull
    public ByteBuffer read(int i) throws IOException {
        ByteBuffer byteBufferCreateByteBuffer;
        if (this.isEndOfStream || this.isClosed) {
            return EMPTY_BUFFER;
        }
        ByteBuffer byteBufferSlice = this.oggStream.read(this.oggStream.getFormat().getChannels() * i);
        if (!byteBufferSlice.hasRemaining()) {
            if (this.cacheBuilder != null) {
                this.cacheBuilder.flushToCache();
            }
            this.isEndOfStream = true;
            return byteBufferSlice;
        }
        if (this.oggStream.getFormat().getChannels() == 2) {
            ByteBuffer byteBufferOrder = byteBufferSlice.duplicate().order(ByteOrder.nativeOrder());
            if (!byteBufferSlice.isReadOnly()) {
                byteBufferCreateByteBuffer = byteBufferSlice.duplicate().order(ByteOrder.nativeOrder());
                byteBufferCreateByteBuffer.limit(byteBufferOrder.remaining() / 2);
            } else {
                byteBufferCreateByteBuffer = BufferUtils.createByteBuffer(byteBufferOrder.remaining() / 2);
            }
            byteBufferSlice = byteBufferCreateByteBuffer.slice();
            do {
                byteBufferCreateByteBuffer.putShort((short) Math.round((byteBufferOrder.getShort() + byteBufferOrder.getShort()) / 2.0f));
            } while (byteBufferOrder.hasRemaining());
        }
        if (this.cacheBuilder != null) {
            this.cacheBuilder.appendAudio(byteBufferSlice.duplicate());
        }
        return byteBufferSlice;
    }

    public void close() throws IOException {
        if (!this.isClosed) {
            this.oggStream.close();
            this.isClosed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }
}