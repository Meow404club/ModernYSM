package rip.ysm.compat.sbackpack;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import rip.ysm.compat.sbackpack.platform.forge.SBackpackCompatImpl;

public final class SBackpackCompat {

    private SBackpackCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        return SBackpackCompatImpl.isLoaded();
    }

    @ExpectPlatform
    public static void setupRenderLayers() {
        SBackpackCompatImpl.setupRenderLayers();
    }

    @ExpectPlatform
    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerControllerFunctions(CtrlBinding binding) {
        SBackpackCompatImpl.registerControllerFunctions(binding);
    }

    @ExpectPlatform
    public static ItemStack getBackpackItem(LivingEntity livingEntity) {
        return SBackpackCompatImpl.getBackpackItem(livingEntity);
    }
}
