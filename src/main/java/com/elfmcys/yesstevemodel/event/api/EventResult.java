package com.elfmcys.yesstevemodel.event.api;

import org.jetbrains.annotations.Nullable;

/**
 * 项目内事件结果三元组，语义对齐 architectury 9.2.14 的
 * architectury event 包 EventResult 的复刻（javap 核对：PASS/STOP/TRUE/FALSE
 * 四单例 + interruptsFurtherEvaluation/value 两字段）：
 * pass = 不干预且不拦截后续监听；interrupt = 干预并拦截后续监听；
 * fail* 为 interrupt* 别名（新版 architectury 命名，保留以便后续卡迁移）。
 */
public final class EventResult {

    private static final EventResult PASS = new EventResult(false, null);
    private static final EventResult TRUE = new EventResult(true, Boolean.TRUE);
    private static final EventResult FALSE = new EventResult(true, Boolean.FALSE);
    private static final EventResult STOP = new EventResult(true, null);

    private final boolean interruptsFurtherEvaluation;
    @Nullable
    private final Boolean value;

    private EventResult(boolean interruptsFurtherEvaluation, @Nullable Boolean value) {
        this.interruptsFurtherEvaluation = interruptsFurtherEvaluation;
        this.value = value;
    }

    public static EventResult pass() {
        return PASS;
    }

    public static EventResult interrupt(@Nullable Boolean value) {
        if (value == null) {
            return STOP;
        }
        return value ? TRUE : FALSE;
    }

    public static EventResult interruptTrue() {
        return TRUE;
    }

    public static EventResult interruptDefault() {
        return STOP;
    }

    public static EventResult interruptFalse() {
        return FALSE;
    }

    public static EventResult fail(@Nullable Boolean value) {
        return interrupt(value);
    }

    public static EventResult failTrue() {
        return TRUE;
    }

    public static EventResult failDefault() {
        return STOP;
    }

    public static EventResult failFalse() {
        return FALSE;
    }

    public boolean interruptsFurtherEvaluation() {
        return interruptsFurtherEvaluation;
    }

    @Nullable
    public Boolean value() {
        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isTrue() {
        return value == Boolean.TRUE;
    }

    public boolean isFalse() {
        return value == Boolean.FALSE;
    }
}
