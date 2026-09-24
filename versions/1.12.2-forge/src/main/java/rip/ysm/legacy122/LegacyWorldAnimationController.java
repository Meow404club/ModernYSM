package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import rip.ysm.util.UseAction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 1.12.2 世界渲染动画状态机（wave-d-anim1）。
 *
 * <p>主线对位：main 控制器 19 coded 状态优先级表（AnimationRegister.java:17-46 +
 * Priority.java:3-8 + AnimationManager.java:56-72 先匹配先赢）+ 三相转场
 * （AnimationControllerInstance：BEGINNING 淡入/RUNNING/ENDING 3.0f 淡出 :41；
 * main 控制器注册转场 0.1f → TicksInterpolator ×20 = 2 tick）。每个状态判定源
 * 逐条考古 vanilla-mc-1.12.2（行号锚见各谓词注释），每渲染帧从玩家实体公开字段
 * 求值当前状态，命中→按 {@link LegacyAnimationSampler} 既有合并序查同名动画→
 * 采样写 boneParams 12 float/骨契约。
 *
 * <p>语义镜像（逐条对照主线代码后落定）：
 * <ul>
 * <li>先匹配先赢：扁表按优先级桶序×桶内注册序扁平化，命中即停；模型缺该状态动画
 *     时【不】回落低优先级（AnimationManager.java:58-70 命中即 return +
 *     AnimationControllerInstance.setAnimation:104-107 查不到仅置 pin 不落 pending
 *     → 绑定位静像），缺动画淡出=处理器骨复位曲线（resetTickLength=3.0f
 *     AnimationData:18,24）→ 复用 ENDING 数学淡回绑定位。</li>
 * <li>同状态重复请求不重触发（setAnimation:99-101 pin 等值早退）；PLAY_ONCE 播完 →
 *     ENDING 淡回绑定位 → IDLE 后 pin 挡住不重播（clearAnimation:362-377 不清
 *     lastRequestedAnimation）——死亡等持续状态播一次后保持绑定位，状态切换才可重进。
 *     HOLD_ON_LAST_FRAME 钳 len 常驻 RUNNING 不进 ENDING（:160-162）。</li>
 * <li>状态消失（骑乘，AnimationManager.java:49-52 vehicle→STOP）：
 *     PredicateBasedController.process 的 STOP 分支=BEGINNING/RUNNING →
 *     beginEndingTransition 淡出+resetRequestedAnimation 清 pin，ENDING 续淡，
 *     IDLE 静像——逐相镜像。</li>
 * <li>BEGINNING 从上一帧姿态 lerp 进新动画，动画时间轴 BEGINNING 期间照常推进，
 *     进 RUNNING 不清轴（主线 :143-145 adjustedTick 减转场进度同构）；
 *     全局时钟永不回退（m2.6.1 教训）。</li>
 * </ul>
 *
 * <p>1.12.2 无法对位的状态（如实保留表内恒假，注释即考古结论）：
 * riptide=激流勇进 1.13+ 三叉戟（vanilla-mc-1.12.2 全文无 isAutoSpinAttack）；
 * climb/climbing=主线 Pose.SWIMMING 判据（1.13+ 泳姿，1.12.2 无 Pose 体系）。
 * swim 在 1.12.2 对位=水中离地且移幅过门（isSwimming 是 1.13+ 独立泳姿），
 * swim_stand 沿主线 :38 判据原样。
 *
 * <p>ponytail: fly 判定走 capabilities.isFlying——1.12.2 vanilla 不向观察者同步
 * PlayerCapabilities，远端玩家恒 false（主线对位=positionTracker 位置推断，本卡
 * 仅本地玩家精确，升级路径=按位置历史推断）；多玩家同模型共享 per-model params 面
 * （L3-1 既有布局），同模型多玩家动画取最后渲染者，升级路径=per-UUID params。
 * 每玩家时钟=渲染帧间 nanoTime 增量（渲染才推进，离屏暂停），钳 5 tick/帧防跳轴。
 */
final class LegacyWorldAnimationController {

    private static final Logger LOG = Logger.getLogger("yes_steve_model");

    private static final float MIN_SPEED = 0.05f;
    /** 主线 main 控制器注册转场 0.1f，TicksInterpolator ×20 = 2 tick。 */
    private static final float BEGIN_TICKS = 2.0f;
    /** AnimationControllerInstance.java:41 defaultTransitionTick = 3.0f。 */
    private static final float END_TICKS = 3.0f;
    /** 单帧最大推进 tick（卡顿钳制）。 */
    private static final float MAX_FRAME_TICKS = 5.0f;
    /** swing 控制器注册转场 0.0f（PlayerAnimationController.java:67）=进 RUNNING 无淡入。 */
    private static final float SWING_BEGIN_TICKS = 0.0f;
    /** use 控制器注册转场 0.1f（PlayerAnimationController.java:70）×20 = 2 tick（同 main）。 */
    private static final float USE_BEGIN_TICKS = BEGIN_TICKS;
    /** vehicle 控制器注册转场 0.1f（PlayerAnimationController.java:55）×20 = 2 tick（同 main）。 */
    private static final float VEHICLE_BEGIN_TICKS = BEGIN_TICKS;

