package rip.ysm.legacy122;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.ResetVariable;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import com.elfmcys.yesstevemodel.molang.runtime.Variable;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <1.14 轴 molang 高频变量真值供给（D-molang-1）。
 *
 * <p>主线对位：PrimaryBinding >=1.14 注册 q/query→QueryBinding、v/variable→
 * ScopedVariableBinding、ysm/ctrl→各 Binding（PrimaryBinding.java:48-59）；本类是
 * legacy 版微缩同名面，由共享侧 PrimaryBinding <1.14 分支正式注册（替代
 * fix-122-08sta-scale 的 LegacyMolangNullBindings 反射注入，该类已删）。落地清单以
 * builtin 全部动画 json 表达式扫描频率为纲（harness/out/wd-molang1/freq-scan.txt）：
 * query.anim_time 1285 / vertical_speed 653 / ground_speed 644 / yaw_speed 147 /
 * is_sneaking 103 / life_time 83 / position_delta 69 / position 62 /
 * head_x·y_rotation 56 / is_jumping 19 / time_stamp 16 / body_y_rotation 16 /
 * ctrl.elytra_fly 2364（ctrl 状态布尔族）/ ysm.head_yaw 6428+head_pitch 5547，
 * 加卡面点名 ysm.swing_time/attack_time（1.12.2 swingProgressInt/getSwingProgress 真值
 * 直供，先例 02e5320）。
 *
 * <p>求值上下文挂接：表达式关键帧在采样期经 BoneKeyFrame.evaluate→
 * Variable.evaluate 求值（渲染线程），本类静态 Frame 快照由世界链
 * （LegacyAnimationDriver.beginFrame 传 player+partialTick）与 GUI 预览链
 * （LegacyCardPreview beginFrame(null,0)）每帧写入；采样器每次施加动画时
 * setAnimTime(t/20 秒)——对位主线 AnimationControllerInstance:164 setAnimTime(adjustedTick/20.0f)
 * （query.anim_time 单位=秒）。未落地变量沿 08sta 空语义兜底：NullVariable 求值恒
 * null（asFloat 0/asBoolean false/?? 透传右值），未注册命名空间走 NULL_NS 空链——
 * `!v.roaming.car` 得 1、`v.roaming.car` 得 0 的 08sta 语义保持（v 命名空间落地为
 * 表达式内赋值暂存，ResetVariable 随 parser 归还清空=主线 ScopedVariableBinding
 * reset 同款，无跨帧/跨模型泄漏）。
 *
 * <p>1.12.2 面考古（vanilla-mc-1.12.2 行号亲证）：swing 族 EntityLivingBase:93/:95/:102
 * （swingArm :1156-1157 置 -1、updateArmSwingProgress :1221-1231 每 tick ++ 并
 * swingProgress=swingProgressInt/end、getSwingProgress :1979-1985=prev+wrap 差插值）
 * ；头Look=RenderLivingBase.doRender:71-88 interpolateRotation(prev,cur,pt)+
 * wrapDegrees 差值±85 钳；food=getFoodStats()。ctrl 状态布尔=主线 CtrlBinding:54-72
 * 同名 19 状态（本线 LegacyWorldAnimationController.STATES 裁决同名发布）。
 *
 * <p>适形声明：ctrl.*=本帧主状态机裁决名相等（主线=实体谓词逐帧求值——模型缺该
 * 状态动画时主线仍 true、本线 false）；anim_time 于 ENDING 相不刷新（legacy 该相
 * 不做关键帧求值=观察面为空）；is_close_eyes 眨眼钟用 ticksExisted+partialTick
 * （主线=AnimationEvent tick）；head 插值 wrap 单步差（>180°/帧 的传送帧）与
 * vanilla 循环式微差。
 */
public final class LegacyMolangContext {

    private static final Logger LOG = Logger.getLogger("yes_steve_model");

