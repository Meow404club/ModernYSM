// 1.16.5 版本独有 shim（m2-gate-compat）：mod-absent 语义，见 BetterCombatCompat 头注。
package rip.ysm.compat.curios;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.util.YsmTag;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.List;

public final class CuriosCompat {

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean hasItemInSlot(LivingEntity livingEntity, String str, ReferenceOpenHashSet<Item> set) {
        return false;
    }

    // 调用面改 YsmTag.ItemTag 句柄（TagKey 1.16.5 不存在，m2-compile-green-gate 同步 shim 签名）
    public static boolean hasTaggedItemInSlot(LivingEntity livingEntity, String str, List<YsmTag.ItemTag> list) {
        return false;
    }

    public static boolean hasNoTaggedItemInSlot(LivingEntity entity, String str, List<YsmTag.ItemTag> list) {
        return false;
    }

    public static void registerCuriosItems(ContextBinding binding) {
    }
}
