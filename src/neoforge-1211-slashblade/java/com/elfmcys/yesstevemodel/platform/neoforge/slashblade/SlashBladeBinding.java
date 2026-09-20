package com.elfmcys.yesstevemodel.platform.neoforge.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;

/**
 * slashblade_animation molang 绑定（1.20.1 SlashBladeBinding 直译，单方法不折分支）。
 */
public final class SlashBladeBinding {

	private SlashBladeBinding() {
	}

	public static void registerFunctions(CtrlBinding binding) {
		binding.livingEntityVar("slashblade_animation", SlashBladeStateHelper::getSlashBladeAnimationFromContext);
	}
}
