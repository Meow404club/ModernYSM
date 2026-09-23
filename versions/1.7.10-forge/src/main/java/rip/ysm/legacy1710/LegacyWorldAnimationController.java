package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.client.ClientModelInfo;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 1.7.10 世界渲染动画状态机（wave-d-anim1-world-state-machine-1710）。
 *
 * <p>1.12.2 LegacyWorldAnimationController（已合 06801af）的 1.7.10 twin：19 状态
 * 扁表+优先级裁决+三相转场+pin 语义逐式照搬。主线对位：main 控制器 19 coded 状态
 * 优先级表（AnimationRegister.java:17-46 + AnimationManager.java:56-72 先匹配先赢）
 * + 三相转场（BEGINNING 淡入/RUNNING/ENDING 3.0f 淡出）。每个状态判定源逐条考古
 * vanilla-mc-1.7.10（MCP stable_12 行号锚见各谓词注释），每渲染帧从玩家实体公开
 * 字段求值当前状态，命中→按 {@link LegacyAnimationSampler} 既有合并序查同名动画→
 * 采样写 boneParams 12 float/骨契约。
 *
 * <p>语义镜像（与 122 twin 同款，逐条对照主线代码后落定）：
 * <ul>
 * <li>先匹配先赢：扁表按优先级桶序×桶内注册序扁平化，命中即停；模型缺该状态动画
 *     时【不】回落低优先级（缺动画只置 pin 不落 pending → 绑定位静像），缺动画
 *     淡出=骨复位曲线 → 复用 ENDING 数学淡回绑定位。</li>
 * <li>同状态重复请求不重触发（pin 等值早退）；PLAY_ONCE 播完 → ENDING 淡回绑定
 *     位 → IDLE 后 pin 挡住不重播——死亡等持续状态播一次后保持绑定位，状态切换
 *     才可重进。HOLD_ON_LAST_FRAME 钳 len 常驻 RUNNING 不进 ENDING。</li>
 * <li>状态消失（骑乘→STOP）：淡出+清 pin，ENDING 续淡，IDLE 静像——逐相镜像。</li>
 * <li>BEGINNING 从上一帧姿态 lerp 进新动画，动画时间轴 BEGINNING 期间照常推进；
 *     全局时钟永不回退（m2.6.1 教训）；状态机不清全局时间轴。</li>
 * </ul>
 *
 * <p>1.7.10 无法对位的状态（如实保留表内恒假，注释即考古结论）：
 * riptide=激流勇进 1.13+ 三叉戟（vanilla-mc-1.7.10 全文无 isAutoSpinAttack）；
 * elytra_fly=鞘翅 1.9+（vanilla-mc-1.7.10 全文无 isElytraFlying/Elytra）；
 * climb/climbing=主线 Pose.SWIMMING 判据（1.13+ 泳姿，1.7.10 无 Pose 体系）。
 * swim 在 1.7.10 对位=水中离地且移幅过门（isSwimming 是 1.13+ 独立泳姿），
 * swim_stand 沿主线判据原样。
 *
 * <p>ponytail: fly 判定走 capabilities.isFlying——1.7.10 vanilla 不向观察者同步
 * PlayerCapabilities，远端玩家恒 false（主线对位=positionTracker 位置推断，本卡
 * 仅本地玩家精确，升级路径=按位置历史推断）；多玩家同模型共享 per-model params 面
 * （既有布局），同模型多玩家动画取最后渲染者，升级路径=per-UUID params。
 * 每玩家时钟=渲染帧间 nanoTime 增量（渲染才推进，离屏暂停），钳 5 tick/帧防跳轴。
 */
final class LegacyWorldAnimationController {

    private static final Logger LOG = Logger.getLogger("yes_steve_model");

    private static final float MIN_SPEED = 0.05f;
    /** 主线 main 控制器注册转场 0.1f，TicksInterpolator ×20 = 2 tick。 */
    private static final float BEGIN_TICKS = 2.0f;
    /** AnimationControllerInstance defaultTransitionTick = 3.0f。 */
    private static final float END_TICKS = 3.0f;
    /** 单帧最大推进 tick（卡顿钳制）。 */
    private static final float MAX_FRAME_TICKS = 5.0f;

    private static final int IDLE = 0;
    private static final int BEGINNING = 1;
    private static final int RUNNING = 2;
    private static final int ENDING = 3;

    /** 播放模式：状态注册固定 LOOP/PLAY_ONCE 两态（主线注册 loopType 覆盖动画自身值）。 */
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

