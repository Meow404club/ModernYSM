package com.elfmcys.yesstevemodel.config;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 音效注册（Forge 原生）：architectury DeferredRegister/RegistrySupplier →
 * Forge DeferredRegister/RegistryObject（MC 1.20.1，Forge 47）。
 * <p>
 * Forge DeferredRegister 只有 {@code register(IEventBus)}
 * （forge-1.20.1 DeferredRegister.java:312）；platform-util 卡接线主类后由
 * YesSteveModel.initConfig 以 {@code REGISTER.register(YesSteveModelForge.getModEventBus())}
 * 显式传总线，无参形式已随 architectury EventBuses 门面一并退役（mig-purge-architectury）。
 */
public class ModSoundEvents {

    public static final SoundEventsRegister REGISTER = new SoundEventsRegister(ForgeRegistries.SOUND_EVENTS, YesSteveModel.MOD_ID);

    public static final RegistryObject<SoundEvent> CUSTOM_SOUND = REGISTER.register("custom",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(YesSteveModel.MOD_ID, "custom"), 16.0f));

    public static final class SoundEventsRegister {

        private final DeferredRegister<SoundEvent> delegate;

        private SoundEventsRegister(IForgeRegistry<SoundEvent> registry, String modId) {
            this.delegate = DeferredRegister.create(registry, modId);
        }

        public <I extends SoundEvent> RegistryObject<I> register(String name, Supplier<? extends I> sup) {
            return delegate.register(name, sup);
        }

        /** 供 @Mod 主类显式接线（YesSteveModel.initConfig）。 */
        public void register(IEventBus modBus) {
            delegate.register(modBus);
        }
    }
}
