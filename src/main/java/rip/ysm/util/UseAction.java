package rip.ysm.util;

//? if >=1.13
import net.minecraft.world.item.ItemStack;
//? if <1.13
/*import net.minecraft.item.ItemStack;*/

/**
 * UseAnim 泛型 API 收编门面（Rl/isValid 同模式）。
 *
 * 1.21.2 把 {@code net.minecraft.world.item.UseAnim} 改名为
 * {@code net.minecraft.world.item.ItemUseAnimation}（vanilla-1.21.3 sources 实证：
 * ItemUseAnimation.java 常量 NONE/EAT/DRINK/BLOCK/BOW/SPEAR/CROSSBOW/SPYGLASS/TOOT_HORN/BRUSH
 * 与旧 UseAnim 逐一同名同序），ItemStack.getUseAnimation() 返回类型同步换代。
 *
 * 本门面用 {@code Enum#name()} 做跨代映射：getUseAnimation() 在任一代返回该代的枚举类型，
 * .name() 是 Enum 实例方法、静态类型无关——同一行代码在两代都编译且语义一致，零条件块。
 * 消费方（ConditionUse/Hold/Swing、EquipmentUtil、QueryBinding）全部改为引用本枚举。
 */
public enum UseAction {
    NONE,
    EAT,
    DRINK,
    BLOCK,
    BOW,
    SPEAR,
    CROSSBOW,
    SPYGLASS,
    TOOT_HORN,
    BRUSH;

    /** 从 ItemStack 取使用动作（跨代映射到本枚举）。
     * 1.12.2：ItemStack.getItemUseAction() → net.minecraft.item.EnumAction（compile jar
     * recompiled_minecraft-1.12.2.jar javap 实证），常量 NONE/EAT/DRINK/BLOCK/BOW
     * 全在本枚举（1.12.2 无三叉戟故无 SPEAR）；1.13+ 改名 getUseAnimation()。
     * name() 跨代映射不变。 */
    public static UseAction of(ItemStack stack) {
        //? if <1.13
        /*return UseAction.valueOf(stack.getItemUseAction().name());*/
        //? if >=1.13
        return UseAction.valueOf(stack.getUseAnimation().name());
    }
}
