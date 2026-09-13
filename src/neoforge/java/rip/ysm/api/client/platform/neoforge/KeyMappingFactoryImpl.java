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

    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.ALT, type, keyCode, category);
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE, type, keyCode, category);
    }

    @SuppressWarnings({"removal"})
    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.matches(keyCode, scanCode) && keyMapping.getKeyModifier().equals(KeyModifier.getActiveModifier());
    }
}
