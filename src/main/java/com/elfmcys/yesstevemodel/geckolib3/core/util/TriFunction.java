package com.elfmcys.yesstevemodel.geckolib3.core.util;

/**
 * 1.16.5 兼容 shim：MC 1.16.5 自带 commons-lang3 3.9，缺 3.12 引入的
 * org.apache.commons.lang3.function.TriFunction。本类与其同名不同包，
 * apply(A,B,C) 函数签名一致，lambda 目标类型兼容。
 * 1.20.1 构建不引用本类（使用点经 stonecutter import 分支选 commons 原类）。
 * Task: m2-render-pipeline-condition
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

    R apply(A a, B b, C c);
}
