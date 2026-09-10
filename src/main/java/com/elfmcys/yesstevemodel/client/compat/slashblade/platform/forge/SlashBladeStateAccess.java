package com.elfmcys.yesstevemodel.client.compat.slashblade.platform.forge;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.UnsafeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.ComboState;

public class SlashBladeStateAccess {

    private static long comboSeqFieldOffset = -1;

    public static void initialize() {
        try {
            comboSeqFieldOffset = UnsafeUtil.getUnsafe().objectFieldOffset(SlashBladeState.class.getDeclaredField("comboSeq"));
        } catch (NoSuchFieldException e) {
            e.fillInStackTrace();
        }
    }

    public static String getComboState(SlashBladeState slashBladeState, long j) {
        Object object = UnsafeUtil.getUnsafe().getObject(slashBladeState, comboSeqFieldOffset);
        if (!(object instanceof ComboState)) {
            return StringPool.EMPTY;
        }
        ComboState comboState = (ComboState) object;
        if (j > comboState.getTimeoutMS()) {
            return StringPool.EMPTY;
        }
        String name = comboState.getName();
        if (name.startsWith("ex_")) {
            name = name.substring(3);
        }
        return "slashblade:" + name;
    }
}