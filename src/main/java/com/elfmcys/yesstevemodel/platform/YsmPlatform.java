package com.elfmcys.yesstevemodel.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * architectury {@code Platform}/{@code GameInstance} 的 Forge 原生薄封装
 * （ADR 替代矩阵，M1 mig-platform-util）。方法名与语义对齐原 architectury API，
 * 调用方迁移是纯改名替换：
 * <ul>
 *   <li>{@code dev.architectury.platform.Platform} → 本类</li>
 *   <li>{@code dev.architectury.utils.GameInstance.getServer()} → {@link #getServer()}</li>
 * </ul>
 * 实现（Forge 1.20.1 源码核对）：
 * <ul>
 *   <li>ModList.isLoaded / getMods / getModContainerById — fmlcore ModList.java:172,182,187</li>
 *   <li>FMLPaths.CONFIGDIR.get() — fmlloader FMLPaths.java:24</li>
 *   <li>FMLEnvironment.dist / production — fmllauncher FMLEnvironment.java:17,19</li>
 *   <li>ServerLifecycleHooks.getCurrentServer() — net.minecraftforge.server:146</li>
 * </ul>
 */
public final class YsmPlatform {
    private YsmPlatform() {
    }

    /** 原 Platform.isModLoaded：compat 探测统一入口（ModList 索引，mods.toml 声明即可见）。 */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** 原 Platform.getConfigFolder：游戏目录下 config/。 */
    public static Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** 原 Platform.getEnv：逻辑侧物理端。 */
    public static Dist getEnv() {
        return FMLEnvironment.dist;
    }

    /** 原 Platform.isDevelopmentEnvironment：dev 运行时为 true（生产为 false）。 */
    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    /** 原 GameInstance.getServer：客户端单机=集成服实例，专用服=服务端实例，未启动为 null。 */
    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * 原 Platform.getMod。mod 未加载时抛 {@link IllegalArgumentException}
     * （对齐 architectury ModNotFoundException 的快速失败语义；调用方需先 {@link #isModLoaded}）。
     */
    public static YsmModInfo getMod(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> new YsmModInfo(container.getModInfo()))
                .orElseThrow(() -> new IllegalArgumentException("Mod not found: " + modId));
    }

    /** 原 Platform.getMods（不可变快照）。 */
    public static Collection<YsmModInfo> getMods() {
        return Collections.unmodifiableList(ModList.get().getMods().stream()
                .map(YsmModInfo::new)
                .toList());
    }
}
