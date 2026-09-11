package com.elfmcys.yesstevemodel.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field declaredField = Unsafe.class.getDeclaredField("theUnsafe");
            declaredField.setAccessible(true);
            UNSAFE = (Unsafe) declaredField.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Couldn't obtain reference to sun.misc.Unsafe", e);
        }
    }

    /** storeFence/loadFence 门面：主源集（--release 8，ct.sym 盲区）不得解析 Unsafe 类型。 */
    public static void storeFence() {
        UNSAFE.storeFence();
    }

    public static void loadFence() {
        UNSAFE.loadFence();
    }

    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