    private static final int IDLE = 0;
    private static final int BEGINNING = 1;
    private static final int RUNNING = 2;
    private static final int ENDING = 3;

    /** 播放模式：状态注册固定 LOOP/PLAY_ONCE 两态（主线 :108 注册 loopType 覆盖动画自身值）。 */
    private static final int MODE_LOOP = 0;
    private static final int MODE_ONCE = 1;

    /** 状态谓词：p=玩家，lsa=按 partialTick 插值后的 limbSwingAmount。 */
    private interface StateTest {
        boolean test(EntityPlayer p, float lsa);
    }

    private static final class AnimState {
        final String name;
        final StateTest test;

        AnimState(String name, StateTest test) {
            this.name = name;
            this.test = test;
        }
    }

    /** 播放模式按主线注册逐行对位（AnimationRegister.java:18/:39 PLAY_ONCE，余 LOOP）。 */
    private static int modeOf(String name) {
        return "death".equals(name) || "attacked".equals(name) || "riptide".equals(name)
                ? MODE_ONCE : MODE_LOOP;
    }

    /**
     * 无动画数据的模型（LegacyTestModel 程序化桩/OpenYSMStub 面）回退既有 sin 摆四肢
     * （原 LegacyAnimationDriver 程序化路径收编），避免桩模型从"会走"回退成静像。
     * ponytail: 主线不存在无动画模型故无此回退；真实 .ysm 全部走状态机，
     * 桩模型退役时连同本方法删除。
     */
    private static boolean hasAnimationData(ClientModelInfo bundle) {
        return LegacyAnimationSampler.findAnimation(bundle, "idle") != null;
    }

