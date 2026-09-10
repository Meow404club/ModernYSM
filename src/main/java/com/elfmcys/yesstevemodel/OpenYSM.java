package com.elfmcys.yesstevemodel;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * M0 stonecutter 骨架空壳入口：仅验证 stonecutter + legacyforge 构建链路，
 * m0-merge-sources 卡合并真实业务源码时将被真实主类取代。
 */
@Mod(OpenYSM.MOD_ID)
public final class OpenYSM {
    public static final String MOD_ID = "openysm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenYSM() {
        LOGGER.info("Mod openysm loaded");
    }
}
