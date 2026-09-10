package com.elfmcys.yesstevemodel.geckolib3.core.util;

import java.util.Objects;

/**
 * 1.16.5 兼容 shim：MC 1.16.5 自带 fastutil 8.3.1，缺 8.5.0 引入的
 * it.unimi.dsi.fastutil.ints.IntReferenceImmutablePair。本类与其同名不同包，
 * 仅镜像共享源实际使用的成员（构造器/leftInt/right + 值语义 equals/hashCode），
 * 语义与 fastutil 一致：不可变（无 setter）。1.20.1 构建不引用本类
 * （使用点经 stonecutter import 分支选 fastutil 原类）。
 * Task: m2-render-pipeline-condition
 */
public final class IntReferenceImmutablePair<V> {

    private final int key;

    private final V value;

    public IntReferenceImmutablePair(int key, V value) {
        this.key = key;
        this.value = value;
    }

    public int leftInt() {
        return this.key;
    }

    public V right() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntReferenceImmutablePair)) {
            return false;
        }
        IntReferenceImmutablePair<?> that = (IntReferenceImmutablePair<?>) o;
        return this.key == that.key && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }

    @Override
    public String toString() {
        return "[" + this.key + "->" + this.value + "]";
    }
}
