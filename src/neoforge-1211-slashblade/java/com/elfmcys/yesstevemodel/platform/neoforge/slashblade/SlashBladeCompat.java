package com.elfmcys.yesstevemodel.platform.neoforge.slashblade;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * neoforge 1.21.1 线 SlashBlade 真 compat（compat-slashblade-1211 卡，研究卡
 * tasks.research-slashblade-1211）。异包门面孪生（compat-curios-1211/de67a25 先例：
 * RAW 源集整体 exclude rip/ysm/compat/**，shim 侧按相对路径 slashblade/*.java 剔除 →
 * 消费面 5 文件 import 走 stonecutter 行条件交换）。
 *
 * 逻辑=1.20.1-forge 真实现（client/compat/slashblade/platform/forge/*）直译，
 * 按 2.x 迁移文档分级：
 * - 零改动：isSlashBladeItem 守卫（ItemSlashBlade 仍在 item/ItemSlashBlade）、
 *   getComboSeq() 同签名（ISlashBladeState.java:180）、getComboAnimName/
 *   handleSlashBladeAnim 我方自有逻辑；
 * - 签名适形：ComboStateRegistry.REGISTRY 已是 neoforge RegistryBuilder 产物
 *  （ComboStateRegistry.java:36）→ REGISTRY.get(RL) 直取（上游同款 ISlashBladeState.java:434），
 *   删 IForgeRegistry.getValue 包装；渲染链 BladeModelManager/WavefrontObject/
 *   BladeRenderState 全同形态（上游源码逐文件核过）仅 import forge→neoforged；
 * - 重写=删除：Capability 体系整体删除（迁移文档 §2.2），状态入口
 *   BladeStateAccess.of(stack)→Optional&lt;ISlashBladeState&gt;（BladeStateAccess.java:29，
 *   底层 DataComponents）；1.20.1 的 SlashBladeStateAccess Unsafe 反射 hack
 *   （读旧可变 POJO comboSeq 字段）无靶，不移植——2.x 无 0.1.x 线，旧分支死代码。
 *
 * VersionRange 判定删除：!(v ∈ (,0.1.2]) 对 2.0.7 恒 true，单一 new-API 分支。
 *
 * 【惰性 init】neoforge 线无 ForgeClientSetupHooks 挂点（compat-curios-1211 同因），
 * 沿 fpm-card-b-2128 卡B 双检锁先例。mod-absent 降级：isSlashBladeItem 恒 false +
 * slashblade_animation molang NOOP（零 NCDFE——slashblade 类仅方法体引用，JVM 惰性解析）。
 */
public final class SlashBladeCompat {

	private SlashBladeCompat() {
	}

	private static final String MOD_ID = "slashblade";

	private static volatile boolean inited;

	private static boolean IS_LOADED;

	/** 惰性 init（首个消费面调用触发一次；双检锁兜底，2128 卡B/curios 卡先例）。 */
	private static void ensureInit() {
		if (inited) {
			return;
		}
		synchronized (SlashBladeCompat.class) {
			if (inited) {
				return;
			}
			IS_LOADED = ModList.get().isLoaded(MOD_ID);
			// 探测行（冒烟验收数值口径：带 slashblade=isLoaded=true / 不带=false）
			YesSteveModel.LOGGER.info("SlashBladeCompat init(lazy): isLoaded={}{}",
					IS_LOADED, IS_LOADED ? " (slashblade combo/blade-render active)"
							: " (slashblade absent: guard false + slashblade_animation noop)");
			inited = true;
		}
	}

	public static boolean isLoaded() {
		ensureInit();
		return IS_LOADED;
	}

	public static boolean isSlashBladeItem(ItemStack itemStack) {
		return isLoaded() && SlashBladeStateHelper.isSlashBlade(itemStack);
	}

	public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
		if (isLoaded()) {
			return SlashBladeStateHelper.getSlashBladeAnimation(event);
		}
		return StringPool.EMPTY;
	}

	public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
		if (isLoaded() && isSlashBladeItem(livingEntity.getMainHandItem())) {
			return SlashBladeStateHelper.handleSlashBladeAnim(event, str, loopType);
		}
		return null;
	}

	public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
		if (isLoaded()) {
			ctrlBinding.livingEntityVar("slashblade_animation", SlashBladeStateHelper::getSlashBladeAnimationFromContext);
		} else {
			ctrlBinding.livingEntityVar("slashblade_animation", it -> StringPool.EMPTY);
		}
	}
}
