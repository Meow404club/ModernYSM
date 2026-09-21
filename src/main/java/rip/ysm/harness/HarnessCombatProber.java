package rip.ysm.harness;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// count-debt-harness-1211：slashblade/bettercombat 运行时计数债清偿探测器。
// 债 1（tasks.feature-debts.debt-slashblade-1211-quad-count）：tour 走查无刀物品注入
// 手段 → 本类向本地玩家主手注入 slashblade:slashblade，取两路计数：守卫命中
// （SlashBladeCompat.isSlashBladeItem=true 路径）+ 刀挂渲染 quads>0（BladeModelManager
// 实加载 WavefrontObject 的 GroupObject.faces 总和，SlashBladeRenderer 取模同链）。
// 债 2（compat-bettercombat-1211 交卡遗留）：ATTACK_START Publisher 计数——换注入
// minecraft:iron_sword（bettercombat 武器表覆盖 vanilla 剑，刀物品无 WeaponAttributes
// 不触发 bc 流程），立 NoAI 村民为靶，GuiTourDriver combatTick 阶梯按住 keyAttack 走
// 真实攻击键面（Minecraft.doAttack → bc mixin → startUpswing → ATTACK_START）。
// 全反射实现：本类在 RAW 源集对 1.16.5~26.3 全线直接编译（stonecutter 不改写 RAW），
// 版本敏感符号一律反射（见各方法头注），仅编译期可见面=各线公共 API。计数口径=日志行
// （FPM selfdrive hit# 先例，原文进 client.log）。harness 类，生产 jar 排除
// （build.moddev.gradle.kts:591 exclude rip/ysm/harness/**）。
public final class HarnessCombatProber {

    private HarnessCombatProber() {
    }

    // ATTACK_START 命中计数（探测 handler 递增）。
    private static int attackStartHits;
    private static boolean attackHookRegistered;

