package rip.ysm.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 8 兼容的不可变集合工厂（替代 Java 9+ 的 List.of/Map.of/Set.of）。
 * <p>
 * 语义对齐：返回值不可变（修改操作抛 {@link UnsupportedOperationException}）、
 * 允许查询 contains 是否包含；Set 版本用 LinkedHashSet 保序（List.of/Set.of 的
 * 迭代顺序本就未约定，插入序是合法实现之一）。空集合直接走
 * {@link Collections#emptyList()} 等共享单例，零分配。
 */
public final class YsmCollections {
    private YsmCollections() {
    }

    @SafeVarargs
    public static <T> List<T> immutableListOf(T... items) {
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    @SafeVarargs
    public static <T> Set<T> immutableSetOf(T... items) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(items)));
    }

    public static <K, V> Map<K, V> immutableMapOf() {
        return Collections.emptyMap();
    }
}
