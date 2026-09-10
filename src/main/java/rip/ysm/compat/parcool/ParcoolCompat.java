package rip.ysm.compat.parcool;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.controller.IAnimationController;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.BiFunction;
import rip.ysm.compat.parcool.platform.forge.ParcoolCompatImpl;

public final class ParcoolCompat {

    private ParcoolCompat() {
    }

    public static boolean isLoaded() {
        return ParcoolCompatImpl.isLoaded();
    }

    public static Optional<Pair<String, String>> getInCompatibleInfo() {
        return ParcoolCompatImpl.getInCompatibleInfo();
    }

    public static Optional<BiFunction<String, CustomPlayerEntity, IAnimationController<CustomPlayerEntity>>> getControllerFactory() {
        return ParcoolCompatImpl.getControllerFactory();
    }

    public static boolean isPlayerParcooling(Player player) {
        return ParcoolCompatImpl.isPlayerParcooling(player);
    }

    public static String getActionName(Player player) {
        return ParcoolCompatImpl.getActionName(player);
    }

    public static void registerBindings(CtrlBinding binding) {
        ParcoolCompatImpl.registerBindings(binding);
    }
}
