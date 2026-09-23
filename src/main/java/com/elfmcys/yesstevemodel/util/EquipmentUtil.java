package com.elfmcys.yesstevemodel.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
// 1.12.2=EntityEquipmentSlot（vanilla-mc-1122 EntityEquipmentSlot.java:35 getName/:40 values
// 实证，compile jar javap 复核；mainhand/offhand 自 1.9 起存在）
//? if >=1.13
import net.minecraft.world.entity.EquipmentSlot;
//? if <1.13
/*import net.minecraft.inventory.EntityEquipmentSlot;*/
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

    //? if >=1.13
    private static final Object2ReferenceOpenHashMap<String, EquipmentSlot> SLOT_BY_NAME = new Object2ReferenceOpenHashMap<>((Map) Arrays.stream(EquipmentSlot.values()).collect(Collectors.toMap(equipmentSlot -> equipmentSlot.getName().toLowerCase(Locale.US), equipmentSlot2 -> equipmentSlot2)));
    //? if <1.13
    /*private static final Object2ReferenceOpenHashMap<String, EntityEquipmentSlot> SLOT_BY_NAME = new Object2ReferenceOpenHashMap<>((Map) Arrays.stream(EntityEquipmentSlot.values()).collect(Collectors.toMap(equipmentSlot -> equipmentSlot.getName().toLowerCase(Locale.US), equipmentSlot2 -> equipmentSlot2)));*/

    public static Optional<UseAction> getUseAnimByName(String str) {
        return Optional.ofNullable(EnumUtils.getEnum(UseAction.class, str.toUpperCase(Locale.US)));
    }

    //? if >=1.13 {
    public static Optional<EquipmentSlot> getEquipmentSlotByName(String str) {
        return Optional.ofNullable(SLOT_BY_NAME.get(str));
    }
    //?}
    //? if <1.13 {
    /*public static Optional<EntityEquipmentSlot> getEquipmentSlotByName(String str) {
        return Optional.ofNullable(SLOT_BY_NAME.get(str));
    }*/
    //?}
}