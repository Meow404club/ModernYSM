package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

public final class FeedbackData {
    private final int entityId;
    private final Object2FloatArrayMap<String> stringValues;
    private final Int2FloatArrayMap intValues;
    private final int flags;

    public FeedbackData(int entityId, Object2FloatArrayMap<String> stringValues,
                        Int2FloatArrayMap intValues, int flags) {
        this.entityId = entityId;
        this.stringValues = stringValues;
        this.intValues = intValues;
        this.flags = flags;
    }

    public int entityId() {
        return this.entityId;
    }

    public Object2FloatArrayMap<String> stringValues() {
        return this.stringValues;
    }

    public Int2FloatArrayMap intValues() {
        return this.intValues;
    }

    public int flags() {
        return this.flags;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeedbackData)) {
            return false;
        }
        FeedbackData other = (FeedbackData) obj;
        return this.entityId == other.entityId && this.flags == other.flags
                && Objects.equals(this.stringValues, other.stringValues)
                && Objects.equals(this.intValues, other.intValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityId, this.stringValues, this.intValues, this.flags);
    }

    @Override
    public String toString() {
        return "FeedbackData[entityId=" + this.entityId + ", stringValues=" + this.stringValues
                + ", intValues=" + this.intValues + ", flags=" + this.flags + "]";
    }

    public static void writeToBuf(FeedbackData message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeVarInt(message.flags);
        buf.writeByte(message.stringValues.size());
        message.stringValues.object2FloatEntrySet().fastForEach(entry -> {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getFloatValue());
        });
    }

    public static FeedbackData readFromBuf(FriendlyByteBuf buf, boolean useInternedKeys) {
        Object2FloatArrayMap object2FloatArrayMap;
        Int2FloatArrayMap int2FloatArrayMap;
        int entityId = buf.readInt();
        int varInt = buf.readVarInt();
        int entryCount = buf.readByte();
        if (useInternedKeys) {
            int[] iArr = new int[entryCount];
            float[] fArr = new float[entryCount];
            for (int i = 0; i < entryCount; i++) {
                iArr[i] = StringPool.computeIfAbsent(buf.readUtf());
                fArr[i] = buf.readFloat();
            }
            int2FloatArrayMap = new Int2FloatArrayMap(iArr, fArr);
            object2FloatArrayMap = null;
        } else {
            String[] strArr = new String[entryCount];
            float[] fArr2 = new float[entryCount];
            for (int i = 0; i < entryCount; i++) {
                strArr[i] = buf.readUtf();
                fArr2[i] = buf.readFloat();
            }
            object2FloatArrayMap = new Object2FloatArrayMap<>(strArr, fArr2);
            int2FloatArrayMap = null;
        }
        return new FeedbackData(entityId, object2FloatArrayMap, int2FloatArrayMap, varInt);
    }
}
