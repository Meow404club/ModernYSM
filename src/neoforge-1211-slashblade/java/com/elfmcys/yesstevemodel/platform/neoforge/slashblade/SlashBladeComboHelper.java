package com.elfmcys.yesstevemodel.platform.neoforge.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.google.common.collect.Maps;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * combo 取值助手（1.20.1 SlashBladeComboHelper 直译）。签名适形点：
 * 2.x REGISTRY 是 neoforge RegistryBuilder 产物（上游 ComboStateRegistry.java:36），
 * REGISTRY.get(RL) 直取（上游同款用法 ISlashBladeState.java:434），
 * 删 IForgeRegistry.getValue 包装；hasNewApi 分支删除（2.x 恒 new-API）。
 *
 * 别名表（风险点①核对结论）：2.x 注册名=slashblade:combo_a4_ex
 * （ComboStateRegistry.java:156），与 1.20.1 动画面 combo_a4ex 一致性由
 * 1.20.1 别名表同向映射保留；上游还发现 combo_a5ex 命名（:178），同法补录。
 */
public final class SlashBladeComboHelper {

	private SlashBladeComboHelper() {
	}

	private static final Map<String, String> comboNameAliases = Maps.newHashMap();

	static {
		comboNameAliases.put("slashblade:combo_a4_ex", "slashblade:combo_a4ex");
		comboNameAliases.put("slashblade:combo_a5ex", "slashblade:combo_a5ex");
	}

	public static String getComboState(ISlashBladeState slashBladeState, long j, LivingEntity livingEntity) {
		ResourceLocation comboSeq = slashBladeState.getComboSeq();
		ComboState comboState = ComboStateRegistry.REGISTRY.get(comboSeq);
		if (comboState == null) {
			return StringPool.EMPTY;
		}
		int timeoutMS = comboState.getTimeoutMS();
		if ("slashblade:standby".equals(comboSeq.toString())) {
			timeoutMS -= 553;
		}
		if (j <= timeoutMS) {
			String comboName = normalizeComboName(comboSeq.toString());
			if ("slashblade:judgement_cut".equals(comboName) && !livingEntity.onGround()) {
				comboName = "slashblade:judgement_cut_slash_air";
			}
			if ("slashblade:judgement_cut_slash_just2".equals(comboName) && !livingEntity.onGround()) {
				comboName = "slashblade:judgement_cut_slash_air_just2";
			}
			return comboName;
		}
		return StringPool.EMPTY;
	}

	private static String normalizeComboName(String str) {
		return comboNameAliases.getOrDefault(str, str);
	}
}
