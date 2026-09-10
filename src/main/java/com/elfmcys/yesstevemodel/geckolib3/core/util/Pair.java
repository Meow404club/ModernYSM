package com.elfmcys.yesstevemodel.geckolib3.core.util;

import java.util.Objects;

/**
 * 1.16.5 兼容 shim：MC 1.16.5 自带 fastutil 8.3.1，缺 8.5.0 引入的
 * it.unimi.dsi.fastutil.Pair 通用二元组接口。本类与其同名不同包，仅镜像共享源
 * 实际使用的成员（of 工厂/left/right/getKey/getValue），不可变值语义。
 * 1.20.1 构建不引用本类（使用点经 stonecutter import 分支选 fastutil 原类）。
 * Task: m2-render-pipeline-condition
 */
public final class Pair<L, R> {

    private final L left;

    private final R right;

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public L left() {
        return this.left;
    }

    public R right() {
        return this.right;
    }

    public L getKey() {
        return this.left;
    }

    public R getValue() {
        return this.right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> that = (Pair<?, ?>) o;
        return Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.right);
    }

    @Override
    public String toString() {
        return "(" + this.left + "," + this.right + ")";
    }
}
