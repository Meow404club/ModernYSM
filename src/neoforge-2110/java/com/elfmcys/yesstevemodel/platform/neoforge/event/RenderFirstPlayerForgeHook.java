package com.elfmcys.yesstevemodel.platform.neoforge.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.event.RenderFirstPlayerBackground;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterEntities;

/**
 * neoforge 孪生（同 FQCN；RAW 源集，1.20.5+ 代）。1.20.4 版孪生在 src/neoforge-1204/java。
 * 1.20.5+ 断裂：@Mod.EventBusSubscriber 删除（1.20.4 版还误 import 了 TickEvent——1.20.5+ 已无此类，
 * 一并移除）；RenderLevelStageEvent/RenderHandEvent 1.20.6 同名同方法面保留。
 */
@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)
public final class RenderFirstPlayerForgeHook {
    private RenderFirstPlayerForgeHook() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(AfterEntities event) {
        // 1.21.8 RenderLevelStageEvent 抽象化+Stage 枚举删 → 子事件类直接定档；1.21.10 再删
        // AfterBlockEntities 子类（neoforge-21.10.64 RenderLevelStageEvent.java:115-124 实证，
        // 子事件列 AfterEntities 直接跳 AfterTranslucentBlocks）→ 取 AfterEntities（实体/块实体
        // 档前复位，窗口比 21.8 略宽），语义差继续记功能债
        RenderFirstPlayerBackground.resetFrame();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // 1.21.10 RenderHandEvent.getMultiBufferSource 删（getSubmitNodeCollector 替代，
        // 2110 RenderHandEvent.java:73）→ Static 直绘取 renderBuffers 即时缓冲（预览同路），
        // 命中 GuiRenderer 合批的语义差记功能债
        RenderFirstPlayerBackground.onRenderHand(event.getPoseStack(), net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(), event.getPackedLight(), event.getPartialTick());
    }
}
