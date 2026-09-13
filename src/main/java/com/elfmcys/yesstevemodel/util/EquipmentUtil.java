package com.elfmcys.yesstevemodel.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.world.entity.EquipmentSlot;
// 1.21.2 UseAnim 改名 ItemUseAnimation：消费方统一走 rip.ysm.util.UseAction 门面
//（name() 映射，Rl 同模式，无版本条件）
import rip.ysm.util.UseAction;
import org.apache.commons.lang3.EnumUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EquipmentUtil {

    private static final Object2ReferenceOpenHashMap<String, EquipmentSlot> SLOT_BY_NAME = new Object2ReferenceOpenHashMap<>((Map) Arrays.stream(EquipmentSlot.values()).collect(Collectors.toMap(equipmentSlot -> equipmentSlot.getName().toLowerCase(Locale.US), equipmentSlot2 -> equipmentSlot2)));

    public static Optional<UseAction> getUseAnimByName(String str) {
        return Optional.ofNullable(EnumUtils.getEnum(UseAction.class, str.toUpperCase(Locale.US)));
    }

    public static Optional<EquipmentSlot> getEquipmentSlotByName(String str) {
        return Optional.ofNullable(SLOT_BY_NAME.get(str));
    }
}