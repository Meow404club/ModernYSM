package com.elfmcys.yesstevemodel.util;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Java 8 兼容改写：原实现基于 java.lang.invoke.VarHandle（Java 9 API，
 * MethodHandles.privateLookupIn + findVarHandle），Java 8 无法编译也无法加载。
 * 语义对齐：字段不存在或类型不符返回 empty（对齐 findVarHandle 的
 * NoSuchFieldException 路径），可访问性获取失败打印栈后返回 empty。
 */
public class VarHandleHelper {
    public static Optional<Field> findField(Class<?> cls, String str, Class<?> cls2) {
        try {
            Field field = cls.getDeclaredField(str);
            if (field.getType() != cls2) {
                return Optional.empty();
            }
            field.setAccessible(true);
            return Optional.of(field);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
