package com.elfmcys.yesstevemodel.platform.neoforge.event;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。
 * forge 原类经 InterModComms（IMC）接收第三方 mod 注册的自定义换装屏；NeoForge 1.20.2+
 * 移除了 IMC 体系（净室重写，无对应事件）——本孪生为无功能占位：第三方 mod 需走各自
 * neoforge 适配层接入（本域属第三方兼容面，compat 闸门口径）。语义降级点已入交接账。
 */
public final class ModScreenForgeHook {
    private ModScreenForgeHook() {
    }
}
