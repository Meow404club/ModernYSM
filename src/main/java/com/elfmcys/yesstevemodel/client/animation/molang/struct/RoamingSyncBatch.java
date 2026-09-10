package com.elfmcys.yesstevemodel.client.animation.molang.struct;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import java.util.Objects;

public final class RoamingSyncBatch {
    private final int modelHashId;
    private final Int2FloatOpenHashMap changedVariables;

    public RoamingSyncBatch(int modelHashId, Int2FloatOpenHashMap changedVariables) {
        this.modelHashId = modelHashId;
        this.changedVariables = changedVariables;
    }

    public RoamingSyncBatch(int modelHashId, int initialCapacity) {
        this(modelHashId, new Int2FloatOpenHashMap(initialCapacity));
    }

    public int modelHashId() {
        return this.modelHashId;
    }

    public Int2FloatOpenHashMap changedVariables() {
        return this.changedVariables;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RoamingSyncBatch)) {
            return false;
        }
        RoamingSyncBatch other = (RoamingSyncBatch) obj;
        return this.modelHashId == other.modelHashId && Objects.equals(this.changedVariables, other.changedVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modelHashId, this.changedVariables);
    }

    @Override
    public String toString() {
        return "RoamingSyncBatch[modelHashId=" + this.modelHashId + ", changedVariables=" + this.changedVariables + "]";
    }
}
