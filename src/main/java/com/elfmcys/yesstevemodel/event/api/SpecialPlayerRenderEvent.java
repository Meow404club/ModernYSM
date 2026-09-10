package com.elfmcys.yesstevemodel.event.api;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpecialPlayerRenderEvent {

    public static final Event EVENT = new Event();

    /**
     * 自建事件累加器，复刻 architectury EventFactory.createEventResult 的
     * invoker 语义（EventFactory$5 字节码核对）：按注册顺序调用监听器，
     * 某个监听器返回 interrupt/fail 即短路返回该结果，全部 pass 则返回 pass()。
     */
    public static final class Event {

        private final List<RenderHandler> handlers = new CopyOnWriteArrayList<>();

        public void register(RenderHandler handler) {
            handlers.add(handler);
        }
    }

    @FunctionalInterface
    public interface RenderHandler {
        EventResult onRender(SpecialPlayerRenderEvent event);
    }

    public static EventResult post(SpecialPlayerRenderEvent event) {
        for (RenderHandler handler : EVENT.handlers) {
            EventResult result = handler.onRender(event);
            if (result.interruptsFurtherEvaluation()) {
                return result;
            }
        }
        return EventResult.pass();
    }

    private final Player player;

    private final CustomPlayerEntity customPlayer;

    private final String modelId;

    @Nullable
    private ResourceLocation textureLocation;

    public SpecialPlayerRenderEvent() {
        this.player = null;
        this.customPlayer = null;
        this.modelId = null;
    }

    public SpecialPlayerRenderEvent(Player player, CustomPlayerEntity customPlayer, String str) {
        this.player = player;
        this.customPlayer = customPlayer;
        this.modelId = str;
    }

    public Player getPlayer() {
        return this.player;
    }

    public CustomPlayerEntity getCustomPlayer() {
        return this.customPlayer;
    }

    public String getModelId() {
        return this.modelId;
    }

    @Nullable
    public ResourceLocation getTextureLocation() {
        return this.textureLocation;
    }

    public void setTextureLocation(@Nullable ResourceLocation resourceLocation) {
        this.textureLocation = resourceLocation;
    }
}
