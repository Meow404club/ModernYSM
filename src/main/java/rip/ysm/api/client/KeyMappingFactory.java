package rip.ysm.api.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;
import rip.ysm.api.client.platform.forge.KeyMappingFactoryImpl;

public final class KeyMappingFactory {

    private KeyMappingFactory() {
    }

    @ExpectPlatform
    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return KeyMappingFactoryImpl.createInGameAlt(name, type, keyCode, category);
    }

    @ExpectPlatform
    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return KeyMappingFactoryImpl.createInGameNone(name, type, keyCode, category);
    }

    @ExpectPlatform
    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return KeyMappingFactoryImpl.isActiveAndMatches(keyMapping, keyCode, scanCode);
    }
}
