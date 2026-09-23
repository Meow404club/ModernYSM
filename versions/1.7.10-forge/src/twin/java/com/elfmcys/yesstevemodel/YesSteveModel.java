package com.elfmcys.yesstevemodel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.7.10 原生孪生主类（legacy1710-l2a-model-load）。
 *
 * 共享 YesSteveModel 绑定现代 Minecraft/LocalPlayer/Component + 平台钩子链，
 * 1.7.10 无对应面。本孪生只承载编译/装载链消费的最小面：
 * MOD_ID/LOGGER/isAvailable()（1.12.2 twin YesSteveModel.java 同构）。
 */
public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static boolean available;

    private YesSteveModel() {
    }

    public static void init() {
        LOGGER.info("Initializing YesSteveModel (legacy 1.7.10 twin)");
        available = true;
    }

    public static boolean isAvailable() {
        return available;
    }
}
