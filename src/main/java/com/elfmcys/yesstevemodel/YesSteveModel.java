package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.YsmText;
import com.elfmcys.yesstevemodel.config.ModSoundEvents;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.event.YsmEventBootstrap;
import com.elfmcys.yesstevemodel.platform.YsmPlatform;
//? if neoforge && >=1.20.3
/*import com.elfmcys.yesstevemodel.platform.neoforge.YesSteveModelForge;*/
//? if neoforge && <1.20.3
/*import com.elfmcys.yesstevemodel.platform.neoforge1202.YesSteveModelForge;*/
//? if forge
import com.elfmcys.yesstevemodel.platform.forge.YesSteveModelForge;
import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/
//? if forge
import net.minecraftforge.api.distmarker.Dist;
//? if neoforge
/*import net.neoforged.api.distmarker.OnlyIn;*/
//? if forge
import net.minecraftforge.api.distmarker.OnlyIn;
//? if neoforge
/*import net.neoforged.fml.config.ModConfig;*/
//? if forge
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.config.ConfigRegistration;

import java.io.File;
import java.io.IOException;

/**
 * TODO:
 * 默认模型应该就在模组架加载的时候就预加载了
 * 其它模型统统都是进入世界后加载
 */
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private YesSteveModel() {
    }

    public static void init() {
        LOGGER.info("Initializing YesSteveModel, platform: " + PlatformAPI.getPlatformName());
        try {
            NativeLibLoader.init();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize native lib", e);
        }
        if (!NativeLibLoader.isAvailable()) {
            LOGGER.error(getErrorMessage());
        } else {
            initConfig();
        }
        YsmEventBootstrap.register();
    }

    private static void initConfig() {
        File oldConfig = YsmPlatform.getConfigFolder().resolve("yes_steve_model-common.toml").toFile();
        if (oldConfig.isFile()) {
            File file2 = YsmPlatform.getConfigFolder().resolve("yes_steve_model-client.toml").toFile();
            if (!file2.isFile()) {
                oldConfig.renameTo(file2);
            } else {
                oldConfig.delete();
            }
        }
        ConfigRegistration.register(MOD_ID, ModConfig.Type.CLIENT, GeneralConfig.buildSpec());
        ConfigRegistration.register(MOD_ID, ModConfig.Type.SERVER, ServerConfig.buildSpec());
        if (!PlatformAPI.isServer()) {
            // registry-config 卡交接：Forge DeferredRegister 显式总线形态；
            // 总线来自主类静态钩子（原 architectury 无参 register 依赖其全局表）
            ModSoundEvents.REGISTER.register(YesSteveModelForge.getModEventBus());
        }
    }

    @Keep
    public static boolean isAvailable() {
        return NativeLibLoader.isAvailable();
    }

    public static boolean isOnAndroid() {
        return NativeLibLoader.isOnAndroid();
    }

    // 原 fabric @Environment(EnvType.CLIENT) → Forge @OnlyIn 等价替换：
    // 专用服剥离本方法（调用方均在 client 包，见 ClientPlayerJoinNotification/PlayerModelToggleKey）
    // server-dist 缺口修复：neoforge 本方法结构性隔离至 client.ClientModelManager——21.7 起
    // RuntimeDistCleaner 成员剥离废除，@OnlyIn 失去保护力，方法体 Minecraft/LocalPlayer 引用
    // 在类链接期解析 → dedicated server CNFE（21.7/21.9 runServer 实证）；forge 保留原方法。
    // 注解门 >=1.20.1 && <1.20.4 仅命中 1.20.1 线：1201 vcs 直通按原文编译裸行，注解必须
    // 保持基线原位（字节码零差口径）；其余 stonecutter 线（1165 等基线本就无注解）整块剥除
//? if >=1.20.1 && <1.20.4 {
    @OnlyIn(Dist.CLIENT)
//?}
    // forge 注解门必须独占一块（unimined-env-116x）：chasm 只解包「整块内容均为包裹态」的
    // /* */（ClientSetupEvent registerKeyBindings 块同款实证）；混入裸代码后包裹行保持
    // 注释态——58aaaf2 曾把注解与方法体并入同一 forge 块 → 注解静默消失 → 1.16.1 专用服
    // 在 forge 32（modlauncher 6.1.1）严格 dist-cleaner 下炸 LocalPlayer invalid-dist
    // （校验器赋值兼容检查拉起 LocalPlayer；1161 runServer 实证。1165 的 forge36/modlauncher
    // 8.1.3 容忍同形态 → 回归仅击穿 1.16.x 老代 runServer）。
//? if forge {
/*    @OnlyIn(Dist.CLIENT)*/
//?}
    public static void sendUnavailableMessage() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            YsmText.sendSystemMessage(localPlayer, getUnavailableComponent());
        }
    }

    public static Component getUnavailableComponent() {
        return NativeLibLoader.getErrorComponent();
    }

    public static String getErrorMessage() {
        return NativeLibLoader.getErrorMessage();
    }
}