    /** 命名空间根：q/query（真值叶子表）。 */
    public static final ObjectBinding QUERY = new Root(Namespace.QUERY);
    /** 命名空间根：ysm（真值叶子表）。 */
    public static final ObjectBinding YSM = new Root(Namespace.YSM);
    /** 命名空间根：ctrl（19 状态布尔+常量面）。 */
    public static final ObjectBinding CTRL = new Root(Namespace.CTRL);
    /** 未落地命名空间兜底（tlm/args/fn/context/c）：空语义链=08sta NullVariable 原样。 */
    public static final ObjectBinding NULL_NS = new NullNamespace();

    /** 调试打点开关（采证用：-Dysm.legacy.molang.debug 或同款环境变量）。 */
    private static final boolean DEBUG = Boolean.getBoolean("ysm.legacy.molang.debug")
            || System.getenv("YSM_LEGACY_MOLANG_DEBUG") != null;

    private LegacyMolangContext() {
    }

    // ==================== 每帧快照（渲染线程写，采样期读） ====================

    private static final class Frame {
        EntityPlayer player;
        float partialTick;
        float animTimeSeconds;
    }

    private static final Frame FRAME = new Frame();

    /** 调试采样帧计数（beginFrame 每 20 帧打点，挥击期叶子逐帧打点）。 */
    private static int debugFrame;

    /** 世界链每帧开头调用（GUI 预览传 null player）。 */
    public static void beginFrame(@Nullable EntityPlayer player, float partialTick) {
        FRAME.player = player;
        FRAME.partialTick = partialTick;
        FRAME.animTimeSeconds = 0.0f;
        if (DEBUG && player != null && debugFrame++ % 20 == 0) {
            LOG.info(String.format(
                    "[ysm-legacy122] molangFrame vanilla-truth: swingProgressInt=%d swingProgress=%.4f hurtTime=%d health=%.1f groundSpeed=%.3f",
                    player.swingProgressInt, player.getSwingProgress(partialTick),
                    player.hurtTime, player.getHealth(),
                    20.0f * MathHelper.sqrt((float) (player.motionX * player.motionX
                            + player.motionZ * player.motionZ))));
        }
    }

    /** 采样器每次施加动画时写（=该动画的局部时间轴，秒；主线
     * AnimationControllerInstance:164 setAnimTime(adjustedTick/20.0f) 同单位）。 */
    public static void setAnimTime(float seconds) {
        FRAME.animTimeSeconds = seconds;
    }

    // ==================== 叶子求值面 ====================

    private interface LeafEval {
        @Nullable Object eval();
    }

    /** 真值叶子：解析期由 Root.getProperty 产出，求值期读静态快照。 */
    private static final class Leaf implements Variable {
        final String name;
        final LeafEval eval;

        Leaf(String name, LeafEval eval) {
            this.name = name;
            this.eval = eval;
        }

