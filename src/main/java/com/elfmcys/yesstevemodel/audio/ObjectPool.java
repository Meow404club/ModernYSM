package com.elfmcys.yesstevemodel.audio;

import com.elfmcys.yesstevemodel.client.event.ClientTickEvent;

import java.util.LinkedList;
import java.util.Objects;

public class ObjectPool {

    private static final LinkedList<PoolEntry<NativeAudioDecoder>> pool = new LinkedList<>();

    private static volatile int activeCount = 0;

    public static NativeAudioDecoder acquire() {
        synchronized (pool) {
            if (!pool.isEmpty()) {
                NativeAudioDecoder decoder = pool.removeLast().value;
                if (pool.isEmpty()) {
                    activeCount = 0;
                }
                return decoder;
            }
            return new NativeAudioDecoder();
        }
    }

    public static void release(NativeAudioDecoder decoder) {
        decoder.reset();
        synchronized (pool) {
            PoolEntry<NativeAudioDecoder> poolEntry = new PoolEntry<>(decoder, ClientTickEvent.getTickCount() + 200);
            if (pool.isEmpty()) {
                activeCount = poolEntry.expirationTick;
            }
            pool.addLast(poolEntry);
        }
    }

    public static void cleanup() {
        int i;
        if (activeCount != 0 && (i = ClientTickEvent.getTickCount()) > activeCount) {
            synchronized (pool) {
                while (!pool.isEmpty()) {
                    PoolEntry<NativeAudioDecoder> first = pool.getFirst();
                    if (first.expirationTick <= i) {
                        first.value.destroy();
                        pool.removeFirst();
                    } else {
                        activeCount = first.expirationTick;
                        return;
                    }
                }
                activeCount = 0;
            }
        }
    }

    private static final class PoolEntry<T> {
        final T value;
        final int expirationTick;

        PoolEntry(T value, int expirationTick) {
            this.value = value;
            this.expirationTick = expirationTick;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PoolEntry)) {
                return false;
            }
            PoolEntry<?> other = (PoolEntry<?>) obj;
            return Objects.equals(this.value, other.value) && this.expirationTick == other.expirationTick;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.expirationTick);
        }

        @Override
        public String toString() {
            return "PoolEntry[value=" + this.value + ", expirationTick=" + this.expirationTick + "]";
        }
    }
}