    /**
     * 19 状态扁表，顺序=主线优先级桶序×桶内注册序（AnimationRegister.java:17-46
     * 逐行对位）。先命中先赢。
     */
    private static final AnimState[] STATES = {
            // P0 HIGHEST（AnimationRegister:18-26）
            new AnimState("death", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.getHealth() <= 0.0f; // 1.12.2 无 isDeadOrDying（1.13+）；EntityLivingBase:1314 同判据
                }
            }),
            new AnimState("riptide", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 1.13+ 三叉戟激流，1.12.2 无对位 API
                }
            }),
            new AnimState("sleep", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isPlayerSleeping(); // EntityPlayer:1334
                }
            }),
            new AnimState("swim", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    // 1.12.2 无 isSwimming（1.13+）；对位=水中离地且移幅过门（onGround Entity:113 / isInWater Entity:975）
                    return p.isInWater() && !p.onGround && Math.abs(lsa) > MIN_SPEED;
                }
            }),
            new AnimState("climb", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 主线判据 Pose.SWIMMING（AnimationRegister:22），1.12.2 无 Pose
                }
            }),
            new AnimState("climbing", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 同上（AnimationRegister:23）
                }
            }),
            new AnimState("ladder_up", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isOnLadder() && verticalSpeed(p) > 0.0f; // isOnLadder EntityLivingBase:1003
                }
            }),
            new AnimState("ladder_stillness", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isOnLadder() && verticalSpeed(p) == 0.0f;
                }
            }),
            new AnimState("ladder_down", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isOnLadder() && verticalSpeed(p) < 0.0f;
                }
            }),
            // P1 HIGH（AnimationRegister:27-37）
            new AnimState("fly", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.capabilities.isFlying; // EntityPlayer:124 公开字段 + PlayerCapabilities:7
                }
            }),
            new AnimState("elytra_fly", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isElytraFlying(); // EntityLivingBase:2182（DataWatcher 旗标，远端亦同步）
                }
            }),
            // P2 NORMAL（AnimationRegister:38-42）
            new AnimState("swim_stand", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isInWater() && !p.onGround; // AnimationRegister:38 判据原样
                }
            }),
            new AnimState("attacked", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.hurtTime > 0; // EntityLivingBase 公开字段（handleStatusUpdate 写 maxHurtTime=10）
                }
            }),
            new AnimState("jump", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return !p.onGround && !p.isInWater(); // AnimationRegister:40 判据原样
                }
            }),
            new AnimState("sneak", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    // 1.12.2 无 Pose.CROUCHING，潜行=isSneaking（Entity:1804，旗标远端同步）
                    return p.onGround && p.isSneaking() && Math.abs(lsa) > MIN_SPEED;
                }
            }),
            new AnimState("sneaking", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.onGround && p.isSneaking();
                }
            }),
            // P3 LOW（AnimationRegister:43-44）
            new AnimState("run", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.onGround && p.isSprinting(); // isSprinting Entity:1812
                }
            }),
            new AnimState("walk", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.onGround && lsa > MIN_SPEED;
                }
            }),
            // P4 LOWEST（AnimationRegister:45 兜底）
            new AnimState("idle", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return true;
                }
            }),
    };

    private static final Map<UUID, LegacyWorldAnimationController> CONTROLLERS = new ConcurrentHashMap<>();

    private final UUID owner;
    private String displayName = "?";
    private float[] lastParamsRef;
    private int phase = IDLE;
    private String lastRequested;
    private String currentAnim;
    private int mode = MODE_LOOP;
    private float animLength;
    private float animTime;
    private float phaseTime;
    private float[] savedPose;
    private long lastNanos;

    /**
     * swing/use 独立通道（wave-d-anim-2）：注册序在 main 之后（主线
     * PlayerAnimationController.java:67 swing / :70 use），与 main 19 状态并行不互斥；
     * 重叠骨按注册序后到者赢（见 Channel javadoc）。swing 恒 PLAY_ONCE（主线 :59）、
     * use 恒 LOOP（主线 :37）——注册 loopType 覆盖动画自身值。首帧门标记：swing=
     * swingProgressInt==0（主线 :48 swingTime==0 同构）、use=getItemInUseMaxCount()==1
     * （主线 :25 getTicksUsingItem()==1 同构）。
     */
    private final Channel swingChannel = new Channel("swing", SWING_BEGIN_TICKS, MODE_ONCE, 0);
    private final Channel useChannel = new Channel("use", USE_BEGIN_TICKS, MODE_LOOP, 1);
    /**
     * vehicle 通道（wave-d-anim-3）：坐骑链 ride_pig/ride/boat/sit+ConditionVehicle id 面
     * （主线 LivingMovementAnimationPredicate.java:49-102 逐行对位，全 LOOP，注册转场 0.1f
     * =PlayerAnimationController.java:55）。fireMarker=-1=状态门（主线谓词逐帧求值，无
     * swing/use 的首帧沿窗）。
     */
    private final Channel vehicleChannel = new Channel("vehicle", VEHICLE_BEGIN_TICKS, MODE_LOOP, -1);

    private LegacyWorldAnimationController(UUID owner) {
        this.owner = owner;
    }

    /** 世界渲染每帧入口（LegacyAnimationDriver.tick 委托）。每帧全量重写 params（世界/GUI 互不污染先例）。 */
    static void tick(EntityPlayer player, float limbSwing, float limbSwingAmount,
                     GeoModel model, float[] params, ClientModelInfo bundle) {
        if (player == null || model == null || params == null || model.bakedBones == null) {
            return;
        }
        LegacyWorldAnimationController c = CONTROLLERS.get(player.getUniqueID());
        if (c == null) {
            c = new LegacyWorldAnimationController(player.getUniqueID());
            CONTROLLERS.put(c.owner, c);
        }
        c.tickFrame(player, limbSwing, limbSwingAmount, model, params, bundle);
    }

    private void tickFrame(EntityPlayer player, float limbSwing, float limbSwingAmount,
                           GeoModel model, float[] params, ClientModelInfo bundle) {
        displayName = player.getName();
        float dt = frameDeltaTicks();
        // 模型/参数面换绑（热重载替换数组或换模型 id）→ 硬复位（pin 相当于 initBoneQueues fullReset）
        if (params != lastParamsRef || (savedPose != null && savedPose.length != params.length)) {
            lastParamsRef = params;
            phase = IDLE;
            lastRequested = null;
            savedPose = null;
        }

        // 无动画数据的模型走收编的程序化路径（LegacyTestModel 桩面），真实 .ysm 全走状态机
        if (!hasAnimationData(bundle)) {
            procedural(player, limbSwing, limbSwingAmount, model, params);
            return;
        }

        // 状态裁决：先匹配先赢（AnimationManager.java:56-72 同构）；骑乘→null（:49-52
        // vehicle!=null && vehicle.isAlive() → STOP）
        String state = ridingAlive(player) ? null : resolve(player, limbSwingAmount);
        // pin 置位先于查动画（setAnimation:103 同序：等值早退的键）
        String from = lastRequested;
        boolean requested = state != null && !state.equals(lastRequested);
        if (requested) {
            lastRequested = state;
        }
        Animation anim = state == null ? null : LegacyAnimationSampler.findAnimation(bundle, state);
        boolean hasAnim = anim != null && anim.animationLength > 0.0f;

        switch (phase) {
            case IDLE:
                if (state == null) {
                    LegacyAnimationSampler.resetParams(model, params);
                } else if (hasAnim) {
                    begin(from, state, anim, params);
                } else if (requested) {
                    // 缺动画=主线 STOP 静像+骨复位曲线：淡回绑定位，pin 已置持续挡重入（不回落低优先级）
                    startEnding(copyPose(params), "miss:" + state);
                } else {
                    LegacyAnimationSampler.resetParams(model, params);
                }
                break;
            case BEGINNING:
            case RUNNING:
            case ENDING:
                if (state == null) {
                    if (phase == BEGINNING || phase == RUNNING) {
                        // STOP 分支镜像：淡出+清 pin（PredicateBasedController.process STOP 分支）
                        startEnding(copyPose(params), "stop");
                        lastRequested = null;
                    }
                    // ENDING 续淡
                } else if (requested) {
                    // 状态切换（含 ENDING 中新请求：主线 setAnimation:102 clearAnimation 同刻落 pending）
                    if (hasAnim) {
                        begin(from, state, anim, params);
                    } else {
                        startEnding(copyPose(params), "miss:" + state);
                    }
                }
                break;
            default:
                break;
        }

        advance(dt, model, params, bundle);

        // vehicle/swing/use 通道（wave-d-anim-2/-3）：main 采样结果之上叠加。通道相对序=
        // 主线注册序（vehicle :55 → swing :67 → use :70），重叠骨后到者赢（骑乘挥剑=swing
        // 覆 ride 触骨）。vehicle 主线注册于 main 之前，main 骑乘即 STOP（无贡献）使该序
        // 差无观察面，唯上马 3t 淡出窗方向相反（适形注见 Channel javadoc）。通道自带相位机。
        vehicleChannel.tickFrame(player, dt, model, params, bundle);
        swingChannel.tickFrame(player, dt, model, params, bundle);
        useChannel.tickFrame(player, dt, model, params, bundle);
    }

    private void advance(float dt, GeoModel model, float[] params, ClientModelInfo bundle) {
        switch (phase) {
            case BEGINNING: {
                animTime += dt;
                phaseTime += dt;
                LegacyAnimationSampler.resetParams(model, params);
                LegacyAnimationSampler.sample(model, params, bundle, currentAnim, animTime);
                blend(params, savedPose, Math.min(phaseTime / BEGIN_TICKS, 1.0f));
                if (phaseTime >= BEGIN_TICKS) {
                    phase = RUNNING;
                    phaseTime = 0.0f;
                }
                break;
            }
            case RUNNING: {
                animTime += dt;
                LegacyAnimationSampler.resetParams(model, params);
                LegacyAnimationSampler.sample(model, params, bundle, currentAnim, animTime);
                // PLAY_ONCE 播完持末帧→ENDING（主线 startEndingTransition:208-238，savedEndingTick=len）；
                // HOLD 钳 len 常驻（:160-162，sample 内部已钳）不进 ENDING
                if (mode == MODE_ONCE && animTime >= animLength) {
                    startEnding(copyPose(params), "once:" + currentAnim);
                }
                break;
            }
            case ENDING: {
                phaseTime += dt;
                float f = Math.min(phaseTime / END_TICKS, 1.0f);
                LegacyAnimationSampler.resetParams(model, params);
                blend(params, savedPose, 1.0f - f); // savedPose→绑定位 3 tick 淡出
                if (f >= 1.0f) {
                    phase = IDLE; // pin 保留（主线 clearAnimation 不清 lastRequestedAnimation）
                }
                break;
            }
            case IDLE:
            default:
                break;
        }
    }

    /** 进入新动画：BEGINNING 从上一帧姿态淡入，动画局部时间轴从 0 起（全局时钟不动=m2.6.1 教训）。 */
    private void begin(String from, String name, Animation anim, float[] params) {
        boolean loop = anim.loop == ILoopType.EDefaultLoopTypes.LOOP;
        LOG.info(String.format("[ysm-legacy122] animState %s: %s -> %s (loop=%b len=%.1f phase=%d)",
                displayName, from == null ? "-" : from, name, loop,
                anim.animationLength, phase));
        currentAnim = name;
        mode = modeOf(name);
        animLength = anim.animationLength;
        animTime = 0.0f;
        phaseTime = 0.0f;
        savedPose = copyPose(params);
        phase = BEGINNING;
    }

    /** 原 LegacyAnimationDriver 程序化摆（LegacyTestModel 桩面收编，见 hasAnimationData）。 */
    private static void procedural(EntityPlayer player, float limbSwing, float limbSwingAmount,
                                   GeoModel model, float[] params) {
        float amount = Math.min(1.0f, player.isSprinting()
                ? limbSwingAmount * 1.35f : limbSwingAmount);
        float swing = net.minecraft.util.math.MathHelper.sin(limbSwing * 0.6662f)
                * 1.4f * amount;
        LegacyAnimationSampler.resetParams(model, params);
        for (int i = 0; i < model.bakedBones.size() && (i * 12 + 11) < params.length; i++) {
            String name = model.bakedBones.get(i).name;
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            int p = i * 12;
            if (lower.contains("rightarm") || lower.contains("armright")) {
                params[p] = -swing;
            } else if (lower.contains("leftarm") || lower.contains("armleft")) {
                params[p] = swing;
            } else if (lower.contains("rightleg") || lower.contains("legright")) {
                params[p] = swing;
            } else if (lower.contains("leftleg") || lower.contains("legleft")) {
                params[p] = -swing;
            }
        }
    }

    private void startEnding(float[] fromPose, String reason) {
        LOG.info(String.format("[ysm-legacy122] animEnd %s: fade-to-bind (%s)", displayName, reason));
        savedPose = fromPose;
        phaseTime = 0.0f;
        currentAnim = null;
        phase = ENDING;
    }

    private static String resolve(EntityPlayer p, float lsa) {
        for (int i = 0; i < STATES.length; i++) {
            if (STATES[i].test.test(p, lsa)) {
                return STATES[i].name;
            }
        }
        return null;
    }

    /**
     * 骑乘存活门，主线 AnimationManager.java:49-52（main STOP）与
     * LivingMovementAnimationPredicate.java:53（vehicle 链不触发）共用同一面：
     * vehicle != null && vehicle.isAlive()。1.12.2 对位=Entity.isEntityAlive
     * （vanilla-mc-1.12.2 Entity.java:1582-1584，=!isDead，对 EntityBoat 这类
     * 非 Living 载具可达）；getRidingEntity（Entity.java:2473）。
     */
    private static boolean ridingAlive(EntityPlayer p) {
        Entity vehicle = p.getRidingEntity();
        return vehicle != null && vehicle.isEntityAlive();
    }

    /** 主线 AnimationRegister.getVerticalSpeed:56-58 对位（yo→prevPosY）。 */
    private static float verticalSpeed(EntityPlayer p) {
        return 20.0f * (float) (p.posY - p.prevPosY);
    }

    private float frameDeltaTicks() {
        long now = System.nanoTime();
        float dt = 0.0f;
        if (lastNanos != 0L) {
            dt = (now - lastNanos) / 50_000_000.0f;
            if (dt > MAX_FRAME_TICKS) {
                dt = MAX_FRAME_TICKS;
            } else if (dt < 0.0f) {
                dt = 0.0f;
            }
        }
        lastNanos = now;
        return dt;
    }

    /** params = saved + (params - saved) * f（MathUtil.lerpValues:71-75 浮点阵适形）。 */
    private static void blend(float[] params, float[] saved, float f) {
        if (saved == null || saved.length != params.length) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            params[i] = saved[i] + (params[i] - saved[i]) * f;
        }
    }

    private static float[] copyPose(float[] params) {
        return Arrays.copyOf(params, params.length);
    }

    /**
     * swing/use 独立通道（wave-d-anim-2）。主线对位：swing=ItemHoldAnimationPredicate
     * （PlayerAnimationController.java:67 注册，转场 0.0f，谓词 PLAY_ONCE :59）、use=
     * InteractionHandAnimationPredicate（:70 注册，转场 0.1f，谓词 LOOP :37/:46）；两通道
     * 与 main 控制器各自独立裁决（并行不互斥），注册序在 main 之后。
     *
     * <p>跨控制器混合语义（适形）：主线 AnimationProcessor.applyTransform:204-256 逐
     * 控制器施加——首贡献者清零骨值、后续贡献者经 applyLinearBlendTo/applyRotationBlendTo
     * 混入；PredicateBasedController（主线 coded 谓词路径）对普通 AnimationPoint 恒
     * setPercentCompleted(0.0f)（:249/:268/:290），progress=0 时两 blend 即 target.set(本帧值)
     * （TransitionVector3f.java:29-54）→ 后注册控制器在同骨上整值覆盖先注册者。本通道=
     * main 采样结果之上的覆盖层：RUNNING 仅写通道动画有关键帧的骨通道（written mask），
     * 未触骨保持 main 当帧值；BEGINNING/ENDING 与 savedPose/endingPose 间 lerp 只作用于
     * 触面 offsets。
     *
     * <p>1.12.2 门面对位（recompiled_minecraft-1.12.2.jar javap 亲证 + 源码行号）：
     * <ul>
     * <li>swing：isSwingInProgress（EntityLivingBase:93 公开字段）↔主线 swinging；
     *     swingProgressInt（:95）↔主线 swingTime——swingArm :1156-1165 置 -1，
     *     updateArmSwingProgress :1219-1232 每 tick ++，旗标期内恰一拍为 0=主线 :48
     *     swingTime==0 首帧门同构；swingingHand（:94）↔主线 swingingArm。远端玩家经
     *     SPacketAnimation → swingArm（NetHandlerPlayClient:734-743）+EntityOtherPlayerMP:94
     *     updateArmSwingProgress 逐 tick，本地/远端同面。</li>
     * <li>use：isHandActive（EntityLivingBase:2046，HAND_STATES DataParameter 远端同步）
     *     ↔主线 isUsingItem；getItemInUseMaxCount（:2150-2152 = 最大使用时长-剩余计数，
     *     随 tick 递增）↔主线 getTicksUsingItem，首帧恰一拍==1=主线 :25 同构；
     *     getActiveHand（:2050）↔ getUsedItemHand。</li>
     * </ul>
     *
     * <p>pin/沿语义：pin（lastRequested）等值不重触发；首帧沿 armed=主线
     * PositionTracker.markProcessed 等价形（标记离开触发值重新武装，一次窗内一拍）；
     * 门消失=主线谓词 STOP 分支（淡出+清 pin）；PLAY_ONCE 播完进 ENDING 且 pin 保留
     * （主线 clearAnimation 不清 lastRequestedAnimation），下次挥动经首帧沿重触发。
     * 缺动画=只置 pin 不贡献（主线 setAnimation:104-107 同构，观察上=纯 main 值）。
     *
     * <p>vehicle 通道（wave-d-anim-3）：状态门（fireMarker=-1，无沿窗）——骑乘即请求、
     * 换乘即切换、下马即 STOP 淡出+清 pin（主线 PredicateBasedController STOP 分支同构，
     * 与 main :49-52 的骑乘 STOP 语义同一镜像）。注册转场 0.1f=BEGINNING 2 tick、全
     * LOOP（PlayerAnimationController.java:55 + LivingMovementAnimationPredicate 各分支）。
     * 适形注：主线 vehicle 注册于 main（:57）之前，跨控制器后注册者整值覆盖语义下
     * 本应 main 触骨覆盖 vehicle——但 main 骑乘即 STOP（AnimationManager:49-52）无贡献，
     * 故此处 vehicle 叠加在 main 采样结果之上（本架构通道必须在 main 后写）与主线
     * 唯一观察差=上马后 main 3t 淡出窗内骑乘触骨先出 ride 姿态（主线窗内仍是 main 淡出
     * 姿态），下马方向（vehicle ENDING 淡向 main 当帧值）两线一致。
     */
    private final class Channel {
        final String key;
        final float beginTicks;
        final int mode;
        final int fireMarker;
        final boolean swing;
        int phase = IDLE;
        String lastRequested;
        String currentAnim;
        float animLength;
        float animTime;
        float phaseTime;
        boolean[] written;
        float[] savedPose;
        float[] endingPose;
        boolean armed = true;

        Channel(String key, float beginTicks, int mode, int fireMarker) {
            this.key = key;
            this.beginTicks = beginTicks;
            this.mode = mode;
            this.fireMarker = fireMarker;
            this.swing = "swing".equals(key);
        }

        void tickFrame(EntityPlayer p, float dt, GeoModel model, float[] params, ClientModelInfo bundle) {
            // 模型/参数面换绑（热重载）→ 硬复位（main 机 lastParamsRef 同款守卫）
            if (written != null && written.length != params.length) {
                phase = IDLE;
                lastRequested = null;
                currentAnim = null;
                savedPose = null;
                endingPose = null;
                written = null;
            }
            String requested = gateRequest(p, bundle);
            boolean fire = false;
            if (requested != null && fireMarker >= 0) {
                int marker = swing ? p.swingProgressInt : p.getItemInUseMaxCount();
                if (marker != fireMarker) {
                    armed = true;
                } else if (armed) {
                    armed = false;
                    fire = true;
                }
            }
            if (requested == null) {
                if (phase == BEGINNING || phase == RUNNING) {
                    // 主线谓词 STOP 分支镜像：淡出+清 pin
                    startEnding(copyPose(params), "stop");
                    lastRequested = null;
                }
                // ENDING 续淡
            } else if (fire || !requested.equals(lastRequested)) {
                // fire=主线 stopTransition（首帧门+markProcessed 清 pin）再 setAnimation 同拍重启
                String from = lastRequested;
                lastRequested = requested;
                Animation anim = LegacyAnimationSampler.findAnimation(bundle, requested);
                if (anim != null && anim.animationLength > 0.0f) {
                    begin(from, requested, anim, model, params);
                }
                // 缺动画=只置 pin 不贡献（主线 setAnimation:104-107）
            }
            advanceChannel(dt, model, params, bundle);
        }

        /** 通道门分派：swing/use=首帧沿门（fireMarker>=0），vehicle=状态门（主线谓词逐帧求值）。 */
        private String gateRequest(EntityPlayer p, ClientModelInfo bundle) {
            if (fireMarker < 0) {
                return vehicleGate(p, bundle);
            }
            return swing ? swingGate(p, bundle) : useGate(p, bundle);
        }

        private void advanceChannel(float dt, GeoModel model, float[] params, ClientModelInfo bundle) {
            switch (phase) {
                case BEGINNING: {
                    animTime += dt;
                    phaseTime += dt;
                    LegacyAnimationSampler.applyChannelAnimation(model, params, bundle, currentAnim, animTime);
                    float f = Math.min(phaseTime / beginTicks, 1.0f);
                    blendWritten(params, savedPose, f);
                    if (phaseTime >= beginTicks) {
                        phase = RUNNING;
                        phaseTime = 0.0f;
                    }
                    break;
                }
                case RUNNING: {
                    animTime += dt;
                    LegacyAnimationSampler.applyChannelAnimation(model, params, bundle, currentAnim, animTime);
                    if (mode == MODE_ONCE && animTime >= animLength) {
                        startEnding(copyPose(params), "once:" + currentAnim);
                    }
                    break;
                }
                case ENDING: {
                    phaseTime += dt;
                    float f = Math.min(phaseTime / END_TICKS, 1.0f);
                    // params[written] 此刻=main 当帧值（通道未写）；endingPose→main 淡出。
                    // 适形：主线 ENDING 是通道贡献乘 (1-f) 叠在 main 之上，此处为
                    // endingPose 与 main 当帧值两点 lerp——收敛点同为主帧值，淡出曲线同级。
                    if (endingPose != null && endingPose.length == params.length) {
                        for (int i = 0; i < params.length; i++) {
                            if (written[i]) {
                                params[i] = params[i] + (endingPose[i] - params[i]) * (1.0f - f);
                            }
                        }
                    }
                    if (f >= 1.0f) {
                        phase = IDLE; // once 完 pin 保留（挡重播至下一挥动）；stop 已清 pin
                    }
                    break;
                }
                default:
                    break;
            }
        }

        /** 进入新动画：BEGINNING 从 begin 时刻 main 结果淡入（swing beginTicks=0 即进 RUNNING）。 */
        private void begin(String from, String name, Animation anim, GeoModel model, float[] params) {
            LOG.info(String.format("[ysm-legacy122] animChannel %s %s: %s -> %s (main=%s mode=%s len=%.1f phase=%d)",
                    key, displayName, from == null ? "-" : from, name,
                    currentAnimOfMain(), mode == MODE_ONCE ? "ONCE" : "LOOP", anim.animationLength, phase));
            currentAnim = name;
            animLength = anim.animationLength;
            animTime = 0.0f;
            phaseTime = 0.0f;
            written = LegacyAnimationSampler.writtenOffsets(model, anim, params.length);
            savedPose = copyPose(params);
            phase = beginTicks <= 0.0f ? RUNNING : BEGINNING;
        }

        private void startEnding(float[] fromPose, String reason) {
            LOG.info(String.format("[ysm-legacy122] animChannel %s %s end: fade-to-main (%s)",
                    key, displayName, reason));
            endingPose = fromPose;
            phaseTime = 0.0f;
            currentAnim = null;
            phase = ENDING;
        }

        private void blendWritten(float[] params, float[] saved, float f) {
            if (saved == null || saved.length != params.length || written == null) {
                return;
            }
            for (int i = 0; i < params.length; i++) {
                if (written[i]) {
                    params[i] = saved[i] + (params[i] - saved[i]) * f;
                }
            }
        }
    }

    /** main 通道当前动画名（通道日志并行证据：挥动时 main=walk 即并行不互斥）。 */
    private String currentAnimOfMain() {
        return currentAnim == null ? "-" : currentAnim;
    }

    /**
     * swing 门（主线 ItemHoldAnimationPredicate.java:47-59 对位：
     * swinging && !isSleeping()，按 swingingArm 分 swing_hand/swing_offhand）。
     * 返回条件面解析后的请求动画名；null=不活跃。
     */
    private String swingGate(EntityPlayer p, ClientModelInfo bundle) {
        if (!p.isSwingInProgress || p.isPlayerSleeping()) {
            return null;
        }
        // swingingHand==null 守卫取主手（旗标期内恒非 null，防御面）
        boolean offhand = p.swingingHand == EnumHand.OFF_HAND;
        ItemStack held = offhand ? p.getHeldItemOffhand() : p.getHeldItemMainhand();
        return resolveConditioned(p, bundle, held,
                offhand ? "swing_offhand$" : "swing$",
                offhand ? "swing_offhand:" : "swing:",
                offhand ? "swing_offhand" : "swing_hand");
    }

    /**
     * use 门（主线 InteractionHandAnimationPredicate.java:24-46 对位：
     * isUsingItem() && !isSleeping()，按 getUsedItemHand 分 use_mainhand/use_offhand）。
     */
    private String useGate(EntityPlayer p, ClientModelInfo bundle) {
        if (!p.isHandActive() || p.isPlayerSleeping()) {
            return null;
        }
        boolean offhand = p.getActiveHand() == EnumHand.OFF_HAND;
        ItemStack held = offhand ? p.getHeldItemOffhand() : p.getHeldItemMainhand();
        return resolveConditioned(p, bundle, held,
                offhand ? "use_offhand$" : "use_mainhand$",
                offhand ? "use_offhand:" : "use_mainhand:",
                offhand ? "use_offhand" : "use_mainhand");
    }

    /**
     * vehicle 门（主线 LivingMovementAnimationPredicate.renderRidingAnimation:49-102
     * 逐行对位；SWEM gait :56-59/TLM chair :61-66/carryon:princess :92-96/TLM :97-100
     * 第三方 mod 面不实现=域外声明）。分支序：ConditionVehicle id → ride_pig → ride →
     * boat → sit 兜底，全 LOOP（主线各分支 playAnimationWithLoop(LOOP)），命中即 return
     * 不后落。vanilla 面 1.12.2 考古（vanilla-mc-1.12.2 行号亲证）：
     * <ul>
     * <li>ConditionVehicle（:67-73）：id 面=vehicle$&lt;entityId&gt; 命中即 return
     *     （主线 doIdTest ConditionVehicle.java:78-85 返回 idPre+key 同构）。idTest 集合由
     *     模型动画名建面（ConditionManager.addTest ← ModelAssemblyFactory.java:88-90 全动画
     *     名遍历）→ 集合含 key ⟺ 模型含该动画，故动画存在性查=等价适形，无存在动画的
     *     其他 vehicle$ 名照主线一样不拦截。EntityList.getKey（EntityList.java:113，
     *     @Nullable，REGISTRY name "minecraft:horse" 形）。tag 面（vehicle#）1.12.2 无
     *     data pack tags（1.13+）不实现。</li>
     * <li>ride_pig（:74-76）：vehicle instanceof EntityPig（passive/EntityPig.java:41）。</li>
     * <li>ride（:77-88 &lt;21.5 分支=instanceof Saddleable，无鞍检）：1.12.2 鞍乘族=
     *     AbstractHorse（passive/AbstractHorse.java:58，子类 EntityHorse:30/
     *     AbstractChestHorse:23/EntitySkeletonHorse:20/EntityZombieHorse:18；Llama 经
     *     AbstractChestHorse 亦命中，现代 Saddleable 面 Llama 同命中=覆盖一致）。</li>
     * <li>boat（:89-91）：vehicle instanceof EntityBoat（item/EntityBoat.java:38）。</li>
     * <li>sit（:101 兜底）：其余存活载具（矿车/其他玩家等）。</li>
     * </ul>
     */
    private static String vehicleGate(EntityPlayer p, ClientModelInfo bundle) {
        Entity vehicle = p.getRidingEntity(); // Entity.java:2473
        if (vehicle == null || !vehicle.isEntityAlive()) { // 主线 :53 gate 同构
            return null;
        }
        ResourceLocation key = EntityList.getKey(vehicle);
        if (key != null) {
            String conditioned = "vehicle$" + key; // toString=domain:path（主线 idPre+key 同构）
            if (LegacyAnimationSampler.findAnimation(bundle, conditioned) != null) {
                return conditioned;
            }
        }
        if (vehicle instanceof EntityPig) {
            return "ride_pig";
        }
        if (vehicle instanceof AbstractHorse) {
            return "ride";
        }
        if (vehicle instanceof EntityBoat) {
            return "boat";
        }
        return "sit";
    }

    /**
     * ConditionSwing/ConditionUse 数据条件面最小档（ConditionSwing.doTest:89-102
     * 序：id → tag → extra；ConditionUse 同构）。id 面=手持物品 registry name 命中
     * 模型内同前缀动画（主线 :108-112 返回 idPre+key 同构）；extra 面=内部分类
     * （主线 InnerClassify.getItemType:25-86 类序，:125-133 先 inner 后动作）+使用
     * 动作（主线 :133-136，"none" 跳过 :80 同构）。tag 面（swing#…）1.12.2 无
     * data pack tags（1.13+）不实现；TLM gohei/slashblade/lance 模组面不实现
     * （compat 域）。1.12.2 类对位（javap 亲证）：ItemSword/ItemAxe/ItemPickaxe/
     * ItemSpade(=shovel)/ItemHoe/ItemShield/ItemBow/ItemFishingRod；crossbow（1.14+）
     * 与 spear（三叉戟 1.13+）恒缺。
     */
    private static String resolveConditioned(EntityPlayer p, ClientModelInfo bundle, ItemStack held,
                                             String idPrefix, String actionPrefix, String base) {
        if (held.isEmpty()) { // ItemStack.isEmpty（compile jar javap 亲证）
            return base;
        }
        ResourceLocation id = held.getItem().getRegistryName(); // Item 继承 IForgeRegistryEntry.Impl
        if (id != null) {
            String conditioned = idPrefix + id; // toString=domain:path
            if (LegacyAnimationSampler.findAnimation(bundle, conditioned) != null) {
                return conditioned;
            }
        }
        String classify = classifyItem(held);
        if (!classify.isEmpty()) {
            String conditioned = actionPrefix + classify;
            if (LegacyAnimationSampler.findAnimation(bundle, conditioned) != null) {
                return conditioned;
            }
        }
        UseAction action = UseAction.of(held); // 共享门面 <1.13 分支=ItemStack.getItemUseAction()
        if (action != UseAction.NONE) {
            String conditioned = actionPrefix + action.name().toLowerCase(Locale.US);
            if (LegacyAnimationSampler.findAnimation(bundle, conditioned) != null) {
                return conditioned;
            }
        }
        return base;
    }

    /** 内部分类面（主线 InnerClassify.getItemType:25-86 类序，1.12.2 可达子集）。 */
    private static String classifyItem(ItemStack held) {
        Item item = held.getItem();
        if (item instanceof ItemSword) {
            return "sword";
        }
        if (item instanceof ItemAxe) {
            return "axe";
        }
        if (item instanceof ItemPickaxe) {
            return "pickaxe";
        }
        if (item instanceof ItemSpade) {
            return "shovel"; // 1.12.2 铲=ItemSpade，主线分类名沿用 shovel
        }
        if (item instanceof ItemHoe) {
            return "hoe";
        }
        if (item instanceof ItemShield) {
            return "shield";
        }
        if (item instanceof ItemBow) {
            return "bow";
        }
        if (item instanceof ItemFishingRod) {
            return "fishing_rod";
        }
        return "";
    }
}