        @Override
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            return eval.eval();
        }
    }

    /** 命名空间根：落地名→真值叶子/函数，未落地名→NullVariable（空语义兜底）。 */
    private static final class Root implements ObjectBinding {
        private final Map<String, Object> leaves;

        Root(Namespace ns) {
            leaves = ns.build();
        }

        @Override
        public Object getProperty(String name) {
            Object leaf = leaves.get(name);
            return leaf != null ? leaf : NullVariable.INSTANCE;
        }
    }

    /** 未落地命名空间整根空链（08sta FakeNamespace 原样）。 */
    private static final class NullNamespace implements ObjectBinding {
        @Override
        public Object getProperty(String name) {
            return NullVariable.INSTANCE;
        }
    }

    /**
     * 空语义链节点（08sta LegacyMolangNullBindings.NullVariable 原样收编）：既是
     * ObjectBinding（点路径中间段，MolangParserImpl:112-119 要求非空）又是
     * AssignableVariable（叶子求值 null=主线未赋值语义；assign 空=无脚本出口）。
     */
    private static final class NullVariable implements AssignableVariable, ObjectBinding {

        static final NullVariable INSTANCE = new NullVariable();

        @Override
        public Object getProperty(String name) {
            return this;
        }

        @Override
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            return null;
        }

        @Override
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
        }
    }

    /**
     * v/variable/t/temp 暂存槽（主线 ScopedVariableBinding 微缩）：表达式内
     * `v.x=…;v.x` 赋值面，未赋值读出 null（08sta `!v.roaming.car` 得 1 语义根基）；
     * ResetVariable 随 parser 归还（GeckoLibCache.releaseParser）清空——生命周期=
     * 单次解析，对位主线 scopedStorage 随控制器上下文清空。
     */
    public static final class Scoped implements ObjectBinding, ResetVariable {

        private final Map<String, Object> slots = new HashMap<>();

        @Override
        public Object getProperty(String name) {
            return new Slot(name);
        }

        @Override
        public void reset() {
            slots.clear();
        }

        private final class Slot implements AssignableVariable {
            private final String name;

            Slot(String name) {
                this.name = name;
            }

            @Override
            public Object evaluate(@NotNull ExecutionContext<?> context) {
                synchronized (slots) {
                    return slots.get(name);
                }
            }

            @Override
            public void assign(@NotNull ExecutionContext<?> context, Object value) {
                synchronized (slots) {
                    slots.put(name, value);
                }
            }
        }
    }

    // ==================== 逐命名空间落地清单（频率序见类注） ====================

    private enum Namespace {
        QUERY {
            @Override
            Map<String, Object> build() {
                Map<String, Object> m = new HashMap<>();
                m.put("anim_time", new Leaf("anim_time", () -> FRAME.animTimeSeconds));
                m.put("vertical_speed", new Leaf("vertical_speed", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : 20.0f * (float) (p.posY - p.prevPosY);
                }));
                m.put("ground_speed", new Leaf("ground_speed", () -> {
                    EntityPlayer p = FRAME.player;
                    if (p == null) {
                        return null;
                    }
                    double dx = p.motionX;
                    double dz = p.motionZ;
                    return 20.0f * MathHelper.sqrt((float) (dx * dx + dz * dz));
                }));
                m.put("yaw_speed", new Leaf("yaw_speed", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null
                            : 20.0f * MathHelper.wrapDegrees(p.rotationYaw - p.prevRotationYaw);
                }));
                m.put("is_sneaking", new Leaf("is_sneaking", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.isSneaking();
                }));
                m.put("life_time", new Leaf("life_time", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : (p.ticksExisted + FRAME.partialTick) / 20.0f;
                }));
                m.put("position", positionFn());
                m.put("position_delta", positionDeltaFn());
                m.put("head_x_rotation", new Leaf("head_x_rotation", () -> headYaw()));
                m.put("head_y_rotation", new Leaf("head_y_rotation", () -> headPitch()));
                m.put("is_jumping", new Leaf("is_jumping", () -> {
                    // 主线 1.20 面：!flying && !passenger && !onGround && !inWater（QueryBinding:161）
                    EntityPlayer p = FRAME.player;
                    return p != null && !p.capabilities.isFlying && p.getPassengers().isEmpty()
                            && !p.onGround && !p.isInWater();
                }));
                m.put("time_stamp", new Leaf("time_stamp", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : (float) p.world.getWorldTime();
                }));
                m.put("body_y_rotation", new Leaf("body_y_rotation", () -> {
                    EntityPlayer p = FRAME.player;
                    // 1.12.2 无 MCP 名 yBodyRot(O)（ref 实证）→ 躯干 yaw 对位
                    // renderYawOffset 插值（RenderLivingBase 躯干旋转源同字段）
                    return p == null ? null : MathHelper.wrapDegrees(
                            interp(p.prevRenderYawOffset, p.renderYawOffset, FRAME.partialTick));
                }));
                m.put("eye_target_y_rotation", new Leaf("eye_target_y_rotation", () -> headPitch()));
                m.put("is_on_ground", new Leaf("is_on_ground", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.onGround;
                }));
                m.put("health", new Leaf("health", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : p.getHealth();
                }));
                m.put("max_health", new Leaf("max_health", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : p.getMaxHealth();
                }));
                m.put("walk_distance", new Leaf("walk_distance", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : p.distanceWalkedModified;
                }));
                m.put("modified_distance_moved", new Leaf("modified_distance_moved", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : p.distanceWalkedModified;
                }));
                m.put("is_riding", new Leaf("is_riding", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.isRiding();
                }));
                m.put("has_rider", new Leaf("has_rider", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && !p.getPassengers().isEmpty();
                }));
                m.put("is_in_water", new Leaf("is_in_water", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.isInWater();
                }));
                m.put("is_sprinting", new Leaf("is_sprinting", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.isSprinting();
                }));
                m.put("is_first_person", new Leaf("is_first_person", () ->
                        net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView == 0));
                m.put("hurt_time", new Leaf("hurt_time", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : (float) p.hurtTime;
                }));
                return m;
            }
        },
        YSM {
            @Override
            Map<String, Object> build() {
                Map<String, Object> m = new HashMap<>();
                m.put("swinging", new Leaf("swinging", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && p.isSwingInProgress;
                }));
                m.put("swing_time", new Leaf("swing_time", () -> {
                    EntityPlayer p = FRAME.player;
                    if (p == null) {
                        return null;
                    }
                    int v = p.swingProgressInt;
                    if (DEBUG && p.isSwingInProgress) {
                        LOG.info(String.format("[ysm-legacy122] molang swing_time=%d (swingProgress=%.4f)", v, p.swingProgress));
                    }
                    return (float) v;
                }));
                m.put("swinging_arm", new Leaf("swinging_arm", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null
                            : (p.swingingHand == EnumHand.MAIN_HAND ? 0.0f : 1.0f);
                }));
                m.put("attack_time", new Leaf("attack_time", () -> {
                    EntityPlayer p = FRAME.player;
                    if (p == null) {
                        return null;
                    }
                    float v = p.getSwingProgress(FRAME.partialTick);
                    if (DEBUG && p.isSwingInProgress) {
                        LOG.info(String.format("[ysm-legacy122] molang attack_time=%.4f (swingProgressInt=%d)", v, p.swingProgressInt));
                    }
                    return v;
                }));
                m.put("head_yaw", new Leaf("head_yaw", () -> headYaw()));
                m.put("head_pitch", new Leaf("head_pitch", () -> headPitch()));
                m.put("food_level", new Leaf("food_level", () -> {
                    EntityPlayer p = FRAME.player;
                    return p == null ? null : (float) p.getFoodStats().getFoodLevel();
                }));
                m.put("has_mainhand", new Leaf("has_mainhand", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && !p.getHeldItemMainhand().isEmpty();
                }));
                m.put("has_offhand", new Leaf("has_offhand", () -> {
                    EntityPlayer p = FRAME.player;
                    return p != null && !p.getHeldItemOffhand().isEmpty();
                }));
                m.put("is_close_eyes", new Leaf("is_close_eyes", () -> {
                    // 主线 YSMBinding:439-441 同式（眨眼钟=本线 life tick；适形见类注）
                    EntityPlayer p = FRAME.player;
                    if (p == null) {
                        return null;
                    }
                    if (p.isPlayerSleeping()) {
                        return true;
                    }
                    float blinkPhase = (p.ticksExisted + FRAME.partialTick
                            + (float) (Math.abs(p.getUniqueID().getLeastSignificantBits()) % 10)) % 90.0f;
                    return blinkPhase > 85.0f;
                }));
                return m;
            }
        },
        CTRL {
            @Override
            Map<String, Object> build() {
                Map<String, Object> m = new HashMap<>();
                // 19 状态布尔（主线 CtrlBinding:54-72 同名）=本帧主状态机裁决名相等
                String[] states = {
                        "death", "riptide", "sleep", "swim", "climb", "climbing",
                        "ladder_up", "ladder_stillness", "ladder_down",
                        "fly", "elytra_fly", "swim_stand", "attacked", "jump",
                        "sneak", "sneaking", "run", "walk", "idle",
                };
                for (String state : states) {
                    m.put(state, stateLeaf(state));
                }
                // 常量面（主线 CtrlBinding:90-96 constValue 原值）
                m.put("state_continue", new Leaf("state_continue", () -> 2.0f));
                m.put("state_stop", new Leaf("state_stop", () -> 3.0f));
                m.put("state_pause", new Leaf("state_pause", () -> 4.0f));
                m.put("state_bypass", new Leaf("state_bypass", () -> 5.0f));
                m.put("loop", new Leaf("loop", () -> 10.0f));
                m.put("play_once", new Leaf("play_once", () -> 11.0f));
                m.put("hold_on_last_frame", new Leaf("hold_on_last_frame", () -> 12.0f));
                return m;
            }
        };

        abstract Map<String, Object> build();
    }

    /** 本帧主状态机裁决状态名（LegacyWorldAnimationController 每 tick 发布，骑乘=ok null）。 */
    private static String mainState;

    public static void publishMainState(@Nullable String state) {
        mainState = state;
    }

    private static Leaf stateLeaf(final String state) {
        return new Leaf(state, () -> state.equals(mainState));
    }

    // ==================== 插值工具（RenderLivingBase 同式） ====================

    private static float interp(float prev, float cur, float pt) {
        return prev + (cur - prev) * pt;
    }

    /** 净头偏航：wrapDegrees(headYaw-yawOffset) ±85 钳（RenderLivingBase.doRender:71-88）。 */
    private static Float headYaw() {
        EntityLivingBase e = FRAME.player;
        if (e == null) {
            return null;
        }
        float yawOffset = interp(e.prevRenderYawOffset, e.renderYawOffset, FRAME.partialTick);
        float headYaw = interp(e.prevRotationYawHead, e.rotationYawHead, FRAME.partialTick);
        return MathHelper.clamp(MathHelper.wrapDegrees(headYaw - yawOffset), -85.0f, 85.0f);
    }

    /** 头俯仰=rotationPitch 插值（RenderLivingBase.doRender:90）。 */
    private static Float headPitch() {
        EntityLivingBase e = FRAME.player;
        return e == null ? null : interp(e.prevRotationPitch, e.rotationPitch, FRAME.partialTick);
    }

    /** query.position(axis)：0/1/2=x/y/z 插值坐标（主线 Position.java 同式）。 */
    private static Function positionFn() {
        return new Function() {
            @Override
            public Object evaluate(@NotNull ExecutionContext<?> ctx,
                                   @NotNull Function.ArgumentCollection args) {
                EntityPlayer p = FRAME.player;
                if (p == null) {
                    return null;
                }
                switch (args.getAsInt(ctx, 0)) {
                    case 0:
                        return interp((float) p.prevPosX, (float) p.posX, FRAME.partialTick);
                    case 1:
                        return interp((float) p.prevPosY, (float) p.posY, FRAME.partialTick);
                    case 2:
                        return interp((float) p.prevPosZ, (float) p.posZ, FRAME.partialTick);
                    default:
                        return null;
                }
            }

            @Override
            public boolean validateArgumentSize(int size) {
                return size == 1;
            }
        };
    }

    /** query.position_delta(axis)：0/1/2=x/y/z tick 位移（主线 PositionDelta.java 同式）。 */
    private static Function positionDeltaFn() {
        return new Function() {
            @Override
            public Object evaluate(@NotNull ExecutionContext<?> ctx,
                                   @NotNull Function.ArgumentCollection args) {
                EntityPlayer p = FRAME.player;
                if (p == null) {
                    return null;
                }
                switch (args.getAsInt(ctx, 0)) {
                    case 0:
                        return (float) (p.posX - p.prevPosX);
                    case 1:
                        return (float) (p.posY - p.prevPosY);
                    case 2:
                        return (float) (p.posZ - p.prevPosZ);
                    default:
                        return null;
                }
            }

            @Override
            public boolean validateArgumentSize(int size) {
                return size == 1;
            }
        };
    }
}
