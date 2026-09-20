package com.elfmcys.yesstevemodel.platform.neoforge.curios;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm.curios.HasAnyCurios;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm.curios.HasAnyCuriosWithAllTags;
import com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm.curios.HasAnyCuriosWithAnyTag;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ContextBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import com.elfmcys.yesstevemodel.util.YsmTag;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * neoforge 1.21.1 线 Curios 真 compat（compat-curios-1211 卡，研究卡
 * tasks.research-mod-compat-matrix P0 首批）。异包门面孪生（2128 卡先例：
 * RAW 源集已整体 exclude rip/ysm/compat/**，shim 侧按相对路径 curios/CuriosCompat.java
 * 剔除 → 消费面 4 文件 import 走 stonecutter 条件交换（261/2128 卡先例））。
 *
 * 逻辑=1.20.1-forge 真实现（client/compat/curios/platform/forge/CuriosCompat +
 * CuriosBinding 合并单文件）直译。API 漂移逐符号 javap 实证
 * （libs/curios-forge-5.7.0+1.20.1 vs Modrinth vvuO3ImH curios-neoforge-9.5.1+1.21.1-api）：
 * 唯一断裂=CuriosApi.getCuriosInventory 返回 LazyOptional（forge）→ Optional（neoforge）；
 * 链式 .map().flatMap() 语义同构直通。getStacksHandler/getCurios/getStacks/
 * getCosmeticStacks/getRenders/hasCosmetic/IDynamicStackHandler.getStackInSlot
 * 两代同签名（javap 实证），CuriosBinding 原样搬运。
 *
 * 【惰性 init】neoforge 线无 ForgeClientSetupHooks（1.20.1 的 CuriosCompat.init
 * 挂点），沿 fpm-card-b-2128 卡B 惰性 init 先例（渲染线程单线程语义+双检锁），
 * 不引入 FMLClientSetupEvent SetupHook。
 *
 * mod-absent 降级：ModList 探测 false 时 molang 函数注册 NOOP（=1.20.1
 * registerDummyBindings 语义），探测/查询行打 log 一次（冒烟验收数值口径：
 * 带 curios=isLoaded=true / 不带=false，2128 卡探测行先例）。
 * FirstPersonCompat 符号（CuriosApi/ICuriosItemHandler 等）仅方法体引用，
 * JVM 惰性解析：curios 缺席时 dummy 分支不触 curios 类，零 NCDFE。
 */
public final class CuriosCompat {

	private CuriosCompat() {
	}

	private static final String MOD_ID = "curios";

	private static volatile boolean inited;

	private static boolean IS_LOADED;

	/** 惰性 init（首个消费面调用触发一次；渲染线程语义+双检锁兜底，2128 卡B 先例）。 */
	private static void ensureInit() {
		if (inited) {
			return;
		}
		synchronized (CuriosCompat.class) {
			if (inited) {
				return;
			}
			IS_LOADED = ModList.get().isLoaded(MOD_ID);
			// 探测行（冒烟验收数值口径：带 curios=isLoaded=true / 不带=false）
			YesSteveModel.LOGGER.info("CuriosCompat init(lazy): isLoaded={}{}",
					IS_LOADED, IS_LOADED ? " (molang curios bindings active)"
							: " (curios absent: molang curios functions noop)");
			inited = true;
		}
	}

	public static boolean isLoaded() {
		ensureInit();
		return IS_LOADED;
	}

	public static boolean hasItemInSlot(LivingEntity livingEntity, String str, ReferenceOpenHashSet<Item> set) {
		return IS_LOADED && CuriosApi.getCuriosInventory(livingEntity).flatMap(handler -> handler.getStacksHandler(str)).map(handler -> CuriosBinding.findInSlot(handler, itemStack -> set.isEmpty() || set.contains(itemStack.getItem())) != null).orElse(false);
	}

	public static boolean hasTaggedItemInSlot(LivingEntity livingEntity, String str, List<YsmTag.ItemTag> list) {
		return IS_LOADED && CuriosApi.getCuriosInventory(livingEntity).flatMap(handler -> handler.getStacksHandler(str)).map(handler -> CuriosBinding.findInSlot(handler, itemStack -> {
			for (YsmTag.ItemTag itemTag : list) {
				if (itemTag.matches(itemStack)) {
					return true;
				}
			}
			return false;
		}) != null).orElse(false);
	}