    /**
     * 播放模式按主线注册逐行对位（AnimationRegister.java:18/:39 PLAY_ONCE，余 LOOP）。
     * ponytail: riptide=MODE_ONCE 系 122 蓝本原样照搬（主线 :19 两参 register=LOOP，
     * 1.7.10 恒假死配置零行为差）；两线 twin 对齐归后续触表卡一并修。
     */
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
     * 逐行对位，与 122 twin 同表同序）。先命中先赢。
     */
    private static final AnimState[] STATES = {
            // P0 HIGHEST（AnimationRegister:18-26）
            new AnimState("death", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.getHealth() <= 0.0f; // vanilla-mc-1710 EntityLivingBase.java:590 public final float getHealth()
                }
            }),
            new AnimState("riptide", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 1.13+ 三叉戟激流，1.7.10 无对位 API
                }
            }),
            new AnimState("sleep", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isPlayerSleeping(); // vanilla-mc-1710 EntityPlayer.java:1178
                }
            }),
            new AnimState("swim", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    // 1.7.10 无 isSwimming（1.13+）；对位=水中离地且移幅过门（onGround Entity.java:61 / isInWater Entity.java:748）
                    return p.isInWater() && !p.onGround && Math.abs(lsa) > MIN_SPEED;
                }
            }),
            new AnimState("climb", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 主线判据 Pose.SWIMMING（AnimationRegister:22），1.7.10 无 Pose
                }
            }),
            new AnimState("climbing", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 同上（AnimationRegister:23）
                }
            }),
            new AnimState("ladder_up", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return p.isOnLadder() && verticalSpeed(p) > 0.0f; // isOnLadder vanilla-mc-1710 EntityLivingBase.java:779
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
                    return p.capabilities.isFlying; // EntityPlayer.java:101 公开字段 + PlayerCapabilities.java:7
                }
            }),
            new AnimState("elytra_fly", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return false; // 鞘翅 1.9+（vanilla-mc-1.7.10 全文无 Elytra API），恒假保留表位
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
                    return p.hurtTime > 0; // vanilla-mc-1710 EntityLivingBase.java:63 public int hurtTime（受击写 maxHurtTime）
                }
            }),
            new AnimState("jump", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    return !p.onGround && !p.isInWater(); // AnimationRegister:40 判据原样
                }
            }),
            new AnimState("sneak", new StateTest() {
                public boolean test(EntityPlayer p, float lsa) {
                    // 1.7.10 无 Pose.CROUCHING，潜行=isSneaking（vanilla-mc-1710 Entity.java:1362，旗标远端同步）
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
                    return p.onGround && p.isSprinting(); // isSprinting vanilla-mc-1710 Entity.java:1370
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

    private LegacyWorldAnimationController(UUID owner) {
        this.owner = owner;
    }

    /** 世界渲染每帧入口（LegacyAnimationDriver.tick 委托）。每帧全量重写 params（世界/GUI 互不污染先例）。 */
    static void tick(EntityPlayer player, float limbSwing, float limbSwingAmount,
                     LegacyBakedModel model, float[] params, ClientModelInfo bundle) {
        if (player == null || model == null || params == null
                || model.bones == null || model.bones.isEmpty()) {
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
                           LegacyBakedModel model, float[] params, ClientModelInfo bundle) {
        // 1.7.10 无 getName（1.8+），展示名走 getCommandSenderName（vanilla-mc-1710 Entity.java:1508）
        displayName = player.getCommandSenderName();
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

        // 状态裁决：先匹配先赢（AnimationManager 同构）；骑乘→null（vehicle→STOP）
        String state = player.isRiding() ? null : resolve(player, limbSwingAmount); // isRiding Entity.java:1358
        // pin 置位先于查动画（setAnimation 同序：等值早退的键）
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
                        // STOP 分支镜像：淡出+清 pin（PredicateBasedController STOP 分支）
                        startEnding(copyPose(params), "stop");
                        lastRequested = null;
                    }
                    // ENDING 续淡
                } else if (requested) {
                    // 状态切换（含 ENDING 中新请求：主线 setAnimation clearAnimation 同刻落 pending）
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
    }

    private void advance(float dt, LegacyBakedModel model, float[] params, ClientModelInfo bundle) {
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
                // PLAY_ONCE 播完持末帧→ENDING（主线 startEndingTransition，savedEndingTick=len）；
                // HOLD 钳 len 常驻（sample 内部已钳）不进 ENDING
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
        LOG.info(String.format("[ysm-legacy1710] animState %s: %s -> %s (loop=%b len=%.1f phase=%d)",
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
                                   LegacyBakedModel model, float[] params) {
        float amount = Math.min(1.0f, player.isSprinting()
                ? limbSwingAmount * 1.35f : limbSwingAmount);
        float swing = MathHelper.sin(limbSwing * 0.6662f)
                * 1.4f * amount;
        LegacyAnimationSampler.resetParams(model, params);
        for (int i = 0; i < model.bones.size() && (i * 12 + 11) < params.length; i++) {
            String name = model.bones.get(i).name;
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
        LOG.info(String.format("[ysm-legacy1710] animEnd %s: fade-to-bind (%s)", displayName, reason));
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

    /** 主线 AnimationRegister.getVerticalSpeed 对位（yo→prevPosY，vanilla-mc-1710 Entity.java:48,51）。 */
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

    /** params = saved + (params - saved) * f（MathUtil.lerpValues 浮点阵适形）。 */
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
}
