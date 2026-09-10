package com.elfmcys.yesstevemodel.config;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * 音效注册（Forge 原生）：architectury DeferredRegister/RegistrySupplier →
 * Forge DeferredRegister/RegistryObject（MC 1.20.1，Forge 47）。
 * <p>
 * architectury 的无参 {@code REGISTER.register()} 依赖 {@code EventBuses} 全局总线表
 * （由 @Mod 主类构造期注册）；Forge DeferredRegister 只有 {@code register(IEventBus)}
 * （forge-1.20.1 DeferredRegister.java:312）。为不触碰主类接线（归 mig-platform-util 卡），
 * {@link #REGISTER} 是包内薄包装：无参 {@code register()} 在 mod 构造链内
 * （YesSteveModel.init ← @Mod 构造器）自解析 FMLJavaModLoadingContext 总线，
 * 语义与 architectury EventBuses 等价；platform-util 卡接线主类后可改调
 * {@code register(IEventBus)} 显式传总线，届时可删无参形式。
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

        /**
         * 兼容旧调用点（YesSteveModel.initConfig）：等价 architectury 无参 register。
         * 仅可在 mod 构造期内调用（FMLJavaModLoadingContext 上下文存在）。
         */
        public void register() {
            delegate.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        /** 供 @Mod 主类显式接线：主类持有 mod event bus 时直接传入。 */
        public void register(IEventBus modBus) {
            delegate.register(modBus);
        }
    }
}
