package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * 26.2/26.3 PiP 预览渲染状态（debt-262-preview-pip；262/263 代树各一份孪生拷贝）。
 *
 * 为什么不是 AvatarRenderState：EntityRenderDispatcher.getRenderer(S) 对 AvatarRenderState
 * 一律走 vanilla playerRenderers 表（EntityRenderDispatcher.java:112-118 实证），PiP 绘制期
 * 会解析到原版 AvatarRenderer 渲原版玩家模型；非 Avatar 的 state 按 state.entityType 查
 * renderers 表，预览注册钩子（PreviewRendererRegisterHook）把 CustomPlayerRenderer 登记在
 * EntityTypes.PLAYER 下，PiP 由此解析回我方渲染器。vanilla 全部玩家实体/状态均为
 * AbstractClientPlayer/AvatarRenderState（恒走 avatar 分支），本注册项对其零可观测。
 *
 * ysmAnimatable：抽取窗内由 GeoReplacedEntityRenderer.extractRenderState 26.2 twin 从
 * ModelPreviewRenderer 暂存槽捕获（PiP 绘制期 CustomPlayerRenderer.submit 消费——
 * createRenderState(T,float) 只传实体不传 animatable，实体→animatable 无现成反查表，
 * 帧内窗式暂存=最小通路）。复用单实例随 state 走，无跨帧全局态。
 */
public final class YsmPreviewRenderState extends LivingEntityRenderState {

    public LivingAnimatable<?> ysmAnimatable;
}
