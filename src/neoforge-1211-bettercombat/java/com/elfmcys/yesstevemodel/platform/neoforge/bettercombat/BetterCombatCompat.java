package com.elfmcys.yesstevemodel.platform.neoforge.bettercombat;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.neoforged.fml.ModList;

/**
 * neoforge 1.21.1 线 BetterCombat 真 compat（compat-bettercombat-1211 卡，
 * compat-slashblade-1211/e282543 与 compat-curios-1211/de67a25 同构先例）。
 * 异包门面孪生：RAW 源集整体 exclude rip/ysm/compat/**，shim 侧按相对路径
 * bettercombat/BetterCombatCompat.java 剔除 → 消费面 3 文件 import 走
 * stonecutter 行条件交换。
 *
 * API 分级（javap 三方比对：libs/bettercombat-forge-1.9.0+1.20.1.jar vs
 * bettercombat-neoforge-2.4.0+1.21.1.jar，sha1 bacbc8c8）：
 * - 零改动：BetterCombatClientEvents.ATTACK_START（Publisher&lt;PlayerAttackStart&gt;
 *   同形态）、PlayerAttackStart.onPlayerAttackStart(LocalPlayer, AttackHand) 同签名、
 *   AttackHand.isOffHand() 同签名（两代同为 record，逐字段同形）、
 *   AttackAnimationSubStack.base（ModifierLayer，两代逐字段同形）；
 * - 唯一环境差：1.20.1 的 LoadingModList.get().getModFileById 探测改
 *   ModList.get().isLoaded（neoforge 线统一口径，curios/slashblade 卡同）；
 * - attackAnimation 字段（bettercombat mixin 注入 AbstractClientPlayer）仍走
 *   VarHandleHelper 反射探测，缺失即不挂钩（mod-absent 同路）。
 *
 * 【惰性 init】neoforge 线无 ForgeClientSetupHooks 挂点，双检锁先例
 * （slashblade/curios 卡）。mod-absent 降级：bcombat_attack_animation molang
 * NOOP（零 NCDFE——bettercombat 类仅方法体引用，JVM 惰性解析）。
 */
public final class BetterCombatCompat {

	private BetterCombatCompat() {
	}

	private static final String MOD_ID = "bettercombat";

	private static volatile boolean inited;

	private static boolean IS_LOADED;

	/** 惰性 init（首个消费面调用触发一次；双检锁兜底，2128 卡B/curios/slashblade 卡先例）。 */
	private static void ensureInit() {
		if (inited) {
			return;
		}
		synchronized (BetterCombatCompat.class) {
			if (inited) {
				return;
			}
			IS_LOADED = ModList.get().isLoaded(MOD_ID);
			if (IS_LOADED) {
				BetterCombatBinding.initialize();
			}
			// 探测行（冒烟验收数值口径：带 bettercombat=isLoaded=true / 不带=false）
			YesSteveModel.LOGGER.info("BetterCombatCompat init(lazy): isLoaded={}{}",
					IS_LOADED, IS_LOADED ? " (bcombat attack hook + molang bindings active)"
							: " (bettercombat absent: bcombat_attack_animation noop)");
			inited = true;
		}
	}

	public static boolean isLoaded() {
		ensureInit();
		return IS_LOADED;
	}

	public static void registerBindings(CtrlBinding binding) {
		if (isLoaded()) {
			BetterCombatBinding.registerBindings(binding);
		} else {
			registerDummyBindings(binding);
		}
	}

	private static void registerDummyBindings(CtrlBinding binding) {
		binding.clientPlayerEntityVar("bcombat_attack_animation", interfaceC0807x6b368640 -> StringPool.EMPTY);
	}
}
