// 1.16.5 版本独有 shim（m2-gate-compat）：逻辑与共享源 gun/common/ItemUseAnimationPredicate
// 一致（该类不直接触碰第三方 API，只依赖 Tac/SWarfare 门面——本包内的 shim 返回
// mod-absent 默认值，故 predicate 恒走 PlayState.STOP，与守卫链缺席行为一致）。
package rip.ysm.compat.gun.common;

import com.elfmcys.yesstevemodel.client.animation.IAnimationPredicate;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import rip.ysm.compat.gun.tacz.TacCompat;

import java.util.Objects;

public class ItemUseAnimationPredicate implements IAnimationPredicate<LivingAnimatable<?>> {
    @Override
    public PlayState predicate(AnimationEvent<LivingAnimatable<?>> event, ExpressionEvaluator<?> evaluator) {
        LivingEntity livingEntity = (LivingEntity) ((LivingAnimatable) event.getAnimatable()).getEntity();
        if (livingEntity == null || (event.getAnimatable() instanceof IPreviewAnimatable)) {
            return PlayState.STOP;
        }
        if (!livingEntity.swinging && !livingEntity.isUsingItem()) {
            ItemStack itemInHand = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
            PlayState playState = TacCompat.handleGunActionAnimState(itemInHand, event);
            if (playState == null) {
                playState = SWarfareCompat.handleGunActionAnim(itemInHand, event);
            }
            return Objects.requireNonNullElse(playState, PlayState.STOP);
        }
        return PlayState.STOP;
    }

    public static boolean isLoaded() {
        return TacCompat.isLoaded() || SWarfareCompat.isLoaded();
    }
}
