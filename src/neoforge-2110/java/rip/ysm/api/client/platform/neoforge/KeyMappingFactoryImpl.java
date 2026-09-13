package rip.ysm.api.client.platform.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * neoforge 孪生（同 FQCN；RAW 源集）。KeyConflictContext/KeyModifier 包移动
 * net.minecraftforge.client.settings → net.neoforged.neoforge.client.settings（20.4.251 javap 实证），
 * 方法面同签名。
 */
public final class KeyMappingFactoryImpl {
    private KeyMappingFactoryImpl() {
    }

    // 1.21.10 KeyMapping 分类记录化：String category → KeyMapping.Category(ResourceLocation)
    //（2110 KeyMapping.java:293 record Category(ResourceLocation id) 实证）
    private static KeyMapping.Category ysmCategory(String category) {
        return new KeyMapping.Category(net.minecraft.resources.ResourceLocation.parse(category));
    }

    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.ALT, type, keyCode, ysmCategory(category));
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE, type, keyCode, ysmCategory(category));
    }

    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        // 1.21.5 KeyModifier.getActiveModifier 删除 → getActiveModifiers() 列表制
        //（neoforge-21.5.98-sources KeyModifier.java:140），「映射修饰键在活跃修饰键集合中」语义等价
        // 1.21.9/1.21.10 KeyMapping.matches(int,int) → matches(KeyEvent)（2110 KeyMapping.java:255）
        return keyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0))
                && KeyModifier.getActiveModifiers().contains(keyMapping.getKeyModifier());
    }
}
