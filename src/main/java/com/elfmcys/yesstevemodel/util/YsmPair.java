package com.elfmcys.yesstevemodel.util;

/**
 * 1.16.5 侧轻量二元组：fastutil Pair 接口 8.3.0 才引入（1.16.5 打包 fastutil 8.2.1，
 * jar 实证无 it.unimi.dsi.fastutil.Pair），<1.17 分支以本类对位；消费面同名 left()/right()。
 * 1.20.1 轴不使用（保持 fastutil Pair 原样，零行为变化）。
 */
public final class YsmPair {
    private YsmPair() {
    }

    public interface StrPair {
        String left();

        String right();
    }

    public static StrPair of(String left, String right) {
        return new StrPair() {
            @Override
            public String left() {
                return left;
            }

            @Override
            public String right() {
                return right;
            }
        };
    }
}
