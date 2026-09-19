package com.elfmcys.yesstevemodel.platform.neoforge1202;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import rip.ysm.api.PlatformAPI;

/**
 * Forge 主类。原 architectury {@code EventBuses.registerModEventBus} 接线已移除
 * （该调用只为 architectury 内部全局总线表服务；本仓注册表路径实测不读它，
 * 见 architectury-forge 9.2.14 字节码无 getModEventBus 引用）。mod 事件总线改为
 * 静态持有 + {@link #getModEventBus()} 钩子，供 @Mod.EventBusSubscriber 无法覆盖的
 * 接线使用（如 Forge DeferredRegister.register(IEventBus) 显式总线形态）。
 */
@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelForge {
    private static volatile IEventBus modEventBus;

    public YesSteveModelForge() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus = bus;
        // RegisterCapabilitiesEvent 1.19+ 才有；<1.17 能力注册走 @CapabilityInject/CapabilityEvent.register（thinlayer 卡路径）
        bus.addListener(YesSteveModelForge::onRegisterCapabilities);
        YesSteveModel.init();
    }

    /** mod 事件总线（构造期赋值，此后只读）；供注册类域显式接线复用。 */
    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    // <1.17 无 RegisterCapabilitiesEvent（1.19+ 引入）：能力注册改走 @CapabilityInject +
    // CapabilityEvent.register 构造期注册（m2-forge-thinlayer-condition 卡路径），此处整方法不进编译
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.register(ModelInfoCapability.class);
        event.register(ProjectileModelCapability.class);
        event.register(VehicleModelCapability.class);
        event.register(AuthModelsCapability.class);
        event.register(StarModelsCapability.class);
        if (!PlatformAPI.isServer()) {
            event.register(PlayerCapability.class);
            event.register(ProjectileCapability.class);
            event.register(VehicleCapability.class);
        }
    }
}
