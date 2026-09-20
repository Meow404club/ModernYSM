package com.elfmcys.yesstevemodel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2 原生孪生主类（legacy-1222-l1-render）。
 *
 * 共享 YesSteveModel 绑定现代 Minecraft/LocalPlayer/Component + 平台 forge 钩子链
 * （1.20.1 口径），1.12.2 全量接入是 L2 量级。本孪生只承载编译/装载链消费的最小面：
 * MOD_ID/LOGGER/isAvailable()。L2 事件/网络/配置接入时再按共享版逐段平铺。
 */
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static boolean available;

    private YesSteveModel() {
    }

    public static void init() {
        LOGGER.info("Initializing YesSteveModel (legacy 1.12.2 twin)");
        available = true;
    }

    public static boolean isAvailable() {
        return available;
    }
}