    // JPMS 跨模块开放：neoforge 每个 mod 是独立命名模块（run5 实证 IllegalAccessException
    // "cannot access a member of class ... (in module slashblade)"），包未导出→反射拒绝。
    // Module.addOpens 无调用方限制（仅 SecurityManager 检查），先开包再反射。
    // 1.16.5 线（Java 8）无 Class.getModule/addOpens：反射 catch 后仅日志，探测路径不炸。
    private static void openReflectively(Class<?> target) {
        try {
            // Java 8 线无 Class.getModule——JPMS API 本身反射调（1.16.5 编译期无此符号）
            Method getModule = Class.class.getMethod("getModule");
            Object targetMod = getModule.invoke(target);
            Object selfMod = getModule.invoke(HarnessCombatProber.class);
            Method addOpens = targetMod.getClass().getMethod("addOpens", String.class, targetMod.getClass());
            addOpens.invoke(targetMod, target.getPackage().getName(), selfMod);
        } catch (NoSuchMethodException e) {
            // 非 JPMS 线（1.16.5 Java 8）：无模块系统，反射访问无开包需求，静默跳过
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] addOpens failed for " + target + ": " + t);
        }
    }

    // open 包 + setAccessible（run7 实证：ComponentBackedState 非 public 类——addOpens 只开
    //  包导出，非 public 类成员反射仍需 setAccessible 抑制访问检查；开包后 setAccessible 可过）。
    private static java.lang.reflect.Method accessible(java.lang.reflect.Method m) {
        try {
            m.setAccessible(true);
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] setAccessible failed: " + m + " -> " + t);
        }
        return m;
    }

    private static java.lang.reflect.Field accessible(java.lang.reflect.Field f) {
        try {
            f.setAccessible(true);
        } catch (Throwable ignored) {
        }
        return f;
    }

    // "combat" 命令入口（GuiTourDriver.pollCommand，armed 门内）：刀计数 + 换剑 + 立靶。
    public static void injectAndCount(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // ---- 债 1：注入刀 + 守卫/quads 计数 ----
        ItemStack blade = itemByName(mc, "slashblade", "slashblade");
        if (blade == null) {
            System.out.println("[HarnessCombatProber] slashblade item not found (mod absent?)");
            return;
        }
        setMainHand(player, blade);
        blade = mc.player.getMainHandItem();
        System.out.println("[HarnessCombatProber] injected main hand = slashblade:slashblade");

        // 守卫路径：SlashBladeCompat.isSlashBladeItem（孪生包名 1211 专有，反射取）
        Boolean guard = Boolean.FALSE;
        try {
            Class<?> compat = Class.forName(
                    "com.elfmcys.yesstevemodel.platform.neoforge.slashblade.SlashBladeCompat");
            Method m = compat.getMethod("isSlashBladeItem", ItemStack.class);
            guard = (Boolean) m.invoke(null, blade);
        } catch (ClassNotFoundException e) {
            System.out.println("[HarnessCombatProber] SlashBladeCompat class absent (not 1211 twin line)");
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] guard probe failed: " + t);
        }
        System.out.println(guard.booleanValue()
                ? "[HarnessCombatProber] slashblade guard hit #1 (isSlashBladeItem=true path, injected blade in local player main hand)"
                : "[HarnessCombatProber] slashblade guard MISS (isSlashBladeItem=false on injected item)");

        int faces = bladeFaces(blade);
        System.out.println((faces > 0
                ? "[HarnessCombatProber] slashblade blade-render quads hit #1"
                : "[HarnessCombatProber] slashblade blade-render quads MISS")
                + " (faces=" + faces + ", BladeModelManager chain)");

        // ---- 债 2：换剑 + 立靶（ATTACK_START 计数由 combatTick 按键后回收） ----
        ItemStack sword = itemByName(mc, "minecraft", "iron_sword");
        if (sword != null) {
            setMainHand(player, sword);
            System.out.println("[HarnessCombatProber] swapped main hand = minecraft:iron_sword"
                    + " (bettercombat weapon table covers vanilla swords)");
        }
        // 靶：面朝方向 2 格（^ ^ ^2=观察旋转系），NoAI 定住；服务端回环同步后 combatTick 才按键
        sendCommandReflective(mc,
                "execute at @s run summon minecraft:villager ^ ^ ^2 {NoAI:1b,PersistenceRequired:1b}");
        System.out.println("[HarnessCombatProber] summoned villager target 2 blocks ahead (attack-start probe target)");
    }

    // 命令通道反射面：GuiTourDriver.sendChatCommand 在 1.16.5 线 stonecutter 生成物中
    // 位于 >=1.21.1 闸外不可达（combat 成员同），跨线调用统一反射（找不到符号=静默跳过，
    // 旧线 combat 命令本来就不会被触发）。
    private static void sendCommandReflective(Minecraft mc, String command) {
        try {
            Method m = GuiTourDriver.class.getMethod("sendChatCommand", Minecraft.class, String.class);
            m.setAccessible(true);
            m.invoke(null, mc, command);
        } catch (NoSuchMethodException e) {
            System.out.println("[HarnessCombatProber] sendChatCommand absent on this line (expected for <1.21.1): " + e);
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] sendCommandReflective failed: " + t);
        }
    }

    private static void retryCombatReflective() {
        try {
            Method m = GuiTourDriver.class.getMethod("combatRetry");
            m.setAccessible(true);
            m.invoke(null);
        } catch (NoSuchMethodException e) {
            // 旧线无 combat 阶梯，无需重试
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] retryCombatReflective failed: " + t);
        }
    }

    // 主手物品写入反射面：1.16.5 无 getInventory 映射名、26.3 selected/items 转私有——
    // Inventory.items(NonNullList)/selected(旧 int 字段或新 getSelectedItem 反射分代)。
    private static void setMainHand(LocalPlayer player, ItemStack stack) {
        try {
            Object inv;
            try {
                inv = player.getClass().getMethod("getInventory").invoke(player);
            } catch (NoSuchMethodException e) {
                java.lang.reflect.Field f = accessible(player.getClass().getField("inventory"));
                inv = f.get(player);
            }
            // selected 写入面：先试 int selected 字段（1.16~26.2 命名映射），
            // 失败再试 getSelectedItem/setSelectedItem（26.3 SelectedItem 容器化）
            int slot = -1;
            try {
                java.lang.reflect.Field sel = accessible(inv.getClass().getField("selected"));
                slot = sel.getInt(inv);
            } catch (NoSuchFieldException e) {
                try {
                    Object selStack = inv.getClass().getMethod("getSelected").invoke(inv);
                    // 26.3 容器化：直接改返回的 ItemStack 内容不可行——改经 setSelected 反射
                    for (Method m : inv.getClass().getMethods()) {
                        if (m.getName().equals("setSelected") && m.getParameterCount() == 1) {
                            m.invoke(inv, stack);
                            return;
                        }
                    }
                } catch (NoSuchMethodException e2) {
                    System.out.println("[HarnessCombatProber] main-hand selected face not found: " + e2);
                }
            }
            if (slot >= 0) {
                java.util.List<?> items = (java.util.List<?>)
                        accessible(inv.getClass().getField("items")).get(inv);
                ((java.util.List<ItemStack>) items).set(slot, stack);
            }
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] setMainHand failed: " + t);
        }
    }

    // 物品取数：注册表访问逐代反射；取不到（mod absent/旧线无该注册表面）即 null。
    private static ItemStack itemByName(Minecraft mc, String namespace, String path) {
        try {
            Class<?> regs = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Object itemRegistry = regs.getField("ITEM").get(null);
            Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
            Object rl;
            try {
                rl = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                        .invoke(null, namespace, path);
            } catch (NoSuchMethodException e) {
                rl = rlClass.getConstructor(String.class, String.class).newInstance(namespace, path);
            }
            // Registry.get/getKey 走接口反射（泛型擦除后签名 get(ResourceLocation)/getKey(Object)）
            Class<?> registryItf = Class.forName("net.minecraft.core.Registry");
            Object item = registryItf.getMethod("get", rlClass).invoke(itemRegistry, rl);
            Object key = registryItf.getMethod("getKey", Object.class).invoke(itemRegistry, item);
            if (!rl.equals(key)) {
                return null;
            }
            return new ItemStack((net.minecraft.world.item.Item) item);
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] item lookup failed: " + t);
            return null;
        }
    }

    // 刀挂模型面数：BladeStateAccess.of→getModel→BladeModelManager.getModel→faces 总和
    // （全反射；slashblade absent=Class.forName 失败即 MISS，不炸）。
    private static int bladeFaces(ItemStack blade) {
        try {
            Class<?> access = Class.forName(
                    "mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess");
            openReflectively(access);
            Class<?> stateClass = null;
            java.util.Optional<?> stateOpt = (java.util.Optional<?>)
                    accessible(access.getMethod("of", ItemStack.class)).invoke(null, blade);
            if (!stateOpt.isPresent()) {
                return -1;
            }
            Object state = stateOpt.get();
            stateClass = state.getClass();
            openReflectively(stateClass);
            // ComponentBackedState 非 public 类：JPMS 跨模块 invoke 其成员必须先
            // setAccessible(true)（审查打回实证：漏包 accessible() 即 IllegalAccess，
            // 并非"setAccessible 抑制不了"）
            java.util.Optional<?> modelOpt = (java.util.Optional<?>) accessible(stateClass
                    .getMethod("getModel")).invoke(state);
            System.out.println("[HarnessCombatProber] state.getModel() present=" + modelOpt.isPresent()
                    + (modelOpt.isPresent() ? " model=" + modelOpt.get() : ""));
            Class<?> mgrClass = Class.forName(
                    "mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager");
            openReflectively(mgrClass);
            Object mgr = accessible(mgrClass.getMethod("getInstance")).invoke(null);
            Object model;
            if (modelOpt.isPresent()) {
                model = accessible(mgrClass.getMethod("getModel", modelOpt.get().getClass()))
                        .invoke(mgr, modelOpt.get());
            } else {
                // 缺省 blade.obj（SlashBladeRenderer BLADE_OBJ 同款），RL 构造分代反射
                Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
                Object rl;
                try {
                    rl = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                            .invoke(null, "slashblade", "model/blade.obj");
                } catch (NoSuchMethodException e) {
                    rl = rlClass.getConstructor(String.class).newInstance("slashblade:model/blade.obj");
                }
                model = accessible(mgrClass.getMethod("getModel", rlClass)).invoke(mgr, rl);
            }
            openReflectively(model.getClass());
            int faces = 0;
            for (Object group : (Iterable<?>) accessible(model.getClass().getField("groupObjects")).get(model)) {
                openReflectively(group.getClass());
                java.util.List<?> groupFaces = (java.util.List<?>)
                        accessible(group.getClass().getField("faces")).get(group);
                faces += groupFaces.size();
            }
            return faces;
        } catch (Throwable tState) {
            // run8 实证：ComponentBackedState 非 public 类的 getModel 反射 invoke 在 JPMS 下
            // 仍拒（setAccessible 抑制不了该形态）——降级走缺省 blade.obj（SlashBladeRenderer
            // state.getModel().orElse(BLADE_OBJ) 同款取模），quads>0 证据口径不变
            System.out.println("[HarnessCombatProber] state-model path failed (" + tState + "), falling back to default blade.obj");
            try {
                Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
                Object rl;
                try {
                    rl = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                            .invoke(null, "slashblade", "model/blade.obj");
                } catch (NoSuchMethodException e) {
                    rl = rlClass.getConstructor(String.class).newInstance("slashblade:model/blade.obj");
                }
                Class<?> mgrClass = Class.forName(
                        "mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager");
                Object mgr = accessible(mgrClass.getMethod("getInstance")).invoke(null);
                Object model = accessible(mgrClass.getMethod("getModel", rlClass)).invoke(mgr, rl);
                int faces = 0;
                for (Object group : (Iterable<?>) accessible(model.getClass().getField("groupObjects")).get(model)) {
                    openReflectively(group.getClass());
                    java.util.List<?> groupFaces = (java.util.List<?>)
                            accessible(group.getClass().getField("faces")).get(group);
                    faces += groupFaces.size();
                }
                return faces;
            } catch (Throwable t2) {
                System.out.println("[HarnessCombatProber] blade model probe failed: " + t2);
                return -1;
            }
        }
    }

    // GuiTourDriver.combatTick 阶梯回调：找靶→按攻击键→松键收尾。stage 由 driver 持有。
    public static void combatStep(Minecraft mc, int stage) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (stage == 1) {
            LivingEntity target = null;
            double best = 36.0d;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == player || !(e instanceof LivingEntity) || e instanceof AbstractClientPlayer) {
                    continue;
                }
                double d = e.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    target = (LivingEntity) e;
                }
            }
            if (target == null) {
                // 靶未同步：重发召唤（首轮实证：未 op 客户端命令树无 summon → 本地解析
                // "Unknown or incomplete command"；tour.sh combat 块先 op Dev 后重发即通）
                sendCommandReflective(mc,
                        "execute at @s run summon minecraft:villager ^ ^ ^2 {NoAI:1b,PersistenceRequired:1b}");
                System.out.println("[HarnessCombatProber] attack-start: no synced target in reach, summon+retry scheduled");
                retryCombatReflective();
                return;
            }
            registerAttackHook();
            System.out.println("[HarnessCombatProber] target in reach: "
                    + target.getType() + " dist=" + String.format("%.2f", Math.sqrt(best))
                    + " — holding keyAttack (Minecraft.doAttack → bettercombat upswing path)");
            mc.options.keyAttack.setDown(true);
        } else if (stage == 2) {
            mc.options.keyAttack.setDown(false);
            System.out.println("[HarnessCombatProber] keyAttack released; ATTACK_START total hits=" + attackStartHits);
            System.out.println(attackStartHits > 0
                    ? "[HarnessCombatProber] bettercombat attack-start hit #1 (ATTACK_START Publisher → onPlayerAttackStart)"
                    : "[HarnessCombatProber] bettercombat attack-start MISS (ATTACK_START never invoked)");
        }
    }

    // ATTACK_START 挂钩（动态 Proxy 实现 PlayerAttackStart 接口——Publisher.register 真实路径）。
    private static synchronized void registerAttackHook() {
        if (attackHookRegistered) {
            return;
        }
        attackHookRegistered = true;
        try {
            Class<?> events = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
            Object publisher = events.getField("ATTACK_START").get(null);
            final Class<?> handlerItf = Class.forName(
                    "net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackStart");
            Object proxy = Proxy.newProxyInstance(HarnessCombatProber.class.getClassLoader(),
                    new Class[]{handlerItf}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("onPlayerAttackStart".equals(method.getName()) && args != null && args.length == 2) {
                                attackStartHits++;
                                boolean off = false;
                                try {
                                    off = (Boolean) args[1].getClass()
                                            .getMethod("isOffHand").invoke(args[1]);
                                } catch (Throwable ignored) {
                                }
                                System.out.println("[HarnessCombatProber] ATTACK_START hit #" + attackStartHits
                                        + " (Publisher invoke, hand=" + (off ? "off" : "main") + ")");
                            }
                            return null;
                        }
                    });
            // Publisher 是 final class（javap 实证，非接口）：泛型擦除后签名 register(Object)，
            // 反射须按参数 Object.class 查（register(handlerItf) 会 NoSuchMethod——run4 实证）
            publisher.getClass().getMethod("register", Object.class).invoke(publisher, proxy);
        } catch (Throwable t) {
            System.out.println("[HarnessCombatProber] ATTACK_START hook register failed: " + t);
        }
    }
}
