package com.elfmcys.yesstevemodel.platform.neoforge.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.ItemTagsConstants;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 状态获取助手（1.20.1 SlashBladeStateHelper 直译）。签名适形点：
 * Capability 体系删除（2.x 迁移文档 §2.2）→ BladeStateAccess.of(stack)（上游
 * BladeStateAccess.java:29，DataComponents 底座）；Unsafe SlashBladeStateAccess
 * 反射分支不移植（旧可变 POJO 已删，2.x 无 0.1.x 线，死代码）。
 */
public final class SlashBladeStateHelper {

	private SlashBladeStateHelper() {
	}

	public static boolean isSlashBlade(ItemStack itemStack) {
		return (itemStack.getItem() instanceof ItemSlashBlade) || ItemTagsConstants.SLASHBLADE.matches(itemStack);
	}

	public static String getSlashBladeAnimation(AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event) {
		LivingEntity livingEntity = event.getAnimatable().getEntity();
		return getComboState(livingEntity.getMainHandItem(), livingEntity);
	}

	public static String getSlashBladeAnimationFromContext(IContext<? extends LivingEntity> context) {
		LivingEntity livingEntity = context.entity();
		return getComboState(livingEntity.getMainHandItem(), livingEntity);
	}

	public static PlayState handleSlashBladeAnim(AnimationEvent<? extends AnimatableEntity<? extends LivingEntity>> event, String animation, ILoopType loopType) {
		String str2 = "slashblade:" + animation;
		if (event.getAnimatable().getAnimation(str2) != null) {
			return setAnimAndContinue(event, str2, loopType);
		}
		return setAnimAndContinue(event, animation, loopType);
	}

	@NotNull
	private static String getComboState(ItemStack itemStack, LivingEntity livingEntity) {
		if (!SlashBladeCompat.isSlashBladeItem(itemStack)) {
			return StringPool.EMPTY;
		}
		return BladeStateAccess.of(itemStack).map(iSlashBladeState -> {
			long gameTime = (livingEntity.level().getGameTime() - iSlashBladeState.getLastActionTime()) * 50;
			return SlashBladeComboHelper.getComboState(iSlashBladeState, gameTime, livingEntity);
		}).orElse(StringPool.EMPTY);
	}

	@NotNull
	private static PlayState setAnimAndContinue(AnimationEvent<?> event, String animation, ILoopType loopType) {
		event.getController().setAnimation(animation, loopType);
		return PlayState.CONTINUE;
	}
}
