package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.19.2 && neoforge
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;*/
//? if >=1.19 && forge
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
 *///?}
//? if <1.17 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.client.registry.ClientRegistry;*/
//?}
//? if >=1.17 && <1.18 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
 *///?}
//? if >=1.18 && <1.18.2 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.ClientRegistry;
 *///?}
//? if >=1.18.2 && <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.ClientRegistry;
 *///?}
//? if >=1.19.2 {
//?}
//? if neoforge
/*import net.neoforged.bus.api.EventPriority;*/
//? if forge
import net.minecraftforge.eventbus.api.EventPriority;
//? if neoforge
/*import net.neoforged.bus.api.SubscribeEvent;*/
//? if forge
import net.minecraftforge.eventbus.api.SubscribeEvent;
//? if neoforge && >=1.20.5
/*import net.neoforged.fml.common.EventBusSubscriber;*/
//? if neoforge && <1.20.5
/*import net.neoforged.fml.common.Mod;*/
//? if forge
import net.minecraftforge.fml.common.Mod;
//? if neoforge
/*import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;*/
//? if forge
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * mod 总线事件走注解式静态订阅（与 platform/forge/ForgeClientSetupHooks 同款），
 * 不依赖 {@link #register()} 的构造期调用时序。
 */
// 1.21.6+ EventBusSubscriber 删 bus 属性（单总线自动路由）
//? if neoforge && >=1.20.5 && <21.6
/*@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)*/
//? if neoforge && >=21.6
/*@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT)*/
//? if neoforge && <1.20.5
/*@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)*/
//? if forge
@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetupEvent {

    private ClientSetupEvent() {
    }

    /**
     * 键位注册：1.19.3+ 走 mod 总线 RegisterKeyMappingsEvent；1.16.5 无该事件（类不存在），
     * 由 FMLClientSetupEvent enqueueWork 调 ClientRegistry.registerKeyBinding（ClientRegistry javadoc
     * 即注明 "Call this during FMLClientSetupEvent"，unimined 1.16.5 mojmap jar javap 实证签名
     * registerKeyBinding(KeyMapping)）。两侧条件一致：模型开关键无条件，其余仅原生可用时。
     * 原生初始化检查改由 mod 总线事件承担（见下），此处仅保留原有的构造期副作用。
     */
    public static void register() {
        if (YesSteveModel.isAvailable()) {
            AnimationRegister.registerAnimationState();
        }
    }

    // RegisterKeyMappingsEvent 1.19.0 已有（f119 sources 实证）→ 键位注册边界 1.19.2 放宽 1.19；
    // 1.19.0 ClientRegistry 已删 → <1.19 才走 ClientRegistry 路径
    //? if >=1.19 {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PlayerModelToggleKey.KEY_MAPPING);
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.register(AnimationRouletteKey.KEY_ROULETTE);
        event.register(AnimationRouletteKey.KEY_LOCK);
        event.register(DebugAnimationKey.KEY_MAPPING);
        event.register(ExtraPlayerRenderKey.KEY_MAPPING);
        for (KeyMapping mapping : ExtraAnimationKey.getKeyMappings()) {
            event.register(mapping);
        }
    }
    //?} else {
    /*private static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(PlayerModelToggleKey.KEY_MAPPING);
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        ClientRegistry.registerKeyBinding(AnimationRouletteKey.KEY_ROULETTE);
        ClientRegistry.registerKeyBinding(AnimationRouletteKey.KEY_LOCK);
        ClientRegistry.registerKeyBinding(DebugAnimationKey.KEY_MAPPING);
        ClientRegistry.registerKeyBinding(ExtraPlayerRenderKey.KEY_MAPPING);
        for (KeyMapping mapping : ExtraAnimationKey.getKeyMappings()) {
            ClientRegistry.registerKeyBinding(mapping);
        }
    }*/
//?}

    /**
     * 原 ClientLifecycleEvent.CLIENT_STARTED 在 forge 端由 mixin 注入 Minecraft.run() 开头（渲染线程、GL 上下文已就绪）。
     * FMLClientSetupEvent 是并行分发事件（worker 线程），GL 调用必须 enqueueWork 回主线程；
     * HIGH 优先级保证该检查排在 ForgeClientSetupHooks 的兼容初始化/默认模型加载之前，维持原先后顺序。
     * 1.16.5：键位注册同走 enqueueWork（前置，注册动作本身无 GL 依赖）。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 1.16.1 forge 32 无 enqueueWork/ParallelDispatchEvent → DeferredWorkQueue.runLater（同主线程排队语义）
        //? if <1.16.2 {
        /*net.minecraftforge.fml.DeferredWorkQueue.runLater(ClientSetupEvent::registerKeyBindings);*/
        //?}
        //? if >=1.16.2 && <1.19 {
        /*event.enqueueWork(ClientSetupEvent::registerKeyBindings);*/
        //?}
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        //? if <1.16.2
        /*net.minecraftforge.fml.DeferredWorkQueue.runLater(ClientSetupEvent::checkNativeInitialization);*/
        // 1.16.1 无 enqueueWork（forge 32）；ce976e5 曾误改 >=1.16.2→<26 致两分支撞车（1161 五错实证）
        //? if >=1.16.2 && <26
        event.enqueueWork(ClientSetupEvent::checkNativeInitialization);
        // 26.x：loader 11 DeferredWorkQueue 改 modloading-sync-worker 线程执行（无 GL 上下文，
        // 26.1.2 tour 首跑 FATAL nglGetIntegerv 实证）→ GL 探针移交渲染线程首帧执行
        //（execute 入渲染任务队列，语义同=失败抛 RuntimeException，仅时点后移）
        //? if >=26
        /*net.minecraft.client.Minecraft.getInstance().execute(ClientSetupEvent::checkNativeInitialization);*/
    }

    public static Object nativeClientInit() {
        try {
            int maxTexSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            if (maxTexSize <= 0) {
                // Component.literal 为 1.19.4+ 工厂方法，1.16.5 用 new TextComponent
                //? if >=1.19
                return Component.literal("YSM: OpenGL context not available");
                //? if <1.19
                /*return new TextComponent("YSM: OpenGL context not available");*/
            }
            // 原始C++碼檢查了GL20（著色器）和 GL30（VAO）的可用性
            try {
                int testShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
                if (testShader != 0) {
                    GL20.glDeleteShader(testShader);
                }
            } catch (Exception e) {
                //? if >=1.19
                return Component.literal("YSM: GL20 (shaders) not available");
                //? if <1.19
                /*return new TextComponent("YSM: GL20 (shaders) not available");*/
            }

            // 预載入default模型，延遲至第一次渲染tick
            // 不能在FMLClientSetupEvent中同步執行ModelAssembler，會導致StackOverflow
            //ClientModelManager.schedulePreloadDefaultModel();
            return null; // 成功
        } catch (Exception e) {
            //? if >=1.19
            return Component.literal("YSM Client Init Failed: " + e.getMessage());
            //? if <1.19
            /*return new TextComponent("YSM Client Init Failed: " + e.getMessage());*/
        }
    }

    private static void checkNativeInitialization() {
        Component component = (Component) nativeClientInit();
        if (component != null) {
            throw new RuntimeException("YSM Client Initialization Failed: " + component.getString(256));
        }
    }

    // 這裡本來有一個native方法，可能是運行時會初始化載入模型
}
