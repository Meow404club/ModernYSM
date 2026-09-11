package rip.ysm.compat.curios;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import com.elfmcys.yesstevemodel.util.YsmTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.List;
import rip.ysm.compat.curios.platform.forge.CuriosCompatImpl;

public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return CuriosCompatImpl.isLoaded();
    }

    public static boolean hasItemInSlot(LivingEntity livingEntity, String str, ReferenceOpenHashSet<Item> set) {
        return CuriosCompatImpl.hasItemInSlot(livingEntity, str, set);
    }

    public static boolean hasTaggedItemInSlot(LivingEntity livingEntity, String str, List<YsmTag.ItemTag> list) {
        return CuriosCompatImpl.hasTaggedItemInSlot(livingEntity, str, list);
    }

    public static boolean hasNoTaggedItemInSlot(LivingEntity entity, String str, List<YsmTag.ItemTag> list) {
        return CuriosCompatImpl.hasNoTaggedItemInSlot(entity, str, list);
    }

    public static void registerCuriosItems(ContextBinding binding) {
        CuriosCompatImpl.registerCuriosItems(binding);
    }
}