	public static boolean hasNoTaggedItemInSlot(LivingEntity entity, String str, List<YsmTag.ItemTag> list) {
		return IS_LOADED && CuriosApi.getCuriosInventory(entity).flatMap(handler -> handler.getStacksHandler(str)).map(handler -> CuriosBinding.findInSlot(handler, itemStack -> {
			for (YsmTag.ItemTag itemTag : list) {
				if (!itemTag.matches(itemStack)) {
					return false;
				}
			}
			return true;
		}) != null).orElse(false);
	}

	public static void registerCuriosItems(ContextBinding binding) {
		if (isLoaded()) {
			CuriosBinding.registerBindings(binding);
		} else {
			registerDummyBindings(binding);
		}
	}

	private static void registerDummyBindings(ContextBinding binding) {
		binding.function("has_any_curios", Function.NOOP);
		binding.function("has_any_curios_with_all_tags", Function.NOOP);
		binding.function("has_any_curios_with_any_tag", Function.NOOP);
		binding.livingEntityVar("dump_curios", context -> {
			context.logWarning("Curios not installed.");
			return null;
		});
	}

	private static final class CuriosBinding {

		static ItemStack findFirstMatching(ICuriosItemHandler handler, Predicate<ItemStack> predicate) {
			for (ICurioStacksHandler iCurioStacksHandler : handler.getCurios().values()) {
				ItemStack found = findInSlot(iCurioStacksHandler, predicate);
				if (found != null) {
					return found;
				}
			}
			return null;
		}

		static ItemStack findInSlot(ICurioStacksHandler handler, Predicate<ItemStack> predicate) {
			ItemStack stackInSlot;
			NonNullList<Boolean> renders = handler.getRenders();
			IDynamicStackHandler cosmeticStacks = handler.hasCosmetic() ? handler.getCosmeticStacks() : null;
			IDynamicStackHandler stacks = handler.getStacks();
			for (int i = 0; i < stacks.getSlots() && i < renders.size(); i++) {
				if (renders.get(i)) {
					if (cosmeticStacks != null && (stackInSlot = cosmeticStacks.getStackInSlot(i)) != null && !stackInSlot.isEmpty()) {
						if (predicate.test(stackInSlot)) {
							return stackInSlot;
						}
					} else {
						ItemStack stackInSlot2 = stacks.getStackInSlot(i);
						if (!stackInSlot2.isEmpty() && predicate.test(stackInSlot2)) {
							return stackInSlot2;
						}
					}
				}
			}
			return null;
		}

		static void registerBindings(ContextBinding binding) {
			binding.function("has_any_curios", new HasAnyCurios());
			binding.function("has_any_curios_with_all_tags", new HasAnyCuriosWithAllTags());
			binding.function("has_any_curios_with_any_tag", new HasAnyCuriosWithAnyTag());
			binding.livingEntityVar("dump_curios", CuriosBinding::dumpCurios);
		}

		private static Object dumpCurios(com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext<? extends LivingEntity> context) {
			if (!context.isDebugMode()) {
				return null;
			}
			CuriosApi.getCuriosInventory(context.entity()).ifPresent(handler -> {
				for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
					context.logWarningComponent(Component.literal("-------- Type ").append(ComponentUtils.copyOnClickText(entry.getKey())).append(" --------"));
					context.logWarning(StringPool.EMPTY);
					findInSlot(entry.getValue(), stack -> {
						context.logWarningComponent(Component.literal("Display ").append(ComponentUtils.copyOnClickText(stack.getHoverName().getString(99))));
						Holder<Item> itemHolder = stack.getItemHolder();
						itemHolder.unwrapKey().ifPresent(resourceKey -> {
							context.logWarningComponent(Component.literal("Name ").append(ComponentUtils.copyOnClickText(resourceKey.location().toString())));
						});
						itemHolder.tags().forEach(tagKey -> {
							context.logWarningComponent(Component.literal("Tag ").append(ComponentUtils.copyOnClickText(tagKey.location().toString())));
						});
						context.logWarning(StringPool.EMPTY);
						return false;
					});
				}
			});
			return null;
		}
	}
}
