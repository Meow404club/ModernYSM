package com.elfmcys.yesstevemodel.platform.neoforge;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。
 * forge 原类唯一活跃分支是 &lt;1.17 的 RenderGameOverlayEvent.Post(DEBUG) 钩子（1.20.1 展开态
 * 本就无活跃 HUD 钩子，调试浮层经 AnimationDebugOverlay 自身路径渲染），故 neoforge 三线
 * （均 &gt;=1.20.4）无对应事件需要挂接——本孪生保持类名占位以维持 exclude/孪生矩阵完整。
 */
public final class ForgeHudOverlayEvents {
    private ForgeHudOverlayEvents() {
    }
}
