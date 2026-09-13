// 1.16.5 版本独有 shim（m2-gate-compat）：逻辑与共享源 gun/tacz/ConditionTAC 一致
// （纯项目/原版依赖；getGunTexture 走本包 shim 返回 null → doTest 恒返回空串，
// 即 TAC 枪械条件在 mod 缺席时不命中，与守卫链行为一致）。
package rip.ysm.compat.gun.tacz;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import rip.ysm.util.Rl;

public class ConditionTAC {

    private static final String EMPTY = "";

    private final ObjectOpenHashSet<String> nameTest = new ObjectOpenHashSet<>();

    private final ObjectOpenHashSet<ResourceLocation> idTest = new ObjectOpenHashSet<>();

    public void addTest(String name) {
        if (!name.startsWith("tac:") || !name.contains("$")) {
            return;
        }
        String[] strArrSplit = StringUtils.split(name, "$", 2);
        if (strArrSplit.length < 2) {
            return;
        }
        String str2 = strArrSplit[1];
        if (Rl.isValid(str2)) {
            this.nameTest.add(name);
            this.idTest.add(ResourceLocation.parse(str2));
        }
    }

    public String doTest(ItemStack itemStack, String str) {
        if (itemStack.isEmpty()) {
            return EMPTY;
        }
        ResourceLocation gunId = TacCompat.getGunTexture(itemStack);
        if (gunId == null) {
            gunId = SWarfareCompat.getGunTexture(itemStack);
            if (gunId == null) {
                return EMPTY;
            }
        }
        if (this.idTest.contains(gunId)) {
            String str2 = str.substring(0, str.length() - 1) + "$" + gunId;
            if (this.nameTest.contains(str2)) {
                return str2;
            }
            return EMPTY;
        }
        return EMPTY;
    }
}
