package rip.ysm.compat.sbackpack;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import rip.ysm.compat.sbackpack.platform.forge.SBackpackCompatImpl;

public final class SBackpackCompat {

    private SBackpackCompat() {
    }

    public static boolean isLoaded() {
        return SBackpackCompatImpl.isLoaded();
    }

    public static void setupRenderLayers() {
        SBackpackCompatImpl.setupRenderLayers();
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return SBackpackCompatImpl.getInCompatibleInfo();
    }

    public static void registerControllerFunctions(CtrlBinding binding) {
        SBackpackCompatImpl.registerControllerFunctions(binding);
    }

    public static ItemStack getBackpackItem(LivingEntity livingEntity) {
        return SBackpackCompatImpl.getBackpackItem(livingEntity);
    }
}
